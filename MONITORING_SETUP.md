# 🚨 SyncWell Monitoring & Crash Reporting Setup

## Sentry Integration Complete

Your app now has production-ready error tracking and monitoring via Sentry.

---

## 📊 What's Configured

### ✅ Automatic Error Tracking
- Unhandled exceptions are automatically captured
- Stack traces with source maps included
- Device and app state context

### ✅ Performance Monitoring
- Track slow operations
- Identify bottlenecks
- View transaction timings

### ✅ Breadcrumbs
- User actions logged for context
- Network requests tracked
- Custom app events recorded

### ✅ Environment-Specific Settings
- **Development**: Full verbose logging, 100% error capture
- **Preview**: 10% performance sampling, errors + warnings
- **Production**: 10% performance sampling, errors only

---

## 🔧 Setup Instructions

### Step 1: Create Sentry Account & Project

1. Go to [sentry.io](https://sentry.io)
2. Sign up / Log in
3. Create a new project:
   - **Platform**: React Native
   - **Project Name**: SyncWell
   - **Team**: Your team or personal

4. You'll get a **DSN** (looks like: `https://xxxxx@xxxxx.ingest.sentry.io/12345`)

### Step 2: Add Sentry DSN to `.env` Files

**In your `.env` file:**
```
EXPO_PUBLIC_SENTRY_DSN=https://xxxxx@xxxxx.ingest.sentry.io/12345
```

**In your `.env.example`:**
```
EXPO_PUBLIC_SENTRY_DSN=https://your-sentry-dsn@sentry.io/project-id
```

**For each EAS profile in `eas.json`:**
```json
"development": {
  "env": {
    "EXPO_PUBLIC_SENTRY_DSN": ""  // Empty for local dev
  }
},
"preview": {
  "env": {
    "EXPO_PUBLIC_SENTRY_DSN": "$SENTRY_DSN_PREVIEW"
  }
},
"production": {
  "env": {
    "EXPO_PUBLIC_SENTRY_DSN": "$SENTRY_DSN_PRODUCTION"
  }
}
```

### Step 3: Install Sentry Expo Package

```bash
npm install sentry-expo
# or
yarn add sentry-expo
```

### Step 4: Done! ✅

The integration is already complete in your code:
- `src/services/SentryService.ts` - Error tracking service
- `app/_layout.tsx` - Initialized on app startup with breadcrumbs

---

## 📝 Usage Examples

### Capture an Error (for caught exceptions)

```typescript
import { captureException } from '@/src/services/SentryService';

try {
  await someFunctionThatMayFail();
} catch (error) {
  captureException(error, { 
    context: 'user_action',
    action: 'save_routine'
  });
}
```

### Capture a Message

```typescript
import { captureMessage } from '@/src/services/SentryService';

captureMessage('User completed first workout', 'info');
captureMessage('Failed to sync data', 'warning');
```

### Track User Actions (Breadcrumbs)

```typescript
import { addBreadcrumb } from '@/src/services/SentryService';

addBreadcrumb('User logged in', { userId: '123' }, 'auth');
addBreadcrumb('Workout saved', { duration: 45 }, 'workout');
addBreadcrumb('API error', { status: 500 }, 'api');
```

### Set User Context

```typescript
import { setSentryUser, clearSentryUser } from '@/src/services/SentryService';

// After login
setSentryUser('user-123', 'user@example.com', 'john_doe');

// After logout
clearSentryUser();
```

---

## 🎯 Best Practices

### 1. Set User on Login
```typescript
// In LoginScreen or auth service
setSentryUser(user.uid, user.email);
```

### 2. Add Breadcrumbs for Important Flows
```typescript
// Routine tracking
addBreadcrumb('Routine started', { routineId }, 'routine');
addBreadcrumb('Item marked complete', { itemId }, 'routine');
addBreadcrumb('Routine submitted', { itemsCompleted }, 'routine');
```

### 3. Capture Performance
```typescript
// Expensive operations
const start = Date.now();
await expensiveOperation();
addBreadcrumb(`Operation took ${Date.now() - start}ms`, {}, 'performance');
```

### 4. Different Severity Levels
```typescript
captureMessage('User action', 'debug');      // Quiet
captureMessage('Configuration loaded', 'info');    // Info
captureMessage('API rate limited', 'warning');     // Warning
captureMessage('Payment failed', 'error');   // Error
captureMessage('Server down', 'fatal');      // Critical
```

---

## 📊 Viewing Errors in Sentry Dashboard

1. Log in to [sentry.io](https://sentry.io)
2. Go to your **SyncWell** project
3. View errors in real-time:
   - **Issues**: List of unique errors
   - **Releases**: Errors by app version
   - **Performance**: Slow transactions
   - **Alerts**: Set up email/Slack notifications

---

## 🔐 Privacy & GDPR

Sentry by default captures:
- Device OS/model
- App version
- Stack traces
- Breadcrumbs
- Request URL/method
- NO personal data (unless you explicitly add it)

### Sensitive Data Filtering

If you need to exclude certain data:

```typescript
// In SentryService.ts, customize:
Sentry.init({
  beforeSend(event) {
    // Filter out certain breadcrumbs
    if (event.breadcrumbs) {
      event.breadcrumbs = event.breadcrumbs.filter(
        (bc) => !bc.message?.includes('password')
      );
    }
    return event;
  },
});
```

---

## 🚀 Next: Deploy and Monitor

### Build Development Version
```bash
eas build -p ios --profile development
eas build -p android --profile development
```

### Build Preview (with Sentry preview DSN)
```bash
eas build -p ios --profile preview
eas build -p android --profile preview
```

Errors from preview builds will show in Sentry under your preview environment.

### Build Production (with Sentry production DSN)
```bash
eas build -p ios --profile production
eas build -p android --profile production
```

All errors will be tracked in production environment.

---

## 📞 Troubleshooting

### Sentry not capturing errors?
1. Check `.env` has `EXPO_PUBLIC_SENTRY_DSN` set
2. Check `app/_layout.tsx` has `initializeSentry()` call
3. Check DSN is valid (starts with `https://`)

### Seeing too many errors?
1. Adjust `tracesSampleRate` in `SentryService.ts` (currently 10% for production)
2. Set `debug: false` for production

### Want to test?
```typescript
// Manually throw an error to test
import { captureException } from '@/src/services/SentryService';

// In a button or on app start
captureException(new Error('Test error from SyncWell'));
```

---

## ✅ Checklist

- [ ] Created Sentry account at sentry.io
- [ ] Created SyncWell project
- [ ] Got DSN from Sentry
- [ ] Added DSN to `.env` files
- [ ] Installed `sentry-expo` package
- [ ] Tested error capture
- [ ] Set up team/project notifications
- [ ] Configured release tracking in Sentry

**Status**: Ready for production monitoring! 🚀
