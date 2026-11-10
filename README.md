# SyncWell

Android app (Kotlin, Jetpack Compose, Room) for daily routines, workouts, focus timers, to-dos, water, supplements and weight. Data lives on the device. Optional Google sign-in backs it up to a hidden folder in the user's own Google Drive.

## Build

```
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleRelease
```

Use Java 17. Without signing variables, `assembleRelease` signs with the debug key (local R8 checks only; Play rejects it).

## Release signing

Set `KEYSTORE_PATH`, `STORE_PASSWORD` and `KEY_PASSWORD` (key alias `upload`) before building the Play bundle.

## Google setup (needed for sign-in and backup)

1. Enable the Google Drive API in the Google Cloud project.
2. OAuth consent screen: add the `drive.appdata` scope; set to "In production" for release.
3. Create an Android OAuth client for package `com.syncwell.app` with the SHA-1 of the debug, upload and Play App Signing keys.
4. The web client ID is in `app/src/main/res/values/strings.xml` (`google_web_client_id`).

## Policies

`PRIVACY_POLICY.md` and `TERMS_OF_SERVICE.md` are published at https://varun21vaidya.github.io/syncwell-legal/ (source repo: `syncwell-legal`). Keep both copies in sync when editing.
