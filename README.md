# FTTs — Signal Engine Android App

Native Android app for the FTT Signal Worker engine (v6.9.0).

## Features
- Live trading signals — Forex, Crypto, OTC
- Dual AI validation (Cerebras + Groq)
- Signal history with win/loss tracking
- OTC manual result reporting
- Session-aware pair selection
- Auto-refresh every 60 seconds
- Dark theme

## Build

### Debug APK via GitHub Actions
Push to `main` → GitHub Actions automatically builds debug APK → Download from **Actions → Artifacts**

### Local build
```bash
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

## Configuration

Worker URL is set in `app/build.gradle.kts`:
```kotlin
buildConfigField("String", "WORKER_BASE_URL",
    "\"https://your-worker.workers.dev\"")
```

## Requirements
- Android 8.0+ (API 26)
- Internet permission
- JDK 17

## Stack
- Kotlin + Jetpack Compose
- Retrofit + OkHttp
- Material3 dark theme
- Navigation Compose
- ViewModel + StateFlow
