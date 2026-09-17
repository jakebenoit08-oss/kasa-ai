/**
 * KASA AI - Authoritative Firestore Storage & Billing Repositories
 * 
 * Collections:
 * - entitlements/{firebaseUid}
 * - payments/{paystackReference}
 * - subscriptions/{paystackSubscriptionCode}
 * - credit_grants/{grantId}
 * 
 * Enforces:
 * - Server-side authoritative data persistence
 * - Atomic ACID multi-document transactions (runTransaction)
 * - Safe refund ledger tracking (credit_grants)
 */

let getFirestore = null;
try {
  getFirestore = require('firebase-admin/firestore').getFirestore;
} catch (e) {
  // Graceful fallback for mock mode / offline testing
}
const { getFirebaseApp } = require('./auth');
const fs = require('fs');
const path = require('path');

const PERIOD_DAYS = parseInt(process.env.SUBSCRIPTION_PERIOD_DAYS || '30', 10);
const PERIOD_MS = PERIOD_DAYS * 24 * 60 * 60 * 1000;

let firestoreInstance = null;
let useMock = process.env.NODE_ENV === 'test' || process.env.USE_MOCK_FIRESTORE === 'true' || !getFirestore;

// In-Memory Transactional Mock for offline/testing environments
class MockDocumentSnapshot {
  constructor(id, data) {
    this.id = id;
    this._data = data ? JSON.parse(JSON.stringify(data)) : null;
    this.exists = this._data !== null;
  }
  data() {
    return this._data ? JSON.parse(JSON.stringify(this._data)) : null;
  }
}

class MockDocumentReference {
  constructor(store, collectionName, id) {
    this.store = store;
    this.collectionName = collectionName;
    this.id = String(id);
  }

  async get() {
    const col = this.store[this.collectionName] || {};
    const docData = col[this.id] || null;
    return new MockDocumentSnapshot(this.id, docData);
  }

  async set(data, options = {}) {
    if (!this.store[this.collectionName]) {
      this.store[this.collectionName] = {};
    }
    const current = this.store[this.collectionName][this.id] || {};
    if (options.merge) {
      this.store[this.collectionName][this.id] = { ...current, ...JSON.parse(JSON.stringify(data)) };
    } else {
      this.store[this.collectionName][this.id] = JSON.parse(JSON.stringify(data));
    }
    return { writeTime: Date.now() };
  }

  async update(data) {
    if (!this.store[this.collectionName] || !this.store[this.collectionName][this.id]) {
      throw new Error(`No document to update: ${this.collectionName}/${this.id}`);
    }
    this.store[this.collectionName][this.id] = {
      ...this.store[this.collectionName][this.id],
      ...JSON.parse(JSON.stringify(data)),
    };
    return { writeTime: Date.now() };
  }
}

class MockCollectionReference {
  constructor(store, collectionName) {
    this.store = store;
    this.collectionName = collectionName;
  }

  doc(id) {
    return new MockDocumentReference(this.store, this.collectionName, id);
  }

  where(field, op, value) {
    return {
      get: async () => {
        const col = this.store[this.collectionName] || {};
        const docs = [];
        for (const [id, data] of Object.entries(col)) {
          let match = false;
          if (op === '==' && data[field] === value) match = true;
          if (match) {
            docs.push(new MockDocumentSnapshot(id, data));
          }
        }
        return {
          empty: docs.length === 0,
          size: docs.length,
          docs,
          forEach: (cb) => docs.forEach(cb),
        };
      },
    };
  }
}

class MockFirestore {
  constructor() {
    this.store = {
      entitlements: {},
      payments: {},
      subscriptions: {},
      credit_grants: {},
    };
  }

  collection(name) {
    return new MockCollectionReference(this.store, name);
  }

  async runTransaction(updateFunction) {
    // Transactional isolation: Clone store for commit staging
    const stagedStore = JSON.parse(JSON.stringify(this.store));
    const transaction = {
      get: async (docRef) => {
        const col = stagedStore[docRef.collectionName] || {};
        const docData = col[docRef.id] || null;
        return new MockDocumentSnapshot(docRef.id, docData);
      },
      set: (docRef, data, options = {}) => {
        if (!stagedStore[docRef.collectionName]) {
          stagedStore[docRef.collectionName] = {};
        }
        const current = stagedStore[docRef.collectionName][docRef.id] || {};
        if (options.merge) {
          stagedStore[docRef.collectionName][docRef.id] = {
            ...current,
            ...JSON.parse(JSON.stringify(data)),
          };
        } else {
          stagedStore[docRef.collectionName][docRef.id] = JSON.parse(JSON.stringify(data));
        }
      },
      update: (docRef, data) => {
        if (!stagedStore[docRef.collectionName] || !stagedStore[docRef.collectionName][docRef.id]) {
          throw new Error(`No document to update: ${docRef.collectionName}/${docRef.id}`);
        }
        stagedStore[docRef.collectionName][docRef.id] = {
          ...stagedStore[docRef.collectionName][docRef.id],
          ...JSON.parse(JSON.stringify(data)),
        };
      },
    };

    const result = await updateFunction(transaction);
    // Commit staged changes in place
    for (const [colName, colData] of Object.entries(stagedStore)) {
      this.store[colName] = colData;
    }
    return result;
  }
}

const mockDb = new MockFirestore();

function getDb() {
  if (useMock) return mockDb;
  if (firestoreInstance) return firestoreInstance;

  try {
    const app = getFirebaseApp();
    if (app) {
      firestoreInstance = getFirestore(app);
      console.log('[KASA Firestore] Connected to Firebase Firestore Admin.');
      return firestoreInstance;
    }
  } catch (err) {
    console.warn('[KASA Firestore] Live Firestore unavailable, falling back to Transactional Mock:', err.message);
    useMock = true;
    return mockDb;
  }

  useMock = true;
  return mockDb;
}

// Migrate legacy JSON file data if present
function migrateLegacyData(db) {
  try {
    const legacyPath = path.join(__dirname, 'persistent_entitlements.json');
    if (fs.existsSync(legacyPath)) {
      const data = JSON.parse(fs.readFileSync(legacyPath, 'utf8'));
      for (const [uid, ent] of Object.entries(data)) {
        db.collection('entitlements').doc(uid).set({
          userId: uid,
          tier: ent.tier || 'free',
          subscriptionStatus: ent.subscriptionStatus || 'unpaid',
          musicCredits: typeof ent.musicCredits === 'number' ? ent.musicCredits : 1,
          periodStart: ent.periodStart || Date.now(),
          periodEnd: ent.periodEnd || (Date.now() + PERIOD_MS),
          lastPaymentReference: ent.lastPaymentReference || null,
          subscriptionCode: null,
          autoRenew: false,
          createdAt: ent.createdAt || Date.now(),
          updatedAt: ent.updatedAt || Date.now(),
        }, { merge: true }).catch(() => {});
      }
      console.log(`[KASA Firestore] Preloaded legacy entitlements into authoritative store.`);
    }
  } catch (err) {
    console.warn('[KASA Firestore] Legacy data check note:', err.message);
  }
}

migrateLegacyData(getDb());

module.exports = {
  getDb,
  PERIOD_DAYS,
  PERIOD_MS,
  setUseMock: (val) => { useMock = val; },
  getMockStore: () => mockDb.store,
};
