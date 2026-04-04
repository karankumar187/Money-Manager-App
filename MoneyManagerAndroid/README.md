# MoneyManager Android

Native Android app (Kotlin + Jetpack Compose) sharing the same Firebase backend as the iOS app.

## Setup Steps

### 1. Add google-services.json
- Go to Firebase Console → Project Settings → Add App → Android
- Package name: `com.moneymanager.app`
- Download `google-services.json`
- Place it in `app/google-services.json`

### 2. Open in Android Studio
- Open `MoneyManagerAndroid/` folder in Android Studio (not the root)
- Wait for Gradle sync
- Replace `YOUR_WEB_CLIENT_ID` in `AuthScreen.kt` with your Web Client ID from Firebase Console → Authentication → Sign-in method → Google

### 3. Get your SHA256 fingerprint (for App Links)
In Android Studio terminal:
```
./gradlew signingReport
```
Or from your keystore:
```
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```
Copy the `SHA-256` value into `firebase-hosting/public/.well-known/assetlinks.json`

### 4. Deploy Firebase Hosting (for deep links)
```bash
cd firebase-hosting
npm install -g firebase-tools
firebase login
firebase use --add   # select your moneymanager project
firebase deploy --only hosting
```

### 5. iOS Universal Links
In `firebase-hosting/public/.well-known/apple-app-site-association`:
- Replace `REPLACE_WITH_TEAMID` with your Apple Team ID (from developer.apple.com)
- In Xcode → Signing & Capabilities → Add "Associated Domains" capability
- Add: `applinks:moneymanager.web.app`

### 6. Build APK
```
Build → Build Bundle(s)/APK(s) → Build APK(s)
```
APK will be at: `app/build/outputs/apk/debug/app-debug.apk`

## Deep Link Test URLs

| What | URL to test |
|------|-------------|
| UPI payment | `upi://pay?pa=test@ybl&pn=TestUser&am=100&cu=INR` |
| App Link pay | `https://moneymanager.web.app/pay?pa=test@ybl&pn=TestUser&am=100` |
| Contact card | `https://moneymanager.web.app/contact?name=Rahul&phone=%2B919876543210` |

Test deep links using ADB:
```bash
adb shell am start -W -a android.intent.action.VIEW -d "upi://pay?pa=test@upi&pn=Test&am=100&cu=INR" com.moneymanager.app
```

## Features Parity with iOS

| Feature | iOS | Android |
|---------|-----|---------|
| Phone OTP Auth | ✅ | ✅ |
| Google Sign-In | ✅ | ✅ |
| Dashboard + Budget | ✅ | ✅ |
| Transaction History | ✅ | ✅ |
| Lend / Borrow | ✅ | ✅ |
| Split Bill | ✅ | ✅ |
| Person Detail Card | ✅ | ✅ |
| UPI Auto-fill from account | ✅ | ✅ |
| Profile Pic Sync | ✅ | ✅ |
| Two-way Payment Sync | ✅ | ✅ |
| Split badge | ✅ | ✅ |
| Partial Payments | ✅ | ✅ |
| UPI Universal Deep Links | iOS Universal Links | Android App Links + `upi://` |
| Push Notifications (FCM) | 🔜 | ✅ wired |
