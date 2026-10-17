#!/usr/bin/env bash
# Installs the release APK on a running emulator and uses it the way a person
# would: fresh install, import a backup through the Import Data dialogs, export
# it again and check nothing changed, then a saved workout and a new one end to
# end, the exercise screens, and the delete prompts. Fails on any crash or on a
# screen that does not show what it should. Buttons are found by reading the
# view hierarchy, so this does not depend on the screen size. Screenshots land
# in shots/ and the workflow uploads them.
set -euo pipefail

APK=$(ls apk/*.apk | head -1)
PKG=com.gymbuddy.app
SAMPLE=app/src/test/resources/sample-backup.json
SHOTS=shots
mkdir -p "$SHOTS"

adb install -r "$APK"
adb root
adb wait-for-device
sleep 5
adb shell cmd uimode night yes || true

shot() { adb exec-out screencap -p > "$SHOTS/$1.png"; }

# Leaves the share of each theme colour in $SHOTS/$1.txt and prints it.
colors() {
    adb exec-out screencap > "$SHOTS/$1.raw"
    python3 tools/screen_colors.py "$SHOTS/$1.raw" | tee "$SHOTS/$1.txt"
    rm -f "$SHOTS/$1.raw"
}

pct() {  # pct <name> <COLOUR>
    awk -v k="PCT_$2" '$1 == k {print int($2)}' "$SHOTS/$1.txt" | head -1
}

ui() {
    for _ in 1 2 3; do
        if adb shell uiautomator dump /sdcard/ui.xml 2>&1 | grep -q "dumped to"; then
            adb shell cat /sdcard/ui.xml 2>/dev/null
            return
        fi
        sleep 1
    done
    echo "::warning::uiautomator could not dump the screen" >&2
}

center() {  # center <attribute match>: x y of the first node that matches
    ui | tr '<' '\n' | grep -iF "$1" | head -1 \
        | grep -o 'bounds="\[[0-9]*,[0-9]*\]\[[0-9]*,[0-9]*\]"' | head -1 \
        | tr -c '0-9' ' ' | awk '{print int(($1+$3)/2), int(($2+$4)/2)}'
}

# Taps the middle of the first node matching an attribute, e.g. 'text="History"'.
# Case-insensitive, because dialog buttons are drawn in capitals.
tap_node() {
    local xy
    xy=$(center "$1")
    if [ -z "$xy" ]; then
        echo "::error::nothing on screen matches $1"
        ui | tr '<' '\n' | grep -o 'text="[^"]*"' | grep -v 'text=""' || true
        shot "missing-tap"
        exit 1
    fi
    echo "tapping $1 at $xy"
    adb shell input tap $xy
    sleep 2
}

long_press() {
    local xy
    xy=$(center "$1")
    [ -n "$xy" ] || { echo "::error::nothing on screen matches $1"; exit 1; }
    echo "long-pressing $1 at $xy"
    adb shell input swipe $xy $xy 1200
    sleep 2
}

expect() {
    if ! ui | grep -qF "$1"; then
        echo "::error::expected to see \"$1\""
        shot "missing-${2:-text}"
        ui | tr '<' '\n' | grep -o 'text="[^"]*"' | grep -v 'text=""' || true
        exit 1
    fi
    echo "sees \"$1\""
}

crashed() {
    if adb logcat -d | grep -q "FATAL EXCEPTION"; then
        echo "::error::$1 crashed"
        adb logcat -d | grep -A 40 "FATAL EXCEPTION"
        shot "$1-crash"
        exit 1
    fi
}

# Starts a screen on a task of its own. Without clearing the task, starting
# MainActivity again only brings back whatever was on top of it.
open_screen() {  # open_screen <Activity> [am start extras...]
    local name="$1"
    shift
    adb logcat -c
    adb shell am start -W --activity-clear-task -n "$PKG/.$name" "$@"
    sleep 5
    crashed "$name"
}

back() {
    adb shell input keyevent KEYCODE_BACK
    sleep 2
}

# ---- fresh install ---------------------------------------------------------
open_screen MainActivity
expect "TRACK. LIFT. REPEAT." home
expect "Your Gyms" home
expect "No gyms yet" home
shot 01-fresh-home
colors 01-fresh-home
if [ "$(pct 01-fresh-home BACKGROUND)" -lt 50 ] || [ "$(pct 01-fresh-home PRIMARY)" -lt 1 ]; then
    echo "::error::the home screen is not in the GymBuddy colours"
    exit 1
fi
open_screen ExercisesActivity
expect "Ab Rollout" seeded
expect "Bulgarian Split Squat" seeded
shot 02-seeded-exercises

# ---- import a backup -------------------------------------------------------
# Opening a .json with GymBuddy goes through the same dialogs as Import Data.
APP_UID=$(adb shell stat -c %u "/data/data/$PKG" | tr -d '\r')
adb push "$SAMPLE" /data/local/tmp/sample.json
adb shell "mkdir -p /data/data/$PKG/cache && cp /data/local/tmp/sample.json /data/data/$PKG/cache/sample.json \
    && chown -R $APP_UID:$APP_UID /data/data/$PKG/cache && chmod 644 /data/data/$PKG/cache/sample.json \
    && restorecon -R /data/data/$PKG/cache"
adb logcat -c
adb shell am start -W -a android.intent.action.VIEW -t application/json \
    -d "file:///data/data/$PKG/cache/sample.json" -n "$PKG/.BackupActivity"
sleep 5
crashed import-open
expect "Import Data" import
expect "3 gyms, 73 exercises, 5 templates, 36 workouts, 420 sets" import
shot 03-import-dialog
tap_node 'text="Replace"'
expect "Replace all data?" replace
tap_node 'text="Replace"'
sleep 2
expect "Import complete" imported
expect "Added 3 gyms, 73 exercises, 5 templates, 36 workouts, 420 sets." imported
shot 04-imported
tap_node 'text="OK"'
crashed import

# ---- export it again -------------------------------------------------------
expect "Export Data" backup
shot 05-backup
tap_node 'text="Export Data"'
sleep 2
shot 06-share-sheet
crashed export
EXPORTED=$(adb shell ls "/data/data/$PKG/cache/exports/" | tr -d '\r' | head -1)
echo "exported $EXPORTED"
adb pull "/data/data/$PKG/cache/exports/$EXPORTED" "$SHOTS/$EXPORTED"
python3 - "$SAMPLE" "$SHOTS/$EXPORTED" <<'EOF'
import json, sys
a, b = (json.load(open(p, encoding="utf-8")) for p in sys.argv[1:3])
assert b["app"] == "gymbuddy" and b["version"] == 1, "wrong header"
for t in a["data"]:
    assert a["data"][t] == b["data"][t], "table %s changed on the way through" % t
print("export matches the imported backup:", {t: len(v) for t, v in b["data"].items()})
EOF
back

# ---- browse ----------------------------------------------------------------
open_screen MainActivity
expect "Home Gym" home
expect "Hotel gym" home
shot 07-home
long_press 'text="Park"'
expect "Delete Gym" delete-gym
expect "This will remove all related workouts." delete-gym
shot 08-delete-gym
tap_node 'text="Cancel"'
expect "Park" kept-gym

tap_node 'text="Home Gym"'
expect "Saved Workouts" gym
expect "Push day" gym
expect "Tap to start" gym
shot 09-gym
tap_node 'text="History"'
expect "exercises" history
shot 10-history
tap_node 'text="Push day"'
expect "Workout Complete" past-summary
expect "DURATION" past-summary
shot 11-past-summary
back
back

# ---- a saved workout, start to finish -------------------------------------
tap_node 'text="Push day"'
crashed start-template
expect "Finish Workout" workout
expect "Bench Press" workout
expect "Add Set" workout
shot 12-workout
colors 12-workout
if [ "$(pct 12-workout PRIMARY)" -lt 4 ]; then
    echo "::error::no Finish Workout bar in the primary colour"
    exit 1
fi
tap_node 'text="Add Set"'
crashed add-set
tap_node 'text="Finish Workout"'
expect "Complete this workout?" finish
shot 13-finish-dialog
tap_node 'text="Finish"'
crashed finish
expect "Workout Complete" summary
expect "Back to Gym" summary
shot 14-summary
tap_node 'text="Back to Gym"'
expect "Saved Workouts" back-to-gym

# ---- a new workout with a picked exercise ----------------------------------
open_screen NewWorkoutActivity --el gymId 1
expect "WORKOUT NAME" new-workout
adb shell input text "Smoke%stest"
sleep 1
shot 15-new-workout
tap_node 'text="Start Workout"'
crashed start-new
expect "Smoke test" new-running
tap_node 'text="Add Exercise"'
expect "Pick Exercise" pick
adb shell input text "Bench"
sleep 2
shot 16-pick
tap_node 'text="Bench Press"'
crashed pick
expect "Bench Press" picked
tap_node 'text="Finish Workout"'
tap_node 'text="Finish"'
expect "Workout Complete" new-summary
tap_node 'text="Back to Gym"'
# a workout started from scratch is kept as a saved workout
expect "Smoke test" saved-new

# ---- exercises -------------------------------------------------------------
open_screen ExercisesActivity
tap_node 'text="Legs"'
expect "Bulgarian Split Squat" filter
shot 17-exercises-legs
tap_node 'text="Legs"'
expect "Bench Press" unfiltered
tap_node 'text="Bench Press"'
expect "Personal Records" exercise
expect "VOLUME PER SESSION" exercise
shot 18-exercise
tap_node 'content-desc="Edit exercise"'
expect "Edit Exercise" edit
expect "Update Exercise" edit
shot 19-edit-exercise
crashed exercises

echo "fresh install, import, export, both kinds of workout and every screen: no crashes"
