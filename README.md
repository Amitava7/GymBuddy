# GymBuddy

A personal gym tracking app built with React Native and Expo. Built because existing apps on the market were missing key features.

Track workouts across multiple gyms, manage exercises, and log sets with weight and reps — all stored locally on-device via SQLite.

---

## Features

- Manage multiple gyms
- Create and organize custom exercises
- Build reusable workout templates
- Log workout sessions with sets, weight (kg), and reps
- View workout history and summaries
- Dark theme UI
- Fully offline — no account needed

---

## Tech Stack

| Layer | Tech |
|---|---|
| Framework | React Native + Expo (SDK 55) |
| Language | TypeScript |
| Routing | Expo Router (file-based) |
| Database | SQLite via `expo-sqlite` |
| Architecture | New Architecture enabled |

---

## Prerequisites

- [Node.js](https://nodejs.org/) >= 18
- [Expo CLI](https://docs.expo.dev/get-started/installation/) (`npm install -g expo-cli`)
- [EAS CLI](https://docs.expo.dev/eas/cli/) >= 18.3.0 (`npm install -g eas-cli`)
- For Android: Android Studio + Android SDK
- For iOS: macOS with Xcode installed

---

## Getting Started

### Install dependencies

```bash
npm install
```

### Start the dev server

```bash
npm start
```

> Note: React Native DevTools are disabled by default. Remove `REACT_NATIVE_DEVTOOLS_DISABLED=1` from the start script in `package.json` to re-enable them.

---

## Running on Device / Emulator

### Android

```bash
npm run android
```

### iOS

```bash
npm run ios
```

### Web (limited support)

```bash
npm run web
```

---

## Building Locally

### Android APK (debug)

```bash
npx expo run:android --variant debug
```

### Android APK (release)

First, ensure you have a keystore configured in `android/app/build.gradle`, then:

```bash
cd android
./gradlew assembleRelease
```

Output: `android/app/build/outputs/apk/release/app-release.apk`

### Android AAB (release) — for Play Store

```bash
cd android
./gradlew bundleRelease
```

Output: `android/app/build/outputs/bundle/release/app-release.aab`

---

## Building with EAS (Recommended)

[EAS Build](https://docs.expo.dev/build/introduction/) handles cloud builds without needing local Android/iOS toolchains.

### Login to Expo

```bash
eas login
```

### Configure EAS (first time only)

```bash
eas build:configure
```

### Development build (internal testing)

```bash
# Android
eas build --platform android --profile development

# iOS
eas build --platform ios --profile development
```

### Preview build (internal distribution)

```bash
# Android
eas build --platform android --profile preview

# iOS
eas build --platform ios --profile preview
```

### Production build

#### Android AAB — for Google Play Store

```bash
eas build --platform android --profile production
```

#### iOS IPA — for Apple App Store

```bash
eas build --platform ios --profile production
```

#### Both platforms at once

```bash
eas build --platform all --profile production
```

### Submit to stores

```bash
# Google Play Store
eas submit --platform android

# Apple App Store
eas submit --platform ios
```

---

## EAS Build Profiles

Defined in `eas.json`:

| Profile | Distribution | Use Case |
|---|---|---|
| `development` | Internal | Local dev with dev client |
| `preview` | Internal | QA / internal testing |
| `production` | Store | Play Store / App Store release |

---

## Galaxy Watch Companion (Wear OS)

`wear/` holds a native Wear OS app (Kotlin + Compose for Wear OS) that talks to the phone app over Bluetooth using the Wear OS Data Layer `MessageClient`. The phone side of that link is the local Expo module in `modules/wear-bridge`, and you can test it from the **Watch** screen on Android.

- Messages are UTF-8 JSON on the path `/gymbuddy/msg`: `{ "type": "ping" | "pong" | "text" | "set_done", "text"?: string, "ts": number }`.
- A `ping` from either side gets an automatic `pong` back.
- The Data Layer only connects the two apps if **both use the package `com.gymbuddy.app` and are signed with the same key**. By default the watch build signs with `android/app/debug.keystore`, which `npx expo prebuild` creates. To use any other key (EAS, Play), pass `-PgymbuddyKeystore=... -PgymbuddyKeystorePassword=... -PgymbuddyKeyAlias=... -PgymbuddyKeyPassword=...` to the watch build.

### Build

```bash
npx expo prebuild -p android          # generates android/ (and the shared debug keystore)
cd android && ./gradlew assembleRelease && cd ..
cd wear && ./gradlew assembleRelease && cd ..
```

Outputs:
- `android/app/build/outputs/apk/release/app-release.apk` (phone)
- `wear/app/build/outputs/apk/release/app-release.apk` (watch)

The **Android phone + watch build** GitHub Actions workflow builds both APKs and uploads them as the `gymbuddy-apks` artifact.

If you install the phone app from EAS, the watch app has to be signed with the same EAS keystore:

1. Run `eas credentials` → Android → your build profile → **Download credentials** to get the `.jks` file, its password, the key alias and the key password.
2. Add these GitHub repository secrets (Settings → Secrets and variables → Actions):
   - `ANDROID_KEYSTORE_BASE64`: the output of `base64 -w0 your.jks`
   - `ANDROID_KEYSTORE_PASSWORD`
   - `ANDROID_KEY_ALIAS`
   - `ANDROID_KEY_PASSWORD`

When these secrets are set, the workflow signs the watch APK with that key. The phone APK from that workflow is still debug-signed, so keep installing the phone app from EAS.

### Install on a Galaxy Watch

1. On the watch, go to Settings → About watch → Software → tap *Software version* 5 times to turn on Developer options.
2. In Developer options, turn on *ADB debugging* and *Wireless debugging*, then tap *Pair new device*.
3. From a computer on the same Wi-Fi network:
   ```bash
   adb pair <watch-ip>:<pair-port>      # enter the pairing code shown on the watch
   adb connect <watch-ip>:<port>
   adb -s <watch-ip>:<port> install wear/app/build/outputs/apk/release/app-release.apk
   ```
4. Install the phone APK on the phone that the watch is paired with (`adb -s <phone> install ...`).

---

## Linting

```bash
npm run lint
```

---

## Project Structure

```
GymBuddy/
├── app/                        # Expo Router screens (file-based routing)
│   ├── _layout.tsx             # Root layout & navigation
│   ├── index.tsx               # Home screen (gym list)
│   ├── exercises/              # Exercise management screens
│   └── gym/[gymId]/            # Per-gym screens & workouts
├── src/
│   ├── components/             # Shared UI components
│   ├── constants/colors.ts     # Color theme
│   └── db/database.ts          # SQLite schema & queries
├── app.json                    # Expo config
└── eas.json                    # EAS build profiles
```

---

## App Info

| Field | Value |
|---|---|
| App Name | GymBuddy |
| Version | 1.0.0 |
| Android Package | `com.gymbuddy.app` |
| iOS Bundle ID | `com.gymbuddy.app` |
| Minimum iOS | 15.1 |
| Orientation | Portrait |
