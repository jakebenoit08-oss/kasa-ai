/**
 * KASA AI - Production Billing & Subscription Service
 * 
 * Manages Paystack Integration, Authoritative Pricing, Atomic Firestore Transactions,
 * HMAC Webhook Verification, Idempotency, and Refund/Chargeback Processing.
 */

const crypto = require('crypto');
const { getDb, PERIOD_MS } = require('./firestore');
const { isOwnerEmail } = require('./owner');

// Server-authoritative Pricing Matrix
const PLANS = {
  plus: {
    id: 'plus',
    name: 'KASA Plus',
    amountPesewas: 4900, // GH₵49.00
    currency: 'GHS',
    creditsGranted: 5,
  },
  pro: {
    id: 'pro',
    name: 'KASA Pro',
    amountPesewas: 9900, // GH₵99.00
    currency: 'GHS',
    creditsGranted: 15,
  },
};

const PAYSTACK_API_BASE = 'https://api.paystack.co';

function getPaystackSecretKey() {
  const key = process.env.PAYSTACK_SECRET_KEY;
  if (key && key.trim() && key !== 'your_paystack_secret_key_here') {
    return key.trim();
  }
  return null;
}

/**
 * Verifies Paystack HMAC-SHA512 webhook signature against raw request body buffer.
 * 
 * @param {Buffer|string} rawBody
 * @param {string} signatureHeader
 * @returns {boolean}
 */
function verifyWebhookSignature(rawBody, signatureHeader) {
  const secret = getPaystackSecretKey();
  if (!secret || !signatureHeader) return false;

  try {
    const computedHash = crypto
      .createHmac('sha512', secret)
      .update(rawBody)
      .digest('hex');

    const sigBuf = Buffer.from(signatureHeader.trim(), 'utf8');
    const compBuf = Buffer.from(computedHash.trim(), 'utf8');

    if (sigBuf.length !== compBuf.length) return false;
    return crypto.timingSafeEqual(sigBuf, compBuf);
  } catch (err) {
    console.error('[KASA Billing] Signature verification error:', err.message);
    return false;
  }
}

/**
 * Initializes a Paystack checkout session for a verified user.
 * 
 * @param {string} verifiedUid
 * @param {string} verifiedEmail
 * @param {string} planId
 * @returns {Promise<{ authorizationUrl: string, reference: string }>}
 */
async function initializeCheckout(verifiedUid, verifiedEmail, planId) {
  const plan = PLANS[planId];
  if (!plan) {
    const error = new Error('Invalid plan selected. Choose "plus" or "pro".');
    error.statusCode = 400;
    error.code = 'INVALID_PLAN';
    throw error;
  }

  // Owner check: Owners never pay
  if (isOwnerEmail(verifiedEmail)) {
    const error = new Error('Owner accounts have permanent unlimited access and do not require paid subscriptions.');
    error.statusCode = 400;
    error.code = 'OWNER_ACCOUNT';
    throw error;
  }

  const secretKey = getPaystackSecretKey();
  if (!secretKey) {
    const error = new Error('Paystack secret key is not configured on server.');
    error.statusCode = 503;
    error.code = 'BILLING_NOT_CONFIGURED';
    throw error;
  }

  // Generate cryptographically secure reference
  const randomSuffix = crypto.randomBytes(6).toString('hex');
  const reference = `kasa_sub_${planId}_${verifiedUid.slice(0, 8)}_${Date.now()}_${randomSuffix}`;

  const payload = {
    email: verifiedEmail || `${verifiedUid}@kasa.ai`,
    amount: plan.amountPesewas,
    currency: plan.currency,
    reference: reference,
    callback_url: 'https://kasa.ai/billing/callback',
    metadata: {
      userId: verifiedUid,
      planId: planId,
      kasaEnvironment: process.env.NODE_ENV || 'production',
    },
  };

  const db = getDb();
  const now = Date.now();

  // Create initial payment record with status 'initiated'
  await db.collection('payments').doc(reference).set({
    reference,
    paystackTransactionId: null,
    userId: verifiedUid,
    userEmail: verifiedEmail || null,
    planId: plan.id,
    amountPaidPesewas: 0,
    expectedAmountPesewas: plan.amountPesewas,
    currency: plan.currency,
    status: 'initiated',
    channel: null,
    creditsGranted: 0,
    creditsConsumed: 0,
    creditsRemaining: 0,
    creditsGrantedAt: null,
    createdAt: now,
    verifiedAt: null,
    refundedAt: null,
    gatewayResponse: null,
    paystackEventType: null,
  });

  // Call Paystack API
  const response = await fetch(`${PAYSTACK_API_BASE}/transaction/initialize`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${secretKey}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(payload),
  });

  const resJson = await response.json();
  if (!response.ok || !resJson.status || !resJson.data) {
    console.error('[KASA Billing] Paystack init failed:', resJson);
    const error = new Error(resJson.message || 'Failed to initialize payment with Paystack.');
    error.statusCode = 502;
    error.code = 'GATEWAY_ERROR';
    throw error;
  }

  return {
    authorizationUrl: resJson.data.authorization_url,
    reference: reference,
  };
}

/**
 * Verifies transaction with Paystack authoritative API.
 * 
 * @param {string} reference
 * @returns {Promise<any>}
 */
async function verifyPaystackTransaction(reference) {
  const secretKey = getPaystackSecretKey();
  if (!secretKey) {
    const error = new Error('Paystack secret key is not configured.');
    error.statusCode = 503;
    error.code = 'BILLING_NOT_CONFIGURED';
    throw error;
  }

  const response = await fetch(`${PAYSTACK_API_BASE}/transaction/verify/${encodeURIComponent(reference)}`, {
    method: 'GET',
    headers: {
      'Authorization': `Bearer ${secretKey}`,
    },
  });

  const resJson = await response.json();
  if (!response.ok || !resJson.status || !resJson.data) {
    const error = new Error(resJson.message || 'Transaction verification failed at gateway.');
    error.statusCode = 502;
    error.code = 'VERIFICATION_FAILED';
    throw error;
  }

  return resJson.data;
}

/**
 * Executes an atomic Firestore transaction granting payment + entitlement + credit grant.
 * Strictly idempotent: will never double-grant credits if already processed.
 * 
 * @param {string} reference
 * @param {any} paystackData
 * @param {string} eventType
 * @returns {Promise<{ success: boolean, alreadyProcessed: boolean, entitlement: any }>}
 */
async function executeAtomicGrant(reference, paystackData, eventType = 'charge.success') {
  const db = getDb();
  const paymentRef = db.collection('payments').doc(reference);

  return await db.runTransaction(async (transaction) => {
    const paymentDoc = await transaction.get(paymentRef);
    const existingPayment = paymentDoc.exists ? paymentDoc.data() : null;

    if (existingPayment && existingPayment.status === 'success') {
      console.log(`[KASA Billing] Reference ${reference} already successfully processed. Idempotent return.`);
      const userRef = db.collection('entitlements').doc(existingPayment.userId);
      const userDoc = await transaction.get(userRef);
      return {
        success: true,
        alreadyProcessed: true,
        entitlement: userDoc.exists ? userDoc.data() : null,
      };
    }

    const userId = (paystackData.metadata && paystackData.metadata.userId) || (existingPayment && existingPayment.userId);
    const planId = (paystackData.metadata && paystackData.metadata.planId) || (existingPayment && existingPayment.planId);
    const plan = PLANS[planId];

    if (!userId || !plan) {
      throw new Error(`Invalid transaction data: userId=${userId}, planId=${planId}`);
    }

    // Verify amount and currency
    const amountPaid = paystackData.amount;
    const currency = paystackData.currency;
    if (amountPaid < plan.amountPesewas || currency !== 'GHS') {
      throw new Error(`Amount or currency mismatch: received ${amountPaid} ${currency}, expected ${plan.amountPesewas} GHS`);
    }

    const now = Date.now();
    const grantId = `grant_${reference}_${now}`;
    const userRef = db.collection('entitlements').doc(userId);
    const grantRef = db.collection('credit_grants').doc(grantId);

    // 1. Update payment document
    const updatedPayment = {
      reference,
      paystackTransactionId: String(paystackData.id || ''),
      userId,
      userEmail: (paystackData.customer && paystackData.customer.email) || null,
      planId,
      amountPaidPesewas: amountPaid,
      expectedAmountPesewas: plan.amountPesewas,
      currency,
      status: 'success',
      channel: paystackData.channel || null,
      creditsGranted: plan.creditsGranted,
      creditsConsumed: 0,
      creditsRemaining: plan.creditsGranted,
      creditsGrantedAt: now,
      createdAt: (existingPayment && existingPayment.createdAt) || now,
      verifiedAt: now,
      refundedAt: null,
      gatewayResponse: paystackData.gateway_response || 'Successful',
      paystackEventType: eventType,
    };
    transaction.set(paymentRef, updatedPayment, { merge: true });

    // 2. Create credit grant document
    const creditGrant = {
      grantId,
      userId,
      paymentReference: reference,
      planId,
      creditsGranted: plan.creditsGranted,
      creditsConsumed: 0,
      creditsRemaining: plan.creditsGranted,
      status: 'active',
      createdAt: now,
      updatedAt: now,
      revokedAt: null,
    };
    transaction.set(grantRef, creditGrant);

    // 3. Update user entitlement document (No credit rollover per specification)
    const updatedEntitlement = {
      userId,
      tier: plan.id,
      subscriptionStatus: 'active',
      musicCredits: plan.creditsGranted,
      periodStart: now,
      periodEnd: now + PERIOD_MS,
      lastPaymentReference: reference,
      subscriptionCode: (paystackData.subscription && paystackData.subscription.subscription_code) || null,
      autoRenew: true,
      updatedAt: now,
    };
    transaction.set(userRef, updatedEntitlement, { merge: true });

    // 4. If recurring subscription metadata is present, update subscriptions collection
    if (paystackData.subscription && paystackData.subscription.subscription_code) {
      const subCode = paystackData.subscription.subscription_code;
      const subRef = db.collection('subscriptions').doc(subCode);
      transaction.set(subRef, {
        subscriptionCode: subCode,
        userId,
        planId,
        status: 'active',
        amount: amountPaid,
        currency,
        email: paystackData.customer && paystackData.customer.email,
        nextPaymentDate: paystackData.subscription.next_payment_date || null,
        periodStart: now,
        periodEnd: now + PERIOD_MS,
        authorizationCodePresent: !!(paystackData.authorization && paystackData.authorization.authorization_code),
        createdAt: now,
        updatedAt: now,
      }, { merge: true });
    }

    return {
      success: true,
      alreadyProcessed: false,
      entitlement: updatedEntitlement,
    };
  });
}

/**
 * Handles refunds and chargebacks by revoking ONLY applicable unconsumed credits.
 * Never deletes generated songs.
 * 
 * @param {string} reference
 * @param {string} eventType 'refund.processed' | 'charge.dispute.create'
 * @param {string} reason
 * @returns {Promise<{ success: boolean, alreadyRefunded: boolean, revokedCredits: number }>}
 */
async function executeRefundOrChargeback(reference, eventType = 'refund.processed', reason = '') {
  const db = getDb();
  const paymentRef = db.collection('payments').doc(reference);

  return await db.runTransaction(async (transaction) => {
    const paymentDoc = await transaction.get(paymentRef);
    if (!paymentDoc.exists) {
      throw new Error(`Payment record ${reference} not found.`);
    }

    const payment = paymentDoc.data();
    if (payment.status === 'refunded' || payment.status === 'disputed') {
      return { success: true, alreadyRefunded: true, revokedCredits: 0 };
    }

    const userId = payment.userId;
    const userRef = db.collection('entitlements').doc(userId);
    const userDoc = await transaction.get(userRef);
    const userEntitlement = userDoc.exists ? userDoc.data() : { musicCredits: 0 };

    // Find applicable credit grant
    const grantsSnapshot = await db.collection('credit_grants').where('paymentReference', '==', reference).get();
    let revokedCredits = 0;

    const now = Date.now();

    grantsSnapshot.docs.forEach((doc) => {
      const grant = doc.data();
      if (grant.status === 'active' || grant.status === 'partially_consumed') {
        const remainingInGrant = grant.creditsRemaining || 0;
        revokedCredits += remainingInGrant;

        const grantRef = db.collection('credit_grants').doc(grant.grantId);
        transaction.set(grantRef, {
          status: 'revoked',
          creditsRemaining: 0,
          revokedAt: now,
          updatedAt: now,
        }, { merge: true });
      }
    });

    // Mark payment status
    transaction.set(paymentRef, {
      status: eventType.includes('dispute') ? 'disputed' : 'refunded',
      refundedAt: now,
      refundReason: reason || eventType,
      updatedAt: now,
    }, { merge: true });

    // Adjust user wallet: subtract only the unconsumed granted credits (never negative)
    const newCredits = Math.max(0, (userEntitlement.musicCredits || 0) - revokedCredits);
    transaction.set(userRef, {
      tier: 'free',
      subscriptionStatus: 'cancelled',
      musicCredits: newCredits,
      updatedAt: now,
    }, { merge: true });

    console.log(`[KASA Billing] Processed ${eventType} for ${reference}. Revoked ${revokedCredits} unconsumed credits. New balance: ${newCredits}`);

    return {
      success: true,
      alreadyRefunded: false,
      revokedCredits,
    };
  });
}

/**
 * Retrieves the authoritative entitlement for a user.
 * Enforces owner access, cycle resets, and expiration.
 * 
 * @param {string} userId
 * @param {string} userEmail
 * @returns {Promise<any>}
 */
async function getAuthoritativeEntitlement(userId, userEmail) {
  const db = getDb();
  const uid = String(userId);
  const now = Date.now();

  // 1. Permanent Server-side Owner Access
  if (isOwnerEmail(userEmail)) {
    return {
      userId: uid,
      tier: 'pro',
      subscriptionStatus: 'active',
      musicCredits: 999,
      isOwner: true,
      isUnlimitedDev: true,
      periodStart: now,
      periodEnd: now + (365 * 24 * 60 * 60 * 1000),
      label: 'Owner Access',
    };
  }

  const userRef = db.collection('entitlements').doc(uid);
  const doc = await userRef.get();
  let record = doc.exists ? doc.data() : null;

  if (!record) {
    record = {
      userId: uid,
      tier: 'free',
      subscriptionStatus: 'unpaid',
      musicCredits: 1, // Default 1 free credit per cycle
      periodStart: now,
      periodEnd: now + PERIOD_MS,
      lastPaymentReference: null,
      subscriptionCode: null,
      autoRenew: false,
      createdAt: now,
      updatedAt: now,
    };
    await userRef.set(record);
  } else {
    // Check expiration
    if (now >= record.periodEnd) {
      if (record.subscriptionStatus === 'active') {
        // Paid period expired without renewal
        record.tier = 'free';
        record.subscriptionStatus = 'expired';
        record.periodStart = now;
        record.periodEnd = now + PERIOD_MS;
        record.musicCredits = Math.max(record.musicCredits || 0, 1);
        record.updatedAt = now;
        await userRef.set(record, { merge: true });
      } else {
        // Free cycle renewal
        record.periodStart = now;
        record.periodEnd = now + PERIOD_MS;
        record.musicCredits = Math.max(record.musicCredits || 0, 1);
        record.updatedAt = now;
        await userRef.set(record, { merge: true });
      }
    }
  }

  return {
    ...record,
    isOwner: false,
    isUnlimitedDev: false,
  };
}

module.exports = {
  PLANS,
  initializeCheckout,
  verifyWebhookSignature,
  verifyPaystackTransaction,
  executeAtomicGrant,
  executeRefundOrChargeback,
  getAuthoritativeEntitlement,
  getPaystackSecretKey,
};
