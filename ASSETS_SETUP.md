# 🎨 SyncWell Production Assets Guide

## Asset Requirements

### 1. App Icon (1024×1024 px)
The main icon used across all platforms.

**Location:** `assets/icon.png`

### 2. Splash Screen (1242×2436 px for iPhone)
Loading screen shown when app launches.

**Location:** `assets/splash.png`

### 3. Adaptive Icon (for Android)
**Foreground:** `assets/adaptive-icon-foreground.png` (1024×1024)
**Background:** `assets/adaptive-icon-background.png` (1024×1024)

---

## Quick Setup with Expo

Run this command to generate default assets:

```bash
npx expo-app-icon-generator --input assets/icon.png --output assets
```

Or manually create using a design tool (Figma, Adobe XD, Sketch):

### Design Specifications:

**Icon:**
- Format: PNG with transparency
- Size: 1024×1024 px
- Safe zone: Center 512×512 px
- Color: Match brand (emerald green #10B981)

**Splash Screen:**
- Format: PNG with transparency
- Size: 1242×2436 px (iPhone) / 1080×1920 px (Android)
- Content: Logo centered with app name

**Adaptive Icon:**
- Foreground: Icon element (512×512 safe zone within 1024×1024)
- Background: Solid color or gradient
- Monet support: Automatic on Android 13+

---

## Current app.json Configuration

Already set up with icon and splash references:

```json
{
  "icon": "./assets/icon.png",
  "splash": {
    "image": "./assets/splash.png",
    "resizeMode": "contain",
    "backgroundColor": "#ffffff"
  },
  "android": {
    "adaptiveIcon": {
      "foregroundImage": "./assets/adaptive-icon-foreground.png",
      "backgroundImage": "./assets/adaptive-icon-background.png"
    }
  }
}
```

---

## Asset Checklist

- [ ] `assets/icon.png` (1024×1024, PNG)
- [ ] `assets/splash.png` (1242×2436, PNG)
- [ ] `assets/adaptive-icon-foreground.png` (1024×1024, PNG)
- [ ] `assets/adaptive-icon-background.png` (1024×1024, PNG)

Once created, run:
```bash
eas build -p ios --profile development
eas build -p android --profile development
```

The EAS build system will automatically resize assets for all screen sizes.
