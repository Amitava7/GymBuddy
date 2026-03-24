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
