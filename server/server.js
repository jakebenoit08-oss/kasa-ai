/**
 * KASA AI - Secure Backend Service with Production Billing & Entitlements
 * 
 * Proxies music generation requests to AIMusicAPI Sonic API and enforces
 * server-side user subscription entitlements, KASA Music Credit balances,
 * Paystack billing, and Owner Access.
 * 
 * Authoritative Database: Cloud Firestore (kasa-ai-6b7e1)
 * Pricing: PLUS = GH₵49/mo (5 credits), PRO = GH₵99/mo (15 credits)
 * Owner Access: permanent, server-enforced unlimited access for designated accounts
 */

const http = require('http');
const fs = require('fs');
const path = require('path');
const url = require('url');

// Environment loader
function loadEnv() {
  const possiblePaths = [
    path.join(__dirname, '.env'),
    path.join(__dirname, '..', '.env'),
    path.join(__dirname, '..', '.env.local'),
    path.join(process.cwd(), '.env'),
    path.join(process.cwd(), 'server', '.env'),
    '/app/applet/.env',
    '/app/applet/server/.env',
  ];
  for (const envPath of possiblePaths) {
    if (fs.existsSync(envPath)) {
      try {
        const content = fs.readFileSync(envPath, 'utf8');
        for (const line of content.split('\n')) {
          const trimmed = line.trim();
          if (!trimmed || trimmed.startsWith('#')) continue;
          const eqIdx = trimmed.indexOf('=');
          if (eqIdx > 0) {
            const key = trimmed.slice(0, eqIdx).trim();
            const val = trimmed.slice(eqIdx + 1).trim().replace(/^["']|["']$/g, '');
            if (!process.env[key]) {
              process.env[key] = val;
            }
          }
        }
      } catch (err) {
        // Ignore file read errors
      }
    }
  }

  const jsonPaths = ['/app/.dev.env.json', path.join(process.cwd(), '.dev.env.json')];
  for (const jPath of jsonPaths) {
    if (fs.existsSync(jPath)) {
      try {
        const parsed = JSON.parse(fs.readFileSync(jPath, 'utf8'));
        for (const [k, v] of Object.entries(parsed)) {
          if (!process.env[k] && v) {
            process.env[k] = String(v);
          }
        }
      } catch (err) {
        // Ignore json parse error
      }
    }
  }
}

loadEnv();

const { authenticateRequest } = require('./auth');
const { isOwnerEmail } = require('./owner');
const { getDb } = require('./firestore');
const {
  PLANS,
  initializeCheckout,
  verifyWebhookSignature,
  verifyPaystackTransaction,
  executeAtomicGrant,
  executeRefundOrChargeback,
  getAuthoritativeEntitlement,
} = require('./billing');
const {
  userMutex,
  recordTask,
  getTask,
  updateTask,
} = require('./storage');

// Server Port Configuration
const isAiStudioContainer = process.env.NGINX_PORT === '8080' && !process.env.RENDER;
const PORT = parseInt(
  process.env.PORT ||
  process.env.MUSIC_PORT ||
  process.env.MUSIC_BACKEND_PORT ||
  (isAiStudioContainer ? '8765' : '8765'),
  10
);

const AIMUSIC_BASE_URL = 'https://api.aimusicapi.ai/api/v1/sonic';
const SONIC_MODEL = process.env.AIMUSIC_MODEL || 'sonic-v5-5';
const DEV_ALLOW_UNLIMITED = process.env.DEV_ALLOW_UNLIMITED === 'true';

// Masked secret helper for logging
function maskSecret(secret) {
  if (!secret) return 'NOT_CONFIGURED';
  if (secret.length <= 8) return '********';
  return secret.slice(0, 4) + '...' + secret.slice(-4);
}

function getApiKey() {
  const key = process.env.AIMUSIC_API_KEY;
  if (key && key.trim() && key !== 'your_aimusicapi_key_here') {
    return key.trim();
  }
  return null;
}

// HTTP Server
const server = http.createServer(async (req, res) => {
  // CORS Headers
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization, x-user-id, x-kasa-dev-bypass, x-kasa-admin-secret, x-paystack-signature');

  if (req.method === 'OPTIONS') {
    res.writeHead(204);
    res.end();
    return;
  }

  const parsedUrl = url.parse(req.url, true);
  const pathname = parsedUrl.pathname;

  // JSON helper
  const sendJson = (status, data) => {
    res.writeHead(status, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify(data));
  };

  // Helper to read raw body buffer (crucial for HMAC signature verification)
  const readRawBody = () => new Promise((resolve) => {
    const chunks = [];
    req.on('data', chunk => { chunks.push(chunk); });
    req.on('end', () => {
      resolve(Buffer.concat(chunks));
    });
  });

  // Helper to read JSON body
  const readBody = async () => {
    const raw = await readRawBody();
    try {
      return raw.length ? JSON.parse(raw.toString('utf8')) : {};
    } catch (e) {
      return {};
    }
  };

  // ==========================================
  // Public Health Endpoint
  // ==========================================
  if (pathname === '/health' || pathname === '/api/health') {
    const key = getApiKey();
    return sendJson(200, {
      status: 'ok',
      service: 'kasa-ai-billing-and-music-backend',
      model: SONIC_MODEL,
      aimusicConfigured: !!key,
      paystackConfigured: !!process.env.PAYSTACK_SECRET_KEY,
      paystackMode: process.env.PAYSTACK_MODE || 'live',
      devMode: DEV_ALLOW_UNLIMITED,
      timestamp: Date.now(),
    });
  }

  // ==========================================
  // Public Endpoint: Available Plans & Pricing
  // GET /api/billing/plans
  // ==========================================
  if (req.method === 'GET' && pathname === '/api/billing/plans') {
    return sendJson(200, {
      currency: 'GHS',
      plans: [
        {
          id: 'plus',
          name: 'KASA Plus',
          priceGHS: 49,
          amountPesewas: 4900,
          period: 'monthly',
          musicCredits: 5,
          features: [
            '5 KASA Music Generations',
            'Dual Song Variations per prompt',
            'Full Ghanaian Language Support',
            'High-Fidelity Audio Downloads',
          ],
        },
        {
          id: 'pro',
          name: 'KASA Pro',
          priceGHS: 99,
          amountPesewas: 9900,
          period: 'monthly',
          musicCredits: 15,
          badge: 'Most Popular',
          features: [
            '15 KASA Music Generations',
            'Dual Song Variations per prompt',
            'Priority Fast-Track Queue',
            'Full Commercial Use Rights',
            'Advanced Prompt Engineering',
          ],
        },
      ],
    });
  }

  // ==========================================
  // Protected Endpoint: Get User Credits & Entitlements
  // GET /api/music/credits
  // ==========================================
  if (req.method === 'GET' && pathname === '/api/music/credits') {
    let authUser;
    try {
      authUser = await authenticateRequest(req);
    } catch (err) {
      return sendJson(err.statusCode || 401, {
        error: err.code || 'UNAUTHORIZED',
        message: err.message,
      });
    }

    const verifiedUid = authUser.uid;
    const verifiedEmail = authUser.email;
    const clientSuppliedUserId = parsedUrl.query.userId || req.headers['x-user-id'];

    // Consistency check: client-supplied ID cannot mismatch verified UID
    if (clientSuppliedUserId && clientSuppliedUserId !== verifiedUid && clientSuppliedUserId !== 'usr_default_kasa') {
      return sendJson(403, {
        error: 'FORBIDDEN',
        message: 'Supplied userId does not match the authenticated identity.',
      });
    }

    const record = await getAuthoritativeEntitlement(verifiedUid, verifiedEmail);
    const isOwner = record.isOwner || isOwnerEmail(verifiedEmail);
    const limit = isOwner ? 999 : (record.tier === 'pro' ? 15 : (record.tier === 'plus' ? 5 : 1));
    const remaining = isOwner ? 999 : (DEV_ALLOW_UNLIMITED ? 999 : record.musicCredits);

    return sendJson(200, {
      userId: verifiedUid,
      tier: record.tier,
      subscriptionStatus: record.subscriptionStatus,
      musicCredits: remaining,
      used: isOwner ? 0 : Math.max(0, limit - record.musicCredits),
      limit: limit,
      remaining: remaining,
      periodStart: record.periodStart,
      periodEnd: record.periodEnd,
      isOwner: isOwner,
      isUnlimitedDev: DEV_ALLOW_UNLIMITED || isOwner,
    });
  }

  // ==========================================
  // Protected Endpoint: Initialize Paystack Checkout
  // POST /api/billing/initialize-checkout
  // ==========================================
  if (req.method === 'POST' && pathname === '/api/billing/initialize-checkout') {
    let authUser;
    try {
      authUser = await authenticateRequest(req);
    } catch (err) {
      return sendJson(err.statusCode || 401, {
        error: err.code || 'UNAUTHORIZED',
        message: err.message,
      });
    }

    const verifiedUid = authUser.uid;
    const verifiedEmail = authUser.email;
    const body = await readBody();
    const planId = (body.planId || '').toLowerCase().trim();

    try {
      const checkoutResult = await initializeCheckout(verifiedUid, verifiedEmail, planId);
      return sendJson(200, {
        success: true,
        authorizationUrl: checkoutResult.authorizationUrl,
        reference: checkoutResult.reference,
        planId: planId,
      });
    } catch (err) {
      console.error('[KASA Billing] Error initializing checkout:', err.message);
      return sendJson(err.statusCode || 500, {
        error: err.code || 'CHECKOUT_FAILED',
        message: err.message,
      });
    }
  }

  // ==========================================
  // Webhook Endpoint: Paystack Events (HMAC Verified)
  // POST /api/billing/paystack-webhook
  // ==========================================
  if (req.method === 'POST' && pathname === '/api/billing/paystack-webhook') {
    const rawBuffer = await readRawBody();
    const signatureHeader = req.headers['x-paystack-signature'] || '';

    // Verify HMAC SHA512 signature
    if (!verifyWebhookSignature(rawBuffer, signatureHeader)) {
      console.warn('[KASA Webhook] Received webhook with INVALID Paystack signature.');
      return sendJson(400, {
        error: 'INVALID_SIGNATURE',
        message: 'Paystack HMAC signature verification failed.',
      });
    }

    let event;
    try {
      event = JSON.parse(rawBuffer.toString('utf8'));
    } catch (e) {
      return sendJson(400, { error: 'INVALID_JSON', message: 'Malformed JSON payload.' });
    }

    const eventType = event.event;
    const eventData = event.data || {};
    console.log(`[KASA Webhook] Verified Paystack event received: ${eventType}`);

    try {
      if (eventType === 'charge.success') {
        const reference = eventData.reference;
        if (reference) {
          // Authoritatively verify with Paystack to prevent spoofing
          const verifiedData = await verifyPaystackTransaction(reference);
          const targetUserId = (verifiedData.metadata && verifiedData.metadata.userId) || eventData.customer.email;

          // Serialize execution under per-user mutex
          const releaseLock = await userMutex.acquire(targetUserId);
          try {
            await executeAtomicGrant(reference, verifiedData, 'charge.success');
            console.log(`[KASA Webhook] Successfully processed charge.success for reference: ${reference}`);
          } finally {
            releaseLock();
          }
        }
      } else if (eventType === 'refund.processed') {
        const reference = eventData.transaction_reference || eventData.reference;
        if (reference) {
          await executeRefundOrChargeback(reference, 'refund.processed', eventData.reason || 'Refunded');
        }
      } else if (eventType === 'charge.dispute.create') {
        const reference = eventData.transaction && eventData.transaction.reference;
        if (reference) {
          await executeRefundOrChargeback(reference, 'charge.dispute.create', eventData.reason || 'Disputed');
        }
      } else if (eventType === 'subscription.disable') {
        const subCode = eventData.subscription_code;
        if (subCode) {
          const db = getDb();
          await db.collection('subscriptions').doc(subCode).set({
            status: 'disabled',
            disabledAt: Date.now(),
            updatedAt: Date.now(),
          }, { merge: true });
        }
      }

      // Always return 200 OK to acknowledge receipt to Paystack
      return sendJson(200, { status: 'received' });
    } catch (err) {
      console.error(`[KASA Webhook] Error handling event ${eventType}:`, err.message);
      // Return 200 so Paystack does not loop if it was a data error, but log deeply
      return sendJson(200, { status: 'error_logged', message: err.message });
    }
  }

  // ==========================================
  // Protected Endpoint: Verify Session & Refresh Entitlements
  // POST /api/billing/verify-session
  // ==========================================
  if (req.method === 'POST' && pathname === '/api/billing/verify-session') {
    let authUser;
    try {
      authUser = await authenticateRequest(req);
    } catch (err) {
      return sendJson(err.statusCode || 401, {
        error: err.code || 'UNAUTHORIZED',
        message: err.message,
      });
    }

    const verifiedUid = authUser.uid;
    const verifiedEmail = authUser.email;
    const body = await readBody();
    const reference = (body.reference || '').trim();

    if (!reference) {
      return sendJson(400, { error: 'MISSING_REFERENCE', message: 'Payment reference is required.' });
    }

    try {
      // Authoritatively verify with Paystack
      const verifiedData = await verifyPaystackTransaction(reference);
      const paymentUserId = verifiedData.metadata && verifiedData.metadata.userId;

      if (paymentUserId && paymentUserId !== verifiedUid) {
        return sendJson(403, {
          error: 'FORBIDDEN',
          message: 'This payment reference does not belong to your authenticated identity.',
        });
      }

      // Serialize under per-user mutex
      const releaseLock = await userMutex.acquire(verifiedUid);
      let grantResult;
      try {
        grantResult = await executeAtomicGrant(reference, verifiedData, 'verify-session');
      } finally {
        releaseLock();
      }

      const freshEntitlement = await getAuthoritativeEntitlement(verifiedUid, verifiedEmail);
      const isOwner = freshEntitlement.isOwner || isOwnerEmail(verifiedEmail);
      const limit = isOwner ? 999 : (freshEntitlement.tier === 'pro' ? 15 : (freshEntitlement.tier === 'plus' ? 5 : 1));

      return sendJson(200, {
        success: true,
        alreadyProcessed: grantResult.alreadyProcessed,
        tier: freshEntitlement.tier,
        subscriptionStatus: freshEntitlement.subscriptionStatus,
        musicCredits: freshEntitlement.musicCredits,
        limit: limit,
        remaining: freshEntitlement.musicCredits,
        periodEnd: freshEntitlement.periodEnd,
        isOwner: isOwner,
      });
    } catch (err) {
      console.error('[KASA Billing] Error verifying payment session:', err.message);
      return sendJson(err.statusCode || 500, {
        error: err.code || 'VERIFY_FAILED',
        message: err.message,
      });
    }
  }

  // ==========================================
  // Protected Endpoint: Create Music Generation Task
  // POST /api/music/create
  // ==========================================
  if (req.method === 'POST' && pathname === '/api/music/create') {
    let authUser;
    try {
      authUser = await authenticateRequest(req);
    } catch (err) {
      return sendJson(err.statusCode || 401, {
        error: err.code || 'UNAUTHORIZED',
        message: err.message,
      });
    }

    const verifiedUid = authUser.uid;
    const verifiedEmail = authUser.email;
    const body = await readBody();
    const clientSuppliedUserId = body.userId || req.headers['x-user-id'];

    if (clientSuppliedUserId && clientSuppliedUserId !== verifiedUid && clientSuppliedUserId !== 'usr_default_kasa') {
      return sendJson(403, {
        error: 'FORBIDDEN',
        message: 'Supplied userId does not match the authenticated identity.',
      });
    }

    const prompt = (body.prompt || body.gpt_description_prompt || '').trim();
    if (!prompt) {
      return sendJson(400, {
        error: 'INVALID_PROMPT',
        message: 'Please provide a description of the song you want to generate.',
      });
    }

    const isOwner = isOwnerEmail(verifiedEmail);
    const isDevBypass = req.headers['x-kasa-dev-bypass'] === 'true' && DEV_ALLOW_UNLIMITED;
    const COST_PER_GENERATION = 1; // 1 KASA Music Credit per generation

    // 1. ATOMIC CREDIT CHECK & RESERVATION UNDER PER-USER MUTEX
    let reservedCredit = false;
    let entitlement;

    if (!isOwner && !isDevBypass && !DEV_ALLOW_UNLIMITED) {
      const releaseLock = await userMutex.acquire(verifiedUid);
      try {
        entitlement = await getAuthoritativeEntitlement(verifiedUid, verifiedEmail);

        if (entitlement.musicCredits < COST_PER_GENERATION) {
          return sendJson(403, {
            error: 'CREDIT_LIMIT_REACHED',
            message: `You have 0 KASA Music Credits remaining for this cycle. Upgrade to Plus or Pro to keep generating!`,
            tier: entitlement.tier,
            subscriptionStatus: entitlement.subscriptionStatus,
            musicCredits: entitlement.musicCredits,
            remaining: entitlement.musicCredits,
            periodEnd: entitlement.periodEnd,
          });
        }

        // Atomically debit / reserve the credit in Firestore
        const db = getDb();
        const newCredits = entitlement.musicCredits - COST_PER_GENERATION;
        await db.collection('entitlements').doc(verifiedUid).update({
          musicCredits: newCredits,
          updatedAt: Date.now(),
        });
        entitlement.musicCredits = newCredits;
        reservedCredit = true;
        console.log(`[KASA Backend] Reserved ${COST_PER_GENERATION} credit for user ${verifiedUid}. Remaining: ${newCredits}`);
      } finally {
        releaseLock();
      }
    } else {
      entitlement = await getAuthoritativeEntitlement(verifiedUid, verifiedEmail);
    }

    // 2. Check AIMusicAPI key configuration
    const apiKey = getApiKey();
    if (!apiKey) {
      // Refund reserved credit if backend is unconfigured
      if (reservedCredit) {
        const releaseRefund = await userMutex.acquire(verifiedUid);
        try {
          const db = getDb();
          const ref = db.collection('entitlements').doc(verifiedUid);
          const snap = await ref.get();
          if (snap.exists) {
            await ref.update({
              musicCredits: (snap.data().musicCredits || 0) + COST_PER_GENERATION,
              updatedAt: Date.now(),
            });
          }
        } finally {
          releaseRefund();
        }
      }
      console.warn('[KASA Backend] Music generation requested, but AIMUSIC_API_KEY is not configured.');
      return sendJson(503, {
        error: 'BACKEND_NOT_CONFIGURED',
        message: 'AIMusicAPI key is not configured on the KASA backend. Please supply AIMUSIC_API_KEY in the backend environment.',
      });
    }

    // 3. Build AIMusicAPI Sonic payload
    const sonicPayload = {
      custom_mode: false,
      mv: SONIC_MODEL,
      gpt_description_prompt: prompt,
      make_instrumental: !!body.instrumental,
    };

    if (body.title && body.title.trim()) {
      sonicPayload.title = body.title.trim();
    }
    if (body.tags && body.tags.trim()) {
      sonicPayload.tags = body.tags.trim();
    }

    // Helper to safely refund reserved credit upon upstream failure
    const rollbackReservation = async () => {
      if (!reservedCredit) return;
      const releaseRefund = await userMutex.acquire(verifiedUid);
      try {
        const db = getDb();
        const ref = db.collection('entitlements').doc(verifiedUid);
        const snap = await ref.get();
        if (snap.exists) {
          const restored = (snap.data().musicCredits || 0) + COST_PER_GENERATION;
          await ref.update({
            musicCredits: restored,
            updatedAt: Date.now(),
          });
          console.log(`[KASA Backend] Rolled back reserved credit for user ${verifiedUid}. Balance restored to ${restored}`);
        }
      } finally {
        releaseRefund();
      }
    };

    try {
      console.log(`[KASA Backend] Submitting music task to AIMusicAPI Sonic (${SONIC_MODEL}) for user ${verifiedUid.slice(0, 8)}...`);
      const upstreamRes = await fetch(`${AIMUSIC_BASE_URL}/create`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${apiKey}`,
        },
        body: JSON.stringify(sonicPayload),
      });

      const statusCode = upstreamRes.status;
      const rawText = await upstreamRes.text();
      let resJson = {};
      try {
        resJson = JSON.parse(rawText);
      } catch (e) {
        resJson = { message: rawText };
      }

      if (statusCode === 401) {
        await rollbackReservation();
        console.error('[KASA Backend] AIMusicAPI returned 401 Unauthorized (Invalid API Key).');
        return sendJson(401, {
          error: 'AUTH_ERROR',
          message: 'Music service authentication error. Please verify backend provider key.',
        });
      }

      if (statusCode === 403) {
        await rollbackReservation();
        console.error('[KASA Backend] AIMusicAPI returned 403 Forbidden (Provider account issue).');
        return sendJson(403, {
          error: 'INSUFFICIENT_CREDITS',
          message: 'Music generation is temporarily unavailable. Please try again later.',
        });
      }

      if (statusCode === 429) {
        await rollbackReservation();
        console.warn('[KASA Backend] AIMusicAPI rate limited (429).');
        return sendJson(429, {
          error: 'RATE_LIMITED',
          message: 'Too many requests right now. Please wait a moment and try again.',
        });
      }

      if (!upstreamRes.ok) {
        await rollbackReservation();
        console.error(`[KASA Backend] AIMusicAPI returned HTTP ${statusCode}:`, rawText);
        return sendJson(statusCode, {
          error: 'PROVIDER_ERROR',
          message: resJson.message || 'Music generation failed. Please try again.',
        });
      }

      // Extract task ID from upstream response
      let taskId = null;
      if (resJson.data && typeof resJson.data === 'object' && resJson.data.task_id) {
        taskId = resJson.data.task_id;
      } else if (resJson.task_id) {
        taskId = resJson.task_id;
      } else if (resJson.data && typeof resJson.data === 'string') {
        taskId = resJson.data;
      } else if (resJson.id) {
        taskId = resJson.id;
      }

      if (!taskId) {
        await rollbackReservation();
        console.error('[KASA Backend] Could not extract task_id from AIMusicAPI response:', rawText);
        return sendJson(502, {
          error: 'INVALID_PROVIDER_RESPONSE',
          message: 'Music service returned an unexpected response. Please try again.',
        });
      }

      // Record task in persistent ledger
      await recordTask({
        taskId,
        userId: verifiedUid,
        status: 'pending',
        costCredits: COST_PER_GENERATION,
        refunded: false,
        prompt,
      });

      console.log(`[KASA Backend] Task ${taskId} created for user ${verifiedUid}. Remaining credits: ${isOwner ? 999 : entitlement.musicCredits}`);

      return sendJson(200, {
        taskId: taskId,
        status: 'pending',
        message: 'Creating your song...',
        remainingCredits: isOwner ? 999 : (DEV_ALLOW_UNLIMITED ? 999 : entitlement.musicCredits),
        musicCredits: isOwner ? 999 : (DEV_ALLOW_UNLIMITED ? 999 : entitlement.musicCredits),
      });

    } catch (err) {
      await rollbackReservation();
      console.error('[KASA Backend] Network error communicating with AIMusicAPI:', err.message);
      return sendJson(503, {
        error: 'NETWORK_ERROR',
        message: "Couldn't connect to KASA Music. Check your internet connection and try again.",
      });
    }
  }

  // ==========================================
  // Protected Endpoint: Poll Task Status
  // GET /api/music/task/:taskId
  // ==========================================
  if (req.method === 'GET' && pathname.startsWith('/api/music/task/')) {
    let authUser;
    try {
      authUser = await authenticateRequest(req);
    } catch (err) {
      return sendJson(err.statusCode || 401, {
        error: err.code || 'UNAUTHORIZED',
        message: err.message,
      });
    }

    const verifiedUid = authUser.uid;
    const parts = pathname.split('/');
    const taskId = parts[parts.length - 1];

    if (!taskId) {
      return sendJson(400, { error: 'MISSING_TASK_ID', message: 'Task ID is required' });
    }

    // Task Ownership Verification:
    // A user can ONLY view tasks created by their own authenticated UID
    const taskRecord = await getTask(taskId);
    if (taskRecord && taskRecord.userId !== verifiedUid) {
      console.warn(`[KASA Security] User ${verifiedUid} attempted unauthorized access to task ${taskId} owned by ${taskRecord.userId}`);
      return sendJson(403, {
        error: 'FORBIDDEN',
        message: 'You do not have permission to access this task.',
      });
    }

    const apiKey = getApiKey();
    if (!apiKey) {
      return sendJson(503, {
        error: 'BACKEND_NOT_CONFIGURED',
        message: 'AIMusicAPI key is not configured on the KASA backend.',
      });
    }

    try {
      const pollUrl = `${AIMUSIC_BASE_URL}/task/${encodeURIComponent(taskId)}`;
      const upstreamRes = await fetch(pollUrl, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${apiKey}`,
        },
      });

      const statusCode = upstreamRes.status;
      const rawText = await upstreamRes.text();
      let resJson = {};
      try {
        resJson = JSON.parse(rawText);
      } catch (e) {
        resJson = { message: rawText };
      }

      if (statusCode === 401) {
        return sendJson(401, {
          error: 'AUTH_ERROR',
          message: 'Music service authentication error. Please contact support.',
        });
      }
      if (statusCode === 403) {
        return sendJson(403, {
          error: 'INSUFFICIENT_CREDITS',
          message: 'Music generation is temporarily unavailable. Please try again later.',
        });
      }
      if (statusCode === 429) {
        return sendJson(429, {
          error: 'RATE_LIMITED',
          message: 'Too many requests right now. Please wait a moment and try again.',
        });
      }
      if (!upstreamRes.ok) {
        return sendJson(statusCode, {
          error: 'PROVIDER_ERROR',
          message: resJson.message || 'Task status polling failed.',
        });
      }

      let taskState = 'pending';
      let clips = [];

      if (Array.isArray(resJson.data)) {
        clips = resJson.data;
        const allDone = clips.length > 0 && clips.every(c => c.state === 'succeeded' || c.audio_url);
        const anyFailed = clips.some(c => c.state === 'failed');
        if (allDone) {
          taskState = 'succeeded';
        } else if (anyFailed) {
          taskState = 'failed';
        } else {
          taskState = 'running';
        }
      } else if (resJson.data && typeof resJson.data === 'object') {
        taskState = resJson.data.state || resJson.state || 'running';
        if (Array.isArray(resJson.data.clips)) {
          clips = resJson.data.clips;
        } else if (resJson.data.audio_url) {
          clips = [resJson.data];
        }
      } else if (resJson.state) {
        taskState = resJson.state;
      }

      if (taskState === 'succeeded') {
        if (taskRecord && taskRecord.status !== 'succeeded') {
          taskRecord.status = 'succeeded';
          await updateTask(taskRecord);
        }

        const validClips = clips
          .filter(c => c && typeof c === 'object' && c.audio_url && typeof c.audio_url === 'string' && c.audio_url.trim().length > 0)
          .map((c, index) => ({
            id: c.id || `${taskId}_var${index + 1}`,
            title: c.title || 'Untitled Song',
            audioUrl: c.audio_url.trim(),
            duration: typeof c.duration === 'number' ? c.duration : 120.0,
            imageUrl: c.image_url || c.image_large_url || null,
            prompt: c.prompt || '',
            tags: c.tags || '',
          }));

        if (validClips.length === 0) {
          return sendJson(200, {
            status: 'running',
            message: 'Finishing your track...',
          });
        }

        return sendJson(200, {
          status: 'succeeded',
          message: validClips.length > 1 ? 'Your songs are ready 🎵' : 'Song ready 🎵',
          clips: validClips,
          clip: validClips[0],
        });
      }

      if (taskState === 'failed') {
        // IDEMPOTENT REFUND: Atomically refund spent credit in Firestore EXACTLY ONCE
        if (taskRecord && !taskRecord.refunded) {
          const releaseLock = await userMutex.acquire(taskRecord.userId);
          try {
            // Re-read task under mutex lock to eliminate polling race conditions
            const freshTask = await getTask(taskId);
            if (freshTask && !freshTask.refunded) {
              freshTask.refunded = true;
              freshTask.refundedAt = Date.now();
              freshTask.status = 'failed';
              await updateTask(freshTask);

              const db = getDb();
              const ref = db.collection('entitlements').doc(taskRecord.userId);
              const snap = await ref.get();
              if (snap.exists) {
                const restored = (snap.data().musicCredits || 0) + freshTask.costCredits;
                await ref.update({
                  musicCredits: restored,
                  updatedAt: Date.now(),
                });
                console.log(`[KASA Backend] Idempotently refunded ${freshTask.costCredits} credit to user ${taskRecord.userId} for task ${taskId}. New balance: ${restored}`);
              }
            }
          } finally {
            releaseLock();
          }
        }

        return sendJson(200, {
          status: 'failed',
          error: 'Song generation could not be completed. Please try with a different prompt.',
        });
      }

      // Still running / pending
      return sendJson(200, {
        status: 'running',
        message: 'Your song is being generated...',
      });

    } catch (err) {
      console.error('[KASA Backend] Error polling task:', err.message);
      return sendJson(503, {
        error: 'NETWORK_ERROR',
        message: "Couldn't connect to KASA Music. Check your internet connection and try again.",
      });
    }
  }

  // ==========================================
  // Development / Admin Endpoint (Hardened & Disabled in Production)
  // POST /api/music/dev/set-tier
  // ==========================================
  if (req.method === 'POST' && pathname === '/api/music/dev/set-tier') {
    if (process.env.NODE_ENV === 'production') {
      return sendJson(404, { error: 'NOT_FOUND', message: 'Endpoint not found' });
    }

    const adminSecret = process.env.ADMIN_API_SECRET;
    const providedSecret = req.headers['x-kasa-admin-secret'];

    if (!adminSecret || adminSecret.length < 32 || providedSecret !== adminSecret) {
      return sendJson(403, {
        error: 'FORBIDDEN',
        message: 'Admin authorization required.',
      });
    }

    const body = await readBody();
    const targetUserId = body.userId;
    if (!targetUserId) {
      return sendJson(400, { error: 'INVALID_REQUEST', message: 'userId is required' });
    }

    const db = getDb();
    const ref = db.collection('entitlements').doc(targetUserId);
    const updates = { updatedAt: Date.now() };
    if (['free', 'plus', 'pro'].includes(body.tier)) updates.tier = body.tier;
    if (typeof body.musicCredits === 'number') updates.musicCredits = body.musicCredits;
    if (body.subscriptionStatus) updates.subscriptionStatus = body.subscriptionStatus;

    await ref.set(updates, { merge: true });
    const snap = await ref.get();

    return sendJson(200, {
      success: true,
      data: snap.data(),
    });
  }

  // 404 for unknown endpoints
  sendJson(404, { error: 'NOT_FOUND', message: 'Endpoint not found' });
});

process.on('uncaughtException', (err) => {
  console.error('[KASA Backend uncaughtException]', err);
});

process.on('unhandledRejection', (reason, promise) => {
  console.error('[KASA Backend unhandledRejection]', reason);
});

server.on('error', (err) => {
  console.error('[KASA Backend Server Error]', err);
});

server.listen(PORT, '0.0.0.0', () => {
  const key = getApiKey();
  console.log(`====================================================`);
  console.log(`  KASA AI Secure Billing & Music Backend listening on port ${PORT}`);
  console.log(`  AIMUSIC_API_KEY: [${key ? 'CONFIGURED' : 'NOT_CONFIGURED'}]`);
  console.log(`  PAYSTACK_SECRET_KEY: [${process.env.PAYSTACK_SECRET_KEY ? 'CONFIGURED' : 'NOT_CONFIGURED'}]`);
  console.log(`  Paystack Mode: ${process.env.PAYSTACK_MODE || 'live'}`);
  console.log(`  Sonic Model: ${SONIC_MODEL}`);
  console.log(`  Dev Mode (Unlimited): ${DEV_ALLOW_UNLIMITED}`);
  console.log(`  Authoritative DB: Cloud Firestore (kasa-ai-6b7e1)`);
  console.log(`====================================================`);
});
