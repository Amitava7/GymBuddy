# GymBuddy

A personal gym tracker for one phone. Track workouts across multiple gyms,
manage exercises, and log sets with weight and reps, all stored on the device.

This is a rewrite of the React Native / Expo app as a plain Android app in Java
with **no libraries at all**: no React Native, no Hermes, no AndroidX, no
Kotlin runtime. The Expo APK was over 100 MB; this one is around 130 KB. The
screens, the colours, the buttons and the backup file are the same as before.
The APK is produced by GitHub Actions; there is nothing to install locally.

## Features

- Manage multiple gyms (long-press a gym to delete it)
- Create and organise exercises, filter by muscle group and equipment, search
- Saved workouts per gym: tap one to start it with last time's note and sets
  filled in; a workout started from scratch is saved when you finish it
- Each saved workout keeps its own sets and notes: the same exercise in two
  saved workouts (or twice in one) never shares them
- Changes carry forward, never back: exercises you add or remove, and the sets
  and notes you log, become the saved workout from the moment you finish that
  workout. Finished workouts keep exactly what was done, and a cancelled one
  changes nothing
- Log sets with kg and reps, notes per exercise, tick exercises off
- A running clock, Finish / Cancel Workout, and a summary with duration,
  exercises, sets and volume. Back during a workout asks before leaving it
- History per gym, and per exercise: personal records, line charts of the
  weight and reps with a point for every session, a volume-per-session chart
  and every session
- Export everything to a JSON backup and import it back (merge or replace)
- Dark theme, fully offline, no account, no permissions

## Moving from the Expo app

1. In the old app: Home → the ⇅ button → **Export Data**, and keep the file
   somewhere off the phone's app storage (Drive, mail, Downloads).
2. Uninstall the old app.
3. Install the new APK.
4. In the new app: Home → ⇅ → **Import Data**, pick the file, choose
   **Replace**. You can also just open the `.json` file from a file manager and
   pick GymBuddy.

The backup format is the old one, version 1, unchanged: a file exported by
either app imports into the other, and importing a file with *Replace* and
exporting it again gives back the same bytes. The database is the same schema
in the same place (`files/SQLite/gymbuddy.db`), so if Android ever accepts the
new APK as an update of an old CI build (same signing key) the data simply
stays.

Two small behaviour changes, both about not losing data: sets and notes are
saved as you type them rather than when you add an exercise, tick one off or
finish, and a double tap on a saved workout no longer starts it twice.

## Getting the APK

Every push builds one. Open the **Actions** tab, pick the latest *Build APK*
run and download the `gymbuddy-apk` artifact. Pushing a tag like `v2.0` also
publishes a GitHub release with the APK attached.

**Signing.** Without secrets the APK is signed with `app/debug.keystore`, the
stock React Native debug key that the old Expo CI build used. It is public, but
it is the same on every run, so a new build installs over the previous one and
keeps your data. To use a private key instead, add four repository secrets:
`ANDROID_KEYSTORE_BASE64` (`base64 -w0 your.jks`), `ANDROID_KEY_ALIAS`,
`ANDROID_KEY_PASSWORD` and `ANDROID_KEYSTORE_PASSWORD`. Changing key later
means one more export, uninstall, install, import.

Target device is a Galaxy S24 Ultra, so `minSdk` is 34 and there is no
compatibility code for anything older.

## Why it is small

| what | how |
| --- | --- |
| No dependencies | Framework classes only. SQLite, JSON, dialogs and the share sheet all come with Android. |
| No icon font | The 27 Ionicons the screens use are vector drawables cut from the same font file the old app shipped, so they look identical without its 390 KB. |
| No XML layouts | Screens are built in code from the React Native styles: same sizes, colours and weights. |
| One launcher bitmap | The old icon, scaled once to 432 px WebP. |
| R8 + one locale | Minified and shrunk, English only, no dependency metadata block. |

CI fails the build if the APK ever passes 1 MB.

## What CI does

1. **Unit tests** — Robolectric runs the real SQLite code on the JVM: the
   schema and seed, replace and merge imports, export, the byte-for-byte
   round trip, the error messages, the queries behind the screens, the date
   formats, how saved workouts carry sets and notes forward, and every screen
   opened on top of an imported backup. Screenshots
   of each screen are uploaded with the test report.
2. **Build release APK** — assembles, signs, prints a size breakdown in the job
   summary and fails past the size budget.
3. **Use it on an emulator** — installs the APK on Android 14, checks the fresh
   install, imports `app/src/test/resources/sample-backup.json` through the
   Import Data dialogs, exports it again and compares, runs a saved workout and
   a new one with a picked exercise to the summary, presses back in the middle
   of a workout, opens the exercise screens and the delete prompt, checks the
   screen really is in the app's colours, and fails on any crash.

To run the unit tests against a real export locally:

```bash
GB_BACKUP=/path/to/gymbuddy-backup.json GB_SHOTS=/tmp/shots ./gradlew testDebugUnitTest
```

## Layout

```
app/src/main/java/com/gymbuddy/app/
  Db.java                  the schema, every query, the 68 seeded exercises
  Backup.java              export, import (replace / merge), validation
  Json.java                writes JSON the way JSON.stringify(x, null, 2) does
  BackupProvider.java      hands the exported file to the share sheet
  Ui.java  Flow.java       colours, view factories, wrapping rows
  Fmt.java                 dates, durations and numbers as the old screens showed them
  Screen.java              header, insets, overlays, alerts, list pieces
  MainActivity             Home: gyms
  GymActivity              a gym: new workout, history, saved workouts
  NewWorkoutActivity       name a workout
  WorkoutActivity          the workout in progress
  PickExerciseActivity     add an exercise to it
  SummaryActivity          Workout Complete
  HistoryActivity          finished workouts at a gym
  ExercisesActivity        all exercises
  ExerciseActivity         records and trends for one
  LineChart                the weight and reps lines on it
  ExerciseFormActivity     new / edit exercise
  BackupActivity           Backup & Restore
tools/
  build_icons.py           cuts the icons out of the Ionicons font, scales the launcher icon
  make_sample_backup.py    writes the sample backup the tests import
  smoke_test.sh            drives the APK on an emulator
  screen_colors.py         tells the smoke test which theme colours are on screen
```

## Building locally

Needs JDK 17 (21 for the unit tests) and an Android SDK with platform 35:

```bash
./gradlew assembleRelease     # add -PgbStoreFile=... to sign with your own key
```

## App info

| Field | Value |
| --- | --- |
| App name | GymBuddy |
| Version | 2.0 |
| Android package | `com.gymbuddy.app` |
| Minimum Android | 14 (API 34) |
| Orientation | Portrait |
