# 🔐 SyncWell Security Setup Guide

## Sensitive Credentials & Local Setup
To run SyncWell locally, sensitive secrets must be placed in your local uncommitted configuration files or `.env`.

### 1. Local Credentials (`creds-for-app.json`)
Create `creds-for-app.json` in the project root:
```json
{
    "clientId": "YOUR_GOOGLE_CLIENT_ID",
    "clientSecret": "YOUR_GOOGLE_CLIENT_SECRET"
}
```

### 2. Google Services (`google-services.json`)
Download your `google-services.json` from Firebase Console and place it in the project root.
(A template is provided in `google-services.example.json`.)

### 3. Environment Variables (`.env`)
Create `.env` using `.env.example` as a template:
```env
# Firebase Configuration
EXPO_PUBLIC_FIREBASE_PROJECT_ID=your-project-id
EXPO_PUBLIC_FIREBASE_AUTH_DOMAIN=your-project-id.firebaseapp.com
EXPO_PUBLIC_FIREBASE_API_KEY=your-api-key
EXPO_PUBLIC_FIREBASE_STORAGE_BUCKET=your-project-id.firebasestorage.app
EXPO_PUBLIC_FIREBASE_MESSAGING_SENDER_ID=your-sender-id
EXPO_PUBLIC_FIREBASE_APP_ID=your-app-id
EXPO_PUBLIC_FIREBASE_MEASUREMENT_ID=your-measurement-id
FIREBASE_ANDROID_API_KEY=your-android-key

# Google OAuth Configuration
EXPO_PUBLIC_GOOGLE_OAUTH_CLIENT_ID=your-client-id
GOOGLE_OAUTH_CLIENT_SECRET=your-client-secret

# Backend API Configuration
EXPO_PUBLIC_API_BASE_URL=https://api.example.com
EXPO_PUBLIC_SENTRY_DSN=https://your-sentry-dsn

# App Configuration
EXPO_PUBLIC_APP_ENV=development
EXPO_PUBLIC_APP_VERSION=1.0.0
```

> **Note:** `.env`, `creds-for-app.json`, and `google-services.json` are listed in `.gitignore` and must NEVER be committed to version control.
