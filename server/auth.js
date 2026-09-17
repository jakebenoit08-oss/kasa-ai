/**
 * KASA AI - Authentication & Token Verification Service
 * 
 * Verifies Firebase Auth ID Tokens (JWTs) using Firebase Admin SDK.
 * Extracts and returns the verified decodedToken.uid as the authoritative user ID.
 */

let initializeApp, getApps, cert, getAuth;
try {
  const fbApp = require('firebase-admin/app');
  initializeApp = fbApp.initializeApp;
  getApps = fbApp.getApps;
  cert = fbApp.cert;
  getAuth = require('firebase-admin/auth').getAuth;
} catch (e) {
  // Graceful fallback for offline testing
}
const fs = require('fs');
const path = require('path');

const FIREBASE_PROJECT_ID = process.env.FIREBASE_PROJECT_ID || 'kasa-ai-6b7e1';

let firebaseApp = null;
let firebaseAuth = null;

function initFirebase() {
  if (firebaseAuth) return firebaseAuth;
  if (!getAuth || !getApps || !initializeApp) {
    return null;
  }

  try {
    if (getApps().length === 0) {
      const saKey = process.env.FIREBASE_SERVICE_ACCOUNT_KEY || process.env.FIREBASE_SERVICE_ACCOUNT_JSON;
      if (saKey) {
        let creds;
        if (saKey.trim().startsWith('{')) {
          creds = JSON.parse(saKey);
        } else if (fs.existsSync(saKey)) {
          creds = JSON.parse(fs.readFileSync(saKey, 'utf8'));
        }
        if (creds) {
          firebaseApp = initializeApp({
            credential: cert(creds),
            projectId: creds.project_id || FIREBASE_PROJECT_ID,
          });
        }
      }

      if (!firebaseApp) {
        // Initialize with project ID (uses Google public certs for ID token verification)
        firebaseApp = initializeApp({
          projectId: FIREBASE_PROJECT_ID,
        });
      }
    } else {
      firebaseApp = getApps()[0];
    }

    firebaseAuth = getAuth(firebaseApp);
    console.log(`[KASA Auth] Firebase Admin initialized for project: ${FIREBASE_PROJECT_ID}`);
  } catch (err) {
    console.error('[KASA Auth] Error initializing Firebase Admin:', err.message);
  }

  return firebaseAuth;
}

function getFirebaseApp() {
  if (!firebaseApp) {
    initFirebase();
  }
  return firebaseApp;
}

// In-flight init
initFirebase();

/**
 * Authenticates an incoming HTTP request.
 * 
 * Extracts Bearer token from 'Authorization' header.
 * Verifies with Firebase Auth.
 * 
 * @param {import('http').IncomingMessage} req
 * @returns {Promise<{ uid: string, email?: string, token: string }>}
 */
async function authenticateRequest(req) {
  const authHeader = req.headers['authorization'] || '';
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    const error = new Error('Authorization header with Bearer token is required');
    error.statusCode = 401;
    error.code = 'UNAUTHORIZED';
    throw error;
  }

  const token = authHeader.slice(7).trim();
  if (!token) {
    const error = new Error('Bearer token cannot be empty');
    error.statusCode = 401;
    error.code = 'UNAUTHORIZED';
    throw error;
  }

  // Development/Test bypass: ONLY active when explicitly configured in non-production test environments
  if ((process.env.NODE_ENV === 'test' || process.env.ALLOW_DEV_TEST_AUTH === 'true') && token.startsWith('test_token_')) {
    const testUid = token.replace('test_token_', '');
    return {
      uid: testUid,
      email: `${testUid}@kasa.ai.test`,
      token: token,
      isTest: true,
    };
  }

  const auth = initFirebase();
  if (!auth) {
    const error = new Error('Firebase Auth service is unavailable on backend');
    error.statusCode = 503;
    error.code = 'AUTH_SERVICE_UNAVAILABLE';
    throw error;
  }

  try {
    const decodedToken = await auth.verifyIdToken(token);
    if (!decodedToken || !decodedToken.uid) {
      const error = new Error('Token verification failed: missing uid');
      error.statusCode = 401;
      error.code = 'INVALID_TOKEN';
      throw error;
    }

    return {
      uid: decodedToken.uid,
      email: decodedToken.email,
      token: token,
      decoded: decodedToken,
    };
  } catch (err) {
    console.warn(`[KASA Auth] Token verification rejected: ${err.message}`);
    const error = new Error(`Invalid authentication token: ${err.message}`);
    error.statusCode = 401;
    error.code = 'INVALID_TOKEN';
    throw error;
  }
}

module.exports = {
  authenticateRequest,
  initFirebase,
  getFirebaseApp,
};
