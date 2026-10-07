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

// ==========================================
// GEMINI MULTI-LAYER SERVICE
// Supports both @google/generative-ai SDK and Native REST API
// ==========================================
let GoogleGenerativeAIClass = null;
try {
  const sdk = require('@google/generative-ai');
  GoogleGenerativeAIClass = sdk.GoogleGenerativeAI;
} catch (e) {
  // SDK optional; REST fallback works natively without dependencies
}

async function executeGeminiChat(apiKey, systemInstruction, userMessage, conversationHistory = []) {
  const modelsToTry = ['gemini-3.8-flash', 'gemini-flash-latest'];

  // Normalize conversation history format
  const sanitizedHistory = (conversationHistory || []).slice(-8).map(h => ({
    role: (h.role === 'assistant' || h.role === 'model') ? 'model' : 'user',
    parts: [{ text: (h.text || h.content || '').slice(0, 2000) }]
  })).filter(h => h.parts[0].text.length > 0);

  // 1. Try with GoogleGenerativeAI SDK if installed
  if (GoogleGenerativeAIClass) {
    try {
      const genAI = new GoogleGenerativeAIClass(apiKey);
      for (const modelName of modelsToTry) {
        try {
          const model = genAI.getGenerativeModel({
            model: modelName,
            systemInstruction: systemInstruction,
            generationConfig: { temperature: 0.7, maxOutputTokens: 1000 }
          });
          const chat = model.startChat({ history: sanitizedHistory });
          const result = await Promise.race([
            chat.sendMessage(userMessage),
            new Promise((_, reject) => setTimeout(() => reject(new Error('SDK_TIMEOUT')), 20000))
          ]);
          const text = result?.response?.text();
          if (text && text.trim()) return { text: text.trim(), modelUsed: modelName };
        } catch (mErr) {
          console.warn(`[KASA Chat SDK] Model ${modelName} failed: ${mErr.message}`);
        }
      }
    } catch (sdkErr) {
      console.warn(`[KASA Chat SDK] Init failed (${sdkErr.message}), falling back to direct REST...`);
    }
  }

  // 2. Direct Native REST Fallback (Reliable, fast, zero SDK overhead)
  for (const modelName of modelsToTry) {
    const url = `https://generativelanguage.googleapis.com/v1beta/models/${modelName}:generateContent?key=${encodeURIComponent(apiKey)}`;
    const contents = [
      ...sanitizedHistory,
      { role: 'user', parts: [{ text: userMessage }] }
    ];

    const payload = {
      system_instruction: { parts: [{ text: systemInstruction }] },
      contents: contents,
      generationConfig: { temperature: 0.7, maxOutputTokens: 1000 }
    };

    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 20000);

    try {
      const res = await fetch(url, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
        signal: controller.signal
      });
      clearTimeout(timeoutId);

      if (res.ok) {
        const data = await res.json();
        const reply = data.candidates?.[0]?.content?.parts?.[0]?.text;
        if (reply && reply.trim()) return { text: reply.trim(), modelUsed: modelName };
      } else {
        const errJson = await res.json().catch(() => ({}));
        console.warn(`[KASA Chat REST] ${modelName} returned HTTP ${res.status}:`, errJson.error?.message || res.statusText);
      }
    } catch (fetchErr) {
      clearTimeout(timeoutId);
      console.warn(`[KASA Chat REST] ${modelName} fetch error: ${fetchErr.message}`);
    }
  }

  throw new Error('All Gemini connection attempts timed out or failed. Please retry.');
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
      geminiConfigured: !!(process.env.GEMINI_API_KEY || process.env.GOOGLE_API_KEY),
      devMode: DEV_ALLOW_UNLIMITED,
      timestamp: Date.now(),
    });
  }

  // ==========================================
  // GEMINI AI CHAT ROUTE
  // POST /api/chat, /api/gemini/chat, /api/ai/chat
  // ==========================================
  if (req.method === 'POST' && (pathname === '/api/chat' || pathname === '/api/gemini/chat' || pathname === '/api/ai/chat')) {
    let authUser;
    try {
      authUser = await authenticateRequest(req);
    } catch (err) {
      return sendJson(err.statusCode || 401, { error: err.code || 'UNAUTHORIZED', message: err.message });
    }

    const body = await readBody();
    const message = (body.message || body.prompt || '').trim();
    const history = body.history || [];

    if (!message) {
      return sendJson(400, { error: 'INVALID_PROMPT', message: 'Message is required.' });
    }

    const geminiKey = process.env.GEMINI_API_KEY || process.env.GOOGLE_API_KEY || process.env.GEMINI_KEY;
    if (!geminiKey) {
      return sendJson(503, {
        error: 'BACKEND_NOT_CONFIGURED',
        message: 'GEMINI_API_KEY is not configured in backend environment variables.'
      });
    }

    const systemPrompt = "You are KASA AI, Ghana's premier AI companion. You understand English, Ghanaian Pidgin, and Ghanaian languages (Twi, Fante, Ga, Ewe). Be warm, witty, culturally attuned to Ghanaian life, and concise.";

    try {
      console.log(`[KASA Chat] Processing message from user ${authUser.uid.slice(0, 8)}...`);
      const { text, modelUsed } = await executeGeminiChat(geminiKey, systemPrompt, message, history);
      console.log(`[KASA Chat] Responded successfully via ${modelUsed}`);

      return sendJson(200, {
        reply: text,
        text: text,
        response: text,
        modelUsed: modelUsed
      });
    } catch (err) {
      console.error('[KASA Chat Error]', err.message);
      return sendJson(500, {
        error: 'CHAT_FAILED',
        message: err.message.includes('timed out') ? 'Gemini response took too long, please retry.' : err.message
      });
    }
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
          const verifiedData = await verifyPaystackTransaction(reference);
          const targetUserId = (verifiedData.metadata && verifiedData.metadata.userId) || eventData.customer.email;
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
      return sendJson(200, { status: 'received' });
    } catch (err) {
      console.error(`[KASA Webhook] Error handling event ${eventType}:`, err.message);
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
      const verifiedData = await verifyPaystackTransaction(reference);
      const paymentUserId = verifiedData.metadata && verifiedData.metadata.userId;

      if (paymentUserId && paymentUserId !== verifiedUid) {
        return sendJson(403, {
          error: 'FORBIDDEN',
          message: 'This payment reference does not belong to your authenticated identity.',
        });
      }

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
    const COST_PER_GENERATION = 1;

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

    const apiKey = getApiKey();
    if (!apiKey) {
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

    const sonicPayload = {
      custom_mode: false,
      mv: SONIC_MODEL,
      gpt_description_prompt: prompt,
      make_instrumental: !!body.instrumental,
    };
    if (body.title && body.title.trim()) sonicPayload.title = body.title.trim();
    if (body.tags && body.tags.trim()) sonicPayload.tags = body.tags.trim();

    const rollbackReservation = async () => {
      if (!reservedCredit) return;
      const releaseRefund = await userMutex.acquire(verifiedUid);
      try {
        const db = getDb();
        const ref = db.collection('entitlements').doc(verifiedUid);
        const snap = await ref.get();
        if (snap.exists) {
          const restored = (snap.data().musicCredits || 0) + COST_PER_GENERATION;
          await ref.update({ musicCredits: restored, updatedAt: Date.now() });
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
        headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${apiKey}` },
        body: JSON.stringify(sonicPayload),
      });
      const statusCode = upstreamRes.status;
      const rawText = await upstreamRes.text();
      let resJson = {};
      try { resJson = JSON.parse(rawText); } catch (e) { resJson = { message: rawText }; }

      if (statusCode === 401) { await rollbackReservation(); return sendJson(401, { error: 'AUTH_ERROR', message: 'Music service authentication error.' }); }
      if (statusCode === 403) { await rollbackReservation(); return sendJson(403, { error: 'INSUFFICIENT_CREDITS', message: 'Music generation temporarily unavailable.' }); }
      if (statusCode === 429) { await rollbackReservation(); return sendJson(429, { error: 'RATE_LIMITED', message: 'Too many requests.' }); }
      if (!upstreamRes.ok) { await rollbackReservation(); return sendJson(statusCode, { error: 'PROVIDER_ERROR', message: resJson.message || 'Music generation failed.' }); }

      let taskId = null;
      if (resJson.data && resJson.data.task_id) taskId = resJson.data.task_id;
      else if (resJson.task_id) taskId = resJson.task_id;
      else if (resJson.data && typeof resJson.data === 'string') taskId = resJson.data;
      else if (resJson.id) taskId = resJson.id;

      if (!taskId) { await rollbackReservation(); return sendJson(502, { error: 'INVALID_PROVIDER_RESPONSE', message: 'Unexpected response.' }); }

      await recordTask({ taskId, userId: verifiedUid, status: 'pending', costCredits: COST_PER_GENERATION, refunded: false, prompt });
      console.log(`[KASA Backend] Task ${taskId} created for user ${verifiedUid}.`);
      return sendJson(200, { taskId, status: 'pending', message: 'Creating your song...', remainingCredits: isOwner ? 999 : entitlement.musicCredits, musicCredits: isOwner ? 999 : entitlement.musicCredits });
    } catch (err) {
      await rollbackReservation();
      return sendJson(503, { error: 'NETWORK_ERROR', message: "Couldn't connect to KASA Music." });
    }
  }

  // ==========================================
  // Protected Endpoint: Poll Task Status
  // GET /api/music/task/:taskId
  // ==========================================
  if (req.method === 'GET' && pathname.startsWith('/api/music/task/')) {
    let authUser;
    try { authUser = await authenticateRequest(req); } catch (err) { return sendJson(err.statusCode || 401, { error: err.code || 'UNAUTHORIZED', message: err.message }); }
    const verifiedUid = authUser.uid;
    const parts = pathname.split('/');
    const taskId = parts[parts.length - 1];
    if (!taskId) return sendJson(400, { error: 'MISSING_TASK_ID', message: 'Task ID is required' });
    const taskRecord = await getTask(taskId);
    if (taskRecord && taskRecord.userId !== verifiedUid) return sendJson(403, { error: 'FORBIDDEN', message: 'No permission.' });
    const apiKey = getApiKey();
    if (!apiKey) return sendJson(503, { error: 'BACKEND_NOT_CONFIGURED', message: 'Key not configured.' });
    try {
      const pollUrl = `${AIMUSIC_BASE_URL}/task/${encodeURIComponent(taskId)}`;
      const upstreamRes = await fetch(pollUrl, { method: 'GET', headers: { 'Authorization': `Bearer ${apiKey}` } });
      const statusCode = upstreamRes.status;
      const rawText = await upstreamRes.text();
      let resJson = {};
      try { resJson = JSON.parse(rawText); } catch (e) { resJson = { message: rawText }; }
      if (statusCode === 401) return sendJson(401, { error: 'AUTH_ERROR', message: 'Auth error.' });
      if (statusCode === 403) return sendJson(403, { error: 'INSUFFICIENT_CREDITS', message: 'Unavailable.' });
      if (statusCode === 429) return sendJson(429, { error: 'RATE_LIMITED', message: 'Too many requests.' });
      if (!upstreamRes.ok) return sendJson(statusCode, { error: 'PROVIDER_ERROR', message: resJson.message || 'Polling failed.' });

      let taskState = 'pending'; let clips = [];
      if (Array.isArray(resJson.data)) {
        clips = resJson.data;
        const allDone = clips.length > 0 && clips.every(c => c.state === 'succeeded' || c.audio_url);
        const anyFailed = clips.some(c => c.state === 'failed');
        if (allDone) taskState = 'succeeded'; else if (anyFailed) taskState = 'failed'; else taskState = 'running';
      } else if (resJson.data && typeof resJson.data === 'object') {
        taskState = resJson.data.state || resJson.state || 'running';
        if (Array.isArray(resJson.data.clips)) clips = resJson.data.clips;
        else if (resJson.data.audio_url) clips = [resJson.data];
      } else if (resJson.state) taskState = resJson.state;

      if (taskState === 'succeeded') {
        if (taskRecord && taskRecord.status !== 'succeeded') { taskRecord.status = 'succeeded'; await updateTask(taskRecord); }
        const validClips = clips.filter(c => c && c.audio_url && c.audio_url.trim().length > 0).map((c, index) => ({ id: c.id || `${taskId}_var${index + 1}`, title: c.title || 'Untitled Song', audioUrl: c.audio_url.trim(), duration: typeof c.duration === 'number' ? c.duration : 120.0, imageUrl: c.image_url || c.image_large_url || null, prompt: c.prompt || '', tags: c.tags || '' }));
        if (validClips.length === 0) return sendJson(200, { status: 'running', message: 'Finishing your track...' });
        return sendJson(200, { status: 'succeeded', message: validClips.length > 1 ? 'Your songs are ready 🎵' : 'Song ready 🎵', clips: validClips, clip: validClips[0] });
      }
      if (taskState === 'failed') {
        if (taskRecord && !taskRecord.refunded) {
          const releaseLock = await userMutex.acquire(taskRecord.userId);
          try {
            const freshTask = await getTask(taskId);
            if (freshTask && !freshTask.refunded) {
              freshTask.refunded = true; freshTask.refundedAt = Date.now(); freshTask.status = 'failed'; await updateTask(freshTask);
              const db = getDb(); const ref = db.collection('entitlements').doc(taskRecord.userId); const snap = await ref.get();
              if (snap.exists) { const restored = (snap.data().musicCredits || 0) + freshTask.costCredits; await ref.update({ musicCredits: restored, updatedAt: Date.now() }); }
            }
          } finally { releaseLock(); }
        }
        return sendJson(200, { status: 'failed', error: 'Song generation could not be completed.' });
      }
      return sendJson(200, { status: 'running', message: 'Your song is being generated...' });
    } catch (err) {
      return sendJson(503, { error: 'NETWORK_ERROR', message: "Couldn't connect." });
    }
  }

  // ==========================================
  // Development / Admin Endpoint (Hardened & Disabled in Production)
  // POST /api/music/dev/set-tier
  // ==========================================
  if (req.method === 'POST' && pathname === '/api/music/dev/set-tier') {
    if (process.env.NODE_ENV === 'production') return sendJson(404, { error: 'NOT_FOUND', message: 'Endpoint not found' });
    const adminSecret = process.env.ADMIN_API_SECRET;
    const providedSecret = req.headers['x-kasa-admin-secret'];
    if (!adminSecret || adminSecret.length < 32 || providedSecret !== adminSecret) return sendJson(403, { error: 'FORBIDDEN', message: 'Admin authorization required.' });
    const body = await readBody();
    const targetUserId = body.userId;
    if (!targetUserId) return sendJson(400, { error: 'INVALID_REQUEST', message: 'userId is required' });
    const db = getDb(); const ref = db.collection('entitlements').doc(targetUserId);
    const updates = { updatedAt: Date.now() };
    if (['free', 'plus', 'pro'].includes(body.tier)) updates.tier = body.tier;
    if (typeof body.musicCredits === 'number') updates.musicCredits = body.musicCredits;
    if (body.subscriptionStatus) updates.subscriptionStatus = body.subscriptionStatus;
    await ref.set(updates, { merge: true }); const snap = await ref.get();
    return sendJson(200, { success: true, data: snap.data() });
  }

  // 404 for unknown endpoints
  sendJson(404, { error: 'NOT_FOUND', message: 'Endpoint not found' });
});

process.on('uncaughtException', (err) => { console.error('[KASA Backend uncaughtException]', err); });
process.on('unhandledRejection', (reason, promise) => { console.error('[KASA Backend unhandledRejection]', reason); });
server.on('error', (err) => { console.error('[KASA Backend Server Error]', err); });
server.listen(PORT, '0.0.0.0', () => {
  const key = getApiKey();
  console.log(`====================================================`);
  console.log(`  KASA AI Secure Billing & Music Backend listening on port ${PORT}`);
  console.log(`  AIMUSIC_API_KEY: [${key ? 'CONFIGURED' : 'NOT_CONFIGURED'}]`);
  console.log(`  PAYSTACK_SECRET_KEY: [${process.env.PAYSTACK_SECRET_KEY ? 'CONFIGURED' : 'NOT_CONFIGURED'}]`);
  console.log(`  GEMINI_API_KEY: [${process.env.GEMINI_API_KEY ? 'CONFIGURED: ' + maskSecret(process.env.GEMINI_API_KEY) : 'NOT_CONFIGURED'}]`);
  console.log(`  Paystack Mode: ${process.env.PAYSTACK_MODE || 'live'}`);
  console.log(`  Sonic Model: ${SONIC_MODEL}`);
  console.log(`  Dev Mode (Unlimited): ${DEV_ALLOW_UNLIMITED}`);
  console.log(`  Authoritative DB: Cloud Firestore (kasa-ai-6b7e1)`);
  console.log(`====================================================`);
});
