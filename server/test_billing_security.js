/**
 * Automated Verification Suite for KASA AI Billing, Security, & Entitlements
 */

process.env.NODE_ENV = 'test';
process.env.USE_MOCK_FIRESTORE = 'true';

const assert = require('assert');
const crypto = require('crypto');
const { isOwnerEmail } = require('./owner');
const {
  PLANS,
  verifyWebhookSignature,
  executeAtomicGrant,
  executeRefundOrChargeback,
  getAuthoritativeEntitlement,
} = require('./billing');
const { getDb, setUseMock, getMockStore } = require('./firestore');

setUseMock(true);

async function runTests() {
  console.log('--- Starting KASA AI Billing & Security Verification Suite ---');

  // Test 1: Owner Email Verification
  console.log('1. Testing Owner Email Access (case-insensitive & strict)...');
  assert.strictEqual(isOwnerEmail('powerkobbi9@gmail.com'), true);
  assert.strictEqual(isOwnerEmail('PowerKobbi9@GMAIL.COM'), true);
  assert.strictEqual(isOwnerEmail('rabbiking713@gmail.com'), true);
  assert.strictEqual(isOwnerEmail('RabbiKing713@Gmail.Com'), true);
  assert.strictEqual(isOwnerEmail('jakebenoit08@gmail.com'), true);
  assert.strictEqual(isOwnerEmail('JakeBenoit08@GMAIL.com'), true);
  assert.strictEqual(isOwnerEmail('attacker@evil.com'), false);
  assert.strictEqual(isOwnerEmail('random@gmail.com'), false);
  assert.strictEqual(isOwnerEmail(''), false);
  assert.strictEqual(isOwnerEmail(null), false);
  console.log('   PASS: Owner emails strictly authorized.');

  // Test 2: Owner Entitlement
  console.log('2. Testing Owner Entitlements...');
  const ownerEnt = await getAuthoritativeEntitlement('uid_owner_1', 'powerkobbi9@gmail.com');
  assert.strictEqual(ownerEnt.isOwner, true);
  assert.strictEqual(ownerEnt.tier, 'pro');
  assert.strictEqual(ownerEnt.subscriptionStatus, 'active');
  assert.strictEqual(ownerEnt.musicCredits, 999);
  console.log('   PASS: Owner accounts receive permanent pro unlimited access.');

  // Test 3: Standard User Default Entitlement
  console.log('3. Testing Standard User Default Entitlement...');
  const standardEnt = await getAuthoritativeEntitlement('uid_user_1', 'customer@example.com');
  assert.strictEqual(standardEnt.isOwner, false);
  assert.strictEqual(standardEnt.tier, 'free');
  assert.strictEqual(standardEnt.musicCredits, 1);
  console.log('   PASS: Standard user receives 1 free credit.');

  // Test 4: Pricing Matrix Validation
  console.log('4. Validating Server-Side Pricing Matrix...');
  assert.strictEqual(PLANS.plus.amountPesewas, 4900);
  assert.strictEqual(PLANS.plus.currency, 'GHS');
  assert.strictEqual(PLANS.plus.creditsGranted, 5);

  assert.strictEqual(PLANS.pro.amountPesewas, 9900);
  assert.strictEqual(PLANS.pro.currency, 'GHS');
  assert.strictEqual(PLANS.pro.creditsGranted, 15);
  console.log('   PASS: Pricing is exact (Plus = GH₵49 / 4900p, Pro = GH₵99 / 9900p).');

  // Test 5: Paystack Webhook HMAC-SHA512 Verification
  console.log('5. Testing Paystack Webhook HMAC Verification...');
  process.env.PAYSTACK_SECRET_KEY = 'mock_unit_test_secret_key_for_hmac_testing';
  const testPayload = JSON.stringify({ event: 'charge.success', data: { reference: 'ref_123' } });
  const rawBuf = Buffer.from(testPayload, 'utf8');

  const validSig = crypto
    .createHmac('sha512', process.env.PAYSTACK_SECRET_KEY)
    .update(rawBuf)
    .digest('hex');

  assert.strictEqual(verifyWebhookSignature(rawBuf, validSig), true);
  assert.strictEqual(verifyWebhookSignature(rawBuf, 'invalid_sig_hex_123'), false);
  assert.strictEqual(verifyWebhookSignature(Buffer.from('tampered'), validSig), false);
  console.log('   PASS: HMAC-SHA512 verification accurately catches spoofed and tampered payloads.');

  // Test 6: Atomic Grant & Idempotency
  console.log('6. Testing Atomic Grant and Idempotent Webhook Processing...');
  const testRef = 'kasa_test_ref_001';
  const paystackData = {
    id: 987654,
    amount: 4900,
    currency: 'GHS',
    channel: 'mobile_money',
    gateway_response: 'Successful',
    metadata: {
      userId: 'uid_test_customer_1',
      planId: 'plus',
    },
    customer: {
      email: 'ghana_customer@kasa.ai',
    },
  };

  // First grant
  const result1 = await executeAtomicGrant(testRef, paystackData, 'charge.success');
  assert.strictEqual(result1.success, true);
  assert.strictEqual(result1.alreadyProcessed, false);
  assert.strictEqual(result1.entitlement.tier, 'plus');
  assert.strictEqual(result1.entitlement.musicCredits, 5);

  // Check store
  const store = getMockStore();
  assert.strictEqual(store.payments[testRef].status, 'success');
  assert.strictEqual(store.entitlements['uid_test_customer_1'].musicCredits, 5);

  // Duplicate grant (Webhook retry or verify-session race)
  const result2 = await executeAtomicGrant(testRef, paystackData, 'charge.success');
  assert.strictEqual(result2.success, true);
  assert.strictEqual(result2.alreadyProcessed, true);
  // Balance MUST NOT be double-granted!
  assert.strictEqual(store.entitlements['uid_test_customer_1'].musicCredits, 5);
  console.log('   PASS: Atomic grant is strictly idempotent; duplicate calls do NOT double-credit.');

  // Test 7: Refund / Chargeback Revocation
  console.log('7. Testing Partial/Unconsumed Refund Revocation...');
  // Customer spent 2 credits out of 5, 3 remaining in grant
  const grantId = Object.keys(store.credit_grants).find(k => store.credit_grants[k].paymentReference === testRef);
  assert.ok(grantId);
  store.credit_grants[grantId].creditsConsumed = 2;
  store.credit_grants[grantId].creditsRemaining = 3;
  store.entitlements['uid_test_customer_1'].musicCredits = 3;

  const refundRes = await executeRefundOrChargeback(testRef, 'refund.processed', 'Customer chargeback');
  assert.strictEqual(refundRes.success, true);
  assert.strictEqual(refundRes.alreadyRefunded, false);
  assert.strictEqual(refundRes.revokedCredits, 3);
  assert.strictEqual(store.entitlements['uid_test_customer_1'].musicCredits, 0);
  assert.strictEqual(store.entitlements['uid_test_customer_1'].tier, 'free');
  assert.strictEqual(store.payments[testRef].status, 'refunded');

  // Second refund call is idempotent
  const refundRes2 = await executeRefundOrChargeback(testRef, 'refund.processed');
  assert.strictEqual(refundRes2.alreadyRefunded, true);
  assert.strictEqual(refundRes2.revokedCredits, 0);
  console.log('   PASS: Refunds revoke unconsumed credits without negative balance, idempotently.');

  // Test 8: Expiration & Free Cycle Renewal
  console.log('8. Testing Subscription Expiry & Cycle Reset...');
  const expiredUid = 'uid_expired_user_8';
  store.entitlements[expiredUid] = {
    userId: expiredUid,
    tier: 'pro',
    subscriptionStatus: 'active',
    musicCredits: 0,
    periodStart: Date.now() - (40 * 24 * 60 * 60 * 1000),
    periodEnd: Date.now() - (10 * 24 * 60 * 60 * 1000), // Expired 10 days ago
  };

  const refreshedEnt = await getAuthoritativeEntitlement(expiredUid, 'expired_user@example.com');
  assert.strictEqual(refreshedEnt.tier, 'free');
  assert.strictEqual(refreshedEnt.subscriptionStatus, 'expired');
  assert.strictEqual(refreshedEnt.musicCredits, 1); // Reset with 1 free credit for new cycle
  console.log('   PASS: Expired subscriptions automatically lapse to free tier and receive 1 renewal credit.');

  console.log('\n>>> ALL 8 BILLING & SECURITY VERIFICATION SUITE TESTS PASSED SUCCESSFULLY! <<<');
}

runTests().catch(err => {
  console.error('TEST FAILED:', err);
  process.exit(1);
});
