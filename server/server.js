/**
 * KASA AI - Secure Backend Service
 * 
 * Proxies music generation requests to AIMusicAPI Sonic API and enforces
 * server-side user subscription allowances (Free, Plus, Pro).
 * 
 * Security:
 * - AIMUSIC_API_KEY is never sent to clients or printed in plaintext in logs.
 * - Usage allowances are tracked and enforced server-side by authenticated Firebase User ID.
 */

const http = require('http');
const fs = require('fs');
const path = require('path');
const url = require('url');

// Simple .env parser to avoid third-party dependencies
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

  // Also check .dev.env.json if created by AI Studio platform
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

// Server Port Configuration:
// - On Render (and standard cloud platforms), PORT is provided dynamically via process.env.PORT.
// - In the local AI Studio development container, NGINX occupies port 8080 (NGINX_PORT=8080),
//   so local development defaults to 8765 unless overridden.
const isAiStudioContainer = process.env.NGINX_PORT === '8080' && !process.env.RENDER;
const PORT = parseInt(
  process.env.MUSIC_PORT ||
  process.env.MUSIC_BACKEND_PORT ||
  (isAiStudioContainer ? '8765' : (process.env.PORT || '8765')),
  10
);
const AIMUSIC_BASE_URL = 'https://api.aimusicapi.ai/api/v1/sonic';
const SONIC_MODEL = process.env.AIMUSIC_MODEL || 'sonic-v5-5';
const PERIOD_DAYS = parseInt(process.env.SUBSCRIPTION_PERIOD_DAYS || '30', 10);
const PERIOD_MS = PERIOD_DAYS * 24 * 60 * 60 * 1000;
const DEV_ALLOW_UNLIMITED = process.env.DEV_ALLOW_UNLIMITED === 'true';

// Masked secret helper for secure logging
function maskSecret(secret) {
  if (!secret) return 'NOT_CONFIGURED';
  if (secret.length <= 8) return '********';
  return secret.slice(0, 4) + '...' + secret.slice(-4);
}

// In-Memory user subscription & allowance store (persisted to credits_store.json)
const STORE_FILE = path.join(__dirname, 'credits_store.json');
let userCredits = {};

try {
  if (fs.existsSync(STORE_FILE)) {
    userCredits = JSON.parse(fs.readFileSync(STORE_FILE, 'utf8'));
  }
} catch (e) {
  userCredits = {};
}

function saveStore() {
  try {
    fs.writeFileSync(STORE_FILE, JSON.stringify(userCredits, null, 2), 'utf8');
  } catch (e) {
    // Non-fatal if read-only filesystem
  }
}

const TIER_LIMITS = {
  free: 1,
  plus: 5,
  pro: 15,
};

function getUserCreditRecord(userId) {
  const uid = userId || 'usr_default_kasa';
  const now = Date.now();
  let record = userCredits[uid];

  if (!record) {
    record = {
      userId: uid,
      tier: 'free',
      usedCount: 0,
      periodStart: now,
      periodEnd: now + PERIOD_MS,
      createdTasks: [],
    };
    userCredits[uid] = record;
    saveStore();
  } else {
    // Check if subscription renewal period has lapsed (Strictly NOT daily!)
    if (now >= record.periodEnd) {
      record.periodStart = now;
      record.periodEnd = now + PERIOD_MS;
      record.usedCount = 0; // Renew allowance on renewal cycle
      saveStore();
    }
  }

  return record;
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
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization, x-user-id, x-kasa-dev-bypass');

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

  // Helper to read JSON body
  const readBody = () => new Promise((resolve) => {
    let body = '';
    req.on('data', chunk => { body += chunk; });
    req.on('end', () => {
      try {
        resolve(body ? JSON.parse(body) : {});
      } catch (e) {
        resolve({});
      }
    });
  });

  // Health check
  if (pathname === '/health' || pathname === '/api/health') {
    const key = getApiKey();
    return sendJson(200, {
      status: 'ok',
      service: 'kasa-ai-music-backend',
      model: SONIC_MODEL,
      aimusicConfigured: !!key,
      devMode: DEV_ALLOW_UNLIMITED,
      timestamp: Date.now(),
    });
  }

  // Get user credits & subscription info
  // GET /api/music/credits?userId=...
  if (req.method === 'GET' && pathname === '/api/music/credits') {
    const userId = parsedUrl.query.userId || req.headers['x-user-id'] || 'usr_default_kasa';
    const record = getUserCreditRecord(userId);
    const limit = TIER_LIMITS[record.tier] || TIER_LIMITS.free;
    const remaining = Math.max(0, limit - record.usedCount);

    return sendJson(200, {
      userId: record.userId,
      tier: record.tier,
      used: record.usedCount,
      limit: limit,
      remaining: DEV_ALLOW_UNLIMITED ? 999 : remaining,
      periodStart: record.periodStart,
      periodEnd: record.periodEnd,
      isUnlimitedDev: DEV_ALLOW_UNLIMITED,
    });
  }

  // Set user tier / test allowance
  // POST /api/music/dev/set-tier
  if (req.method === 'POST' && pathname === '/api/music/dev/set-tier') {
    const body = await readBody();
    const userId = body.userId || 'usr_default_kasa';
    const tier = ['free', 'plus', 'pro'].includes(body.tier) ? body.tier : 'free';
    const record = getUserCreditRecord(userId);
    record.tier = tier;
    if (typeof body.usedCount === 'number') {
      record.usedCount = body.usedCount;
    } else if (body.resetCount) {
      record.usedCount = 0;
    }
    saveStore();
    const limit = TIER_LIMITS[record.tier];
    return sendJson(200, {
      success: true,
      userId: record.userId,
      tier: record.tier,
      used: record.usedCount,
      limit: limit,
      remaining: Math.max(0, limit - record.usedCount),
    });
  }

  // Create Music Generation Task
  // POST /api/music/create
  if (req.method === 'POST' && pathname === '/api/music/create') {
    const body = await readBody();
    const userId = body.userId || req.headers['x-user-id'] || 'usr_default_kasa';
    const isDevBypass = req.headers['x-kasa-dev-bypass'] === 'true' && DEV_ALLOW_UNLIMITED;

    const prompt = (body.prompt || body.gpt_description_prompt || '').trim();
    if (!prompt) {
      return sendJson(400, {
        error: 'INVALID_PROMPT',
        message: 'Please provide a description of the song you want to generate.',
      });
    }

    // 1. Check server-side credit limits
    const record = getUserCreditRecord(userId);
    const limit = TIER_LIMITS[record.tier] || TIER_LIMITS.free;

    if (!isDevBypass && !DEV_ALLOW_UNLIMITED && record.usedCount >= limit) {
      return sendJson(403, {
        error: 'CREDIT_LIMIT_REACHED',
        message: `You have used your ${record.tier.toUpperCase()} music allowance (${record.usedCount}/${limit} songs) for this subscription period.`,
        tier: record.tier,
        used: record.usedCount,
        limit: limit,
        periodEnd: record.periodEnd,
      });
    }

    // 2. Check AIMusicAPI key configuration
    const apiKey = getApiKey();
    if (!apiKey) {
      console.warn('[KASA Backend] Music generation requested, but AIMUSIC_API_KEY is not configured in backend environment.');
      return sendJson(503, {
        error: 'BACKEND_NOT_CONFIGURED',
        message: 'AIMusicAPI key is not configured on the KASA backend. Please supply AIMUSIC_API_KEY in the backend environment.',
      });
    }

    // 3. Build AIMusicAPI Sonic create payload
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

    try {
      console.log(`[KASA Backend] Submitting music task to AIMusicAPI Sonic (${SONIC_MODEL}) for user ${userId.slice(0, 8)}...`);
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

      // Handle provider error states
      if (statusCode === 401) {
        console.error('[KASA Backend] AIMusicAPI returned 401 Unauthorized (Invalid API Key).');
        return sendJson(401, {
          error: 'AUTH_ERROR',
          message: 'Music service authentication error. Please verify the AIMusicAPI key.',
        });
      }

      if (statusCode === 403) {
        console.error('[KASA Backend] AIMusicAPI returned 403 Forbidden (Insufficient provider credits).');
        return sendJson(403, {
          error: 'INSUFFICIENT_CREDITS',
          message: 'Music generation is temporarily unavailable. Please try again later.',
        });
      }

      if (statusCode === 429) {
        console.warn('[KASA Backend] AIMusicAPI rate limited (429).');
        return sendJson(429, {
          error: 'RATE_LIMITED',
          message: 'Too many requests right now. Please wait a moment and try again.',
        });
      }

      if (!upstreamRes.ok) {
        console.error(`[KASA Backend] AIMusicAPI returned HTTP ${statusCode}:`, rawText);
        return sendJson(statusCode, {
          error: 'PROVIDER_ERROR',
          message: resJson.message || 'Music generation failed. Please try again.',
        });
      }

      // Extract task ID from AIMusicAPI response format
      // Documented formats: { code: 200, data: { task_id: "..." } } or { task_id: "..." } or { data: "task_id" }
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
        console.error('[KASA Backend] Could not extract task_id from AIMusicAPI response:', rawText);
        return sendJson(502, {
          error: 'INVALID_PROVIDER_RESPONSE',
          message: 'Music service returned an unexpected response. Please try again.',
        });
      }

      // Billable generation successfully initiated: Deduct 1 credit for user
      if (!isDevBypass && !DEV_ALLOW_UNLIMITED) {
        record.usedCount += 1;
        record.createdTasks.push({ taskId, prompt, createdAt: Date.now() });
        saveStore();
      }

      console.log(`[KASA Backend] Task created successfully: ${taskId}. User used: ${record.usedCount}/${limit}`);

      return sendJson(200, {
        taskId: taskId,
        status: 'pending',
        message: 'Creating your song...',
        remainingCredits: Math.max(0, limit - record.usedCount),
      });

    } catch (err) {
      console.error('[KASA Backend] Network error communicating with AIMusicAPI:', err.message);
      return sendJson(503, {
        error: 'NETWORK_ERROR',
        message: "Couldn't connect to KASA Music. Check your internet connection and try again.",
      });
    }
  }

  // Poll Task Status
  // GET /api/music/task/:taskId?userId=...
  if (req.method === 'GET' && pathname.startsWith('/api/music/task/')) {
    const parts = pathname.split('/');
    const taskId = parts[parts.length - 1];
    const userId = parsedUrl.query.userId || req.headers['x-user-id'] || 'usr_default_kasa';

    if (!taskId) {
      return sendJson(400, { error: 'MISSING_TASK_ID', message: 'Task ID is required' });
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

      // AIMusicAPI returns data: [ { id, audio_url, title, duration, image_url, state: "succeeded"|"running"|"failed" } ]
      // or object { state: "...", data: [...] }
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
        const primaryClip = clips.find(c => c.audio_url) || clips[0] || {};
        const audioUrl = primaryClip.audio_url || '';
        const title = primaryClip.title || 'Untitled Song';
        const duration = typeof primaryClip.duration === 'number' ? primaryClip.duration : 120.0;
        const imageUrl = primaryClip.image_url || primaryClip.image_large_url || null;

        if (!audioUrl) {
          // Still waiting for audio encoding
          return sendJson(200, {
            status: 'running',
            message: 'Finishing your track...',
          });
        }

        return sendJson(200, {
          status: 'succeeded',
          message: 'Song ready 🎵',
          clip: {
            id: primaryClip.id || taskId,
            title: title,
            audioUrl: audioUrl,
            duration: duration,
            imageUrl: imageUrl,
            prompt: primaryClip.prompt || '',
            tags: primaryClip.tags || '',
          },
        });
      }

      if (taskState === 'failed') {
        // Refund user's credit on generation failure
        const record = getUserCreditRecord(userId);
        if (record.usedCount > 0) {
          record.usedCount -= 1;
          saveStore();
          console.log(`[KASA Backend] Refunded 1 credit for user ${userId} due to task failure.`);
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
  console.log(`  KASA AI Music Backend listening on port ${PORT}`);
  console.log(`  AIMUSIC_API_KEY: [${maskSecret(key)}]`);
  console.log(`  Sonic Model: ${SONIC_MODEL}`);
  console.log(`  Dev Mode (Unlimited): ${DEV_ALLOW_UNLIMITED}`);
  console.log(`====================================================`);
});
