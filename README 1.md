# FTT Signal Android Repo

এই package-এ `same logic + same function` রাখা হয়েছে এবং Android shell-টা polished করা হয়েছে যাতে repo push করলেই GitHub Actions থেকে debug APK build করা যায়।

## কী আছে
- Existing `app.html` logic intact
- Native Android wrapper with WebView bridge
- Background scanning service
- Native notifications + vibration
- Cleaner loading/splash experience
- Fixed Gradle wrapper files
- GitHub Actions debug APK workflow

## GitHub এ push করলে কীভাবে build হবে
1. নতুন GitHub repo তৈরি করো
2. এই ZIP extract করে সব file push করো
3. `Actions` tab এ যাও
4. `Build FTT Signal APK` workflow run করো
5. Build শেষ হলে `Artifacts` থেকে `FTTSignal-debug` download করো

## Local build
```bash
chmod +x gradlew
./gradlew assembleDebug
```

## Debug APK output
`app/build/outputs/apk/debug/app-debug.apk`

## Notes
- API base URL app-এর settings থেকে change করা যাবে
- Watchlist background scan service native layer দিয়ে handle হয়
- Notification permission Android 13+ এ automatically request হবে
