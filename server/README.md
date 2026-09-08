# KASA AI Backend Service

Secure backend proxy and credit enforcement service for KASA AI Music Studio.

## Architecture

```text
KASA Android Client
       │
       ▼ (HTTPS / HTTP)
KASA Secure Backend (this service)
       │ (Enforces user tiers, credits, 30-day renewal periods)
       ▼ (Bearer Authorization: AIMUSIC_API_KEY)
AIMusicAPI Sonic API (https://api.aimusicapi.ai/api/v1/sonic/)
       │
       ▼
Music Generation & Task Polling
       │
       ▼
Audio Result Streamed to KASA Android
```

## Security Guarantees

1. **No Client-Side Secrets**: `AIMUSIC_API_KEY` is strictly held on this server and NEVER sent to the Android APK or stored in Android code/resources.
2. **Key Masking**: The API key is masked in all logs and status endpoints.
3. **Server-Side Quota Enforcement**:
   - **Free Tier**: 1 generation
   - **Plus Tier**: 5 generations per 30-day subscription period
   - **Pro Tier**: 15 generations per 30-day subscription period
   - Allowance is tied to the authenticated user ID and resets strictly on subscription renewal (every 30 days), NOT daily.

## Running the Server

### 1. Environment Configuration
Copy `.env.example` to `.env` and enter your AIMusicAPI key:

```bash
cp .env.example .env
# Edit .env and set:
# AIMUSIC_API_KEY=your_aimusicapi_key_here
```

### 2. Start the Server (Local)

```bash
npm start
# Server starts on http://localhost:8765
```

Zero external dependencies required (uses Node 18+ native HTTP and fetch).

## Deploying to Render (Free Web Service)

1. **Create Web Service on Render**:
   - In your Render Dashboard ([dashboard.render.com](https://dashboard.render.com)), click **New +** ➔ **Web Service**.
   - Connect your repository.
   - **Root Directory**: `server` (or leave blank if repository root is `/server`).
   - **Runtime**: `Node`.
   - **Build Command**: `npm install`
   - **Start Command**: `node server.js`

2. **Set Environment Variables in Render Dashboard**:
   - `AIMUSIC_API_KEY`: Your secret key from AIMusicAPI.
   - `NODE_ENV`: `production`

3. **Copy your Render HTTPS URL**:
   - Render assigns a URL such as `https://kasa-music-service.onrender.com`.
   - Copy this URL into Android `AppConfig.kt` (`MUSIC_BACKEND_PRODUCTION_URL`) or configure it directly in the KASA app under **Settings ➔ Music Studio Backend**.

## API Endpoints

- `GET /health` : Health check and provider configuration status.
- `GET /api/music/credits?userId={userId}` : Returns current user plan, usage, limit, and renewal date.
- `POST /api/music/create` : Submits prompt to AIMusicAPI Sonic (`/api/v1/sonic/create`).
- `GET /api/music/task/{taskId}?userId={userId}` : Polls task status (`/api/v1/sonic/task/{task_id}`).
- `POST /api/music/dev/set-tier` : Testing utility to simulate Free, Plus, and Pro tiers or reset credits.
