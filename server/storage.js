/**
 * KASA AI - Persistent Entitlements & Music Credit Wallet Storage
 * 
 * Provides:
 * - Thread-safe, atomic per-user Mutex locks to eliminate race conditions
 * - Persistent storage for User Entitlements (tier, subscriptionStatus, musicCredits wallet)
 * - Persistent Music Task Ledger with idempotent refund tracking
 * - Atomic write-and-rename disk persistence to survive crashes and Render restarts
 * - Non-destructive migration from legacy credits_store.json
 */

const fs = require('fs');
const path = require('path');

const PERIOD_DAYS = parseInt(process.env.SUBSCRIPTION_PERIOD_DAYS || '30', 10);
const PERIOD_MS = PERIOD_DAYS * 24 * 60 * 60 * 1000;

const DATA_DIR = process.env.KASA_DATA_DIR || __dirname;
const ENTITLEMENTS_FILE = path.join(DATA_DIR, 'persistent_entitlements.json');
const TASKS_FILE = path.join(DATA_DIR, 'music_task_ledger.json');
const LEGACY_STORE_FILE = path.join(DATA_DIR, 'credits_store.json');

// Per-user async Mutex to serialize credit operations and eliminate race conditions
class UserMutex {
  constructor() {
    this.locks = new Map();
  }

  async acquire(userId) {
    const key = String(userId);
    while (this.locks.has(key)) {
      await this.locks.get(key);
    }
    let resolver;
    const promise = new Promise((resolve) => {
      resolver = resolve;
    });
    this.locks.set(key, promise);
    return () => {
      this.locks.delete(key);
      resolver();
    };
  }
}

const userMutex = new UserMutex();

// In-memory caches backed by atomic disk files
let entitlementsCache = {};
let tasksCache = {};

function atomicWriteJson(filePath, data) {
  const tmpPath = `${filePath}.${Date.now()}.${Math.random().toString(36).slice(2, 8)}.tmp`;
  try {
    const dir = path.dirname(filePath);
    if (!fs.existsSync(dir)) {
      fs.mkdirSync(dir, { recursive: true });
    }
    fs.writeFileSync(tmpPath, JSON.stringify(data, null, 2), 'utf8');
    fs.renameSync(tmpPath, filePath);
  } catch (err) {
    console.error(`[KASA Storage] Failed to write ${filePath}:`, err.message);
    try {
      if (fs.existsSync(tmpPath)) fs.unlinkSync(tmpPath);
    } catch (_) {}
  }
}

function loadEntitlementsFromDisk() {
  if (fs.existsSync(ENTITLEMENTS_FILE)) {
    try {
      entitlementsCache = JSON.parse(fs.readFileSync(ENTITLEMENTS_FILE, 'utf8'));
      console.log(`[KASA Storage] Loaded ${Object.keys(entitlementsCache).length} user entitlements from persistent storage.`);
      return;
    } catch (err) {
      console.error('[KASA Storage] Error parsing entitlements file:', err.message);
    }
  }

  // Non-destructive fallback / migration from legacy credits_store.json if present
  if (fs.existsSync(LEGACY_STORE_FILE)) {
    try {
      const legacy = JSON.parse(fs.readFileSync(LEGACY_STORE_FILE, 'utf8'));
      for (const [uid, rec] of Object.entries(legacy)) {
        if (!entitlementsCache[uid]) {
          const used = typeof rec.usedCount === 'number' ? rec.usedCount : 0;
          const limit = rec.tier === 'pro' ? 15 : (rec.tier === 'plus' ? 5 : 1);
          entitlementsCache[uid] = {
            userId: uid,
            tier: rec.tier || 'free',
            subscriptionStatus: 'unpaid',
            musicCredits: Math.max(0, limit - used),
            periodStart: rec.periodStart || Date.now(),
            periodEnd: rec.periodEnd || (Date.now() + PERIOD_MS),
            createdAt: rec.periodStart || Date.now(),
            updatedAt: Date.now(),
          };
        }
      }
      console.log(`[KASA Storage] Migrated ${Object.keys(legacy).length} legacy credit records into persistent entitlements.`);
      atomicWriteJson(ENTITLEMENTS_FILE, entitlementsCache);
    } catch (err) {
      console.error('[KASA Storage] Error reading legacy store:', err.message);
    }
  }
}

function loadTasksFromDisk() {
  if (fs.existsSync(TASKS_FILE)) {
    try {
      tasksCache = JSON.parse(fs.readFileSync(TASKS_FILE, 'utf8'));
      console.log(`[KASA Storage] Loaded ${Object.keys(tasksCache).length} task records from task ledger.`);
    } catch (err) {
      console.error('[KASA Storage] Error parsing task ledger:', err.message);
    }
  }
}

// Initial load
loadEntitlementsFromDisk();
loadTasksFromDisk();

/**
 * Gets or creates an entitlement record for a verified user UID.
 * Checks 30-day period expiration and resets free allowance if cycle elapsed.
 */
async function getOrCreateEntitlement(userId) {
  const uid = String(userId);
  const now = Date.now();
  let record = entitlementsCache[uid];

  if (!record) {
    record = {
      userId: uid,
      tier: 'free',
      subscriptionStatus: 'unpaid',
      musicCredits: 1, // Default 1 KASA Music Credit for new users
      periodStart: now,
      periodEnd: now + PERIOD_MS,
      createdAt: now,
      updatedAt: now,
    };
    entitlementsCache[uid] = record;
    atomicWriteJson(ENTITLEMENTS_FILE, entitlementsCache);
  } else {
    // Check if subscription renewal window has elapsed
    if (now >= record.periodEnd) {
      record.periodStart = now;
      record.periodEnd = now + PERIOD_MS;
      // Free users receive renewal of 1 credit per cycle if depleted
      if (record.subscriptionStatus !== 'active' && record.tier === 'free') {
        record.musicCredits = Math.max(record.musicCredits, 1);
      }
      record.updatedAt = now;
      atomicWriteJson(ENTITLEMENTS_FILE, entitlementsCache);
    }
  }

  return record;
}

/**
 * Atomically saves an updated entitlement record.
 */
async function saveEntitlement(record) {
  record.updatedAt = Date.now();
  entitlementsCache[record.userId] = record;
  atomicWriteJson(ENTITLEMENTS_FILE, entitlementsCache);
}

/**
 * Records a new task in the persistent ledger.
 */
async function recordTask(taskData) {
  const now = Date.now();
  const task = {
    taskId: taskData.taskId,
    userId: taskData.userId,
    status: taskData.status || 'pending',
    costCredits: typeof taskData.costCredits === 'number' ? taskData.costCredits : 1,
    refunded: !!taskData.refunded,
    refundedAt: taskData.refundedAt || null,
    prompt: taskData.prompt || null,
    createdAt: taskData.createdAt || now,
    updatedAt: now,
  };
  tasksCache[task.taskId] = task;
  atomicWriteJson(TASKS_FILE, tasksCache);
  return task;
}

/**
 * Gets a task by ID.
 */
async function getTask(taskId) {
  return tasksCache[taskId] || null;
}

/**
 * Updates a task in the persistent ledger.
 */
async function updateTask(task) {
  task.updatedAt = Date.now();
  tasksCache[task.taskId] = task;
  atomicWriteJson(TASKS_FILE, tasksCache);
}

module.exports = {
  userMutex,
  getOrCreateEntitlement,
  saveEntitlement,
  recordTask,
  getTask,
  updateTask,
  PERIOD_DAYS,
  PERIOD_MS,
};
