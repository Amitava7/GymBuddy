#!/usr/bin/env bash
# Installs the release APK on a running emulator and uses it the way a person
# would: fresh install, import a backup through the Import Data dialogs, export
# it again and check nothing changed, then a saved workout and a new one end to
# end, back, an exercise's chart, reordering and typing with the keyboard up in
# the middle of a workout, the exercise screens and their charts, and the
# delete prompts. Fails on any crash or on a screen that does not show
# what it should. Buttons are found by reading the view hierarchy, so this does
# not depend on the screen size. Screenshots land in shots/ and the workflow
# uploads them.
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

# Closes the keyboard if it is up; it covers buttons and eats the first back.
no_keyboard() {
    if adb shell dumpsys input_method | grep -q "mInputShown=true"; then
        back
    fi
}

# x y of the middle of the lowest text field that is clear of the Finish
# Workout bar, so tapping it cannot hit the bar instead.
lowest_input() {
    ui | python3 -c '
import re, sys
nodes = re.findall(r"<node [^>]*>", sys.stdin.read())
def box(n):
    return [int(v) for v in re.search(r"bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"", n).groups()]
bar = min([box(n)[1] for n in nodes if "text=\"Finish Workout\"" in n] or [10 ** 9])
fields = [box(n) for n in nodes if "android.widget.EditText" in n]
fields = [b for b in fields if b[3] < bar - 80]
if fields:
    b = max(fields, key=lambda b: b[3])
    print((b[0] + b[2]) // 2, (b[1] + b[3]) // 2)
'
}

# Succeeds when the focused text field sits wholly inside the scrolling list,
# which ends where the keyboard begins.
focused_in_view() {
    ui | python3 -c '
import re, sys
nodes = re.findall(r"<node [^>]*>", sys.stdin.read())
def box(n):
    return [int(v) for v in re.search(r"bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"", n).groups()]
field = [box(n) for n in nodes if "android.widget.EditText" in n and "focused=\"true\"" in n]
lists = [box(n) for n in nodes if "android.widget.ScrollView" in n]
if not field or not lists:
    sys.exit("no focused field or no list on screen")
f, s = field[0], max(lists, key=lambda b: (b[3] - b[1]) * (b[2] - b[0]))
print("field", f, "list", s)
sys.exit(0 if s[1] <= f[1] and f[3] <= s[3] else 1)
'
}

# A slow half-screen drag, so the page moves about that far and no further.
swipe() {  # swipe up|down
    local w h
    read -r w h < <(adb shell wm size | grep -o '[0-9]*x[0-9]*' | tail -1 | tr x ' ')
    if [ "$1" = up ]; then
        adb shell input swipe $((w / 2)) $((h * 3 / 4)) $((w / 2)) $((h / 4)) 800
    else
        adb shell input swipe $((w / 2)) $((h / 4)) $((w / 2)) $((h * 3 / 4)) 800
    fi
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
# each exercise opens its chart over the workout; back closes it
tap_node 'content-desc="Progress for Bench Press"'
crashed workout-chart
expect "WEIGHT PER SESSION" workout-chart
expect "TIME AT EACH WEIGHT" workout-chart
shot 12c-workout-chart
back
if ui | grep -qF "TIME AT EACH WEIGHT"; then
    echo "::error::back did not close the chart"
    exit 1
fi
expect "Finish Workout" chart-closed
# Reorder moves Bench Press down below Pec Deck
tap_node 'content-desc="Reorder exercises"'
expect "Reorder Exercises" reorder
tap_node 'content-desc="Move down: Bench Press"'
crashed reorder
shot 12d-reorder
tap_node 'text="Done"'
first=$(ui | tr '<' '\n' | grep -o -e 'text="Bench Press"' -e 'text="Pec Deck"' | head -1)
if [ "$first" != 'text="Pec Deck"' ]; then
    echo "::error::Pec Deck should come first after moving Bench Press down, saw $first first"
    exit 1
fi
# typing in the lowest field: the Finish bar steps aside and the field stays above the keyboard
xy=$(lowest_input)
[ -n "$xy" ] || { echo "::error::no text field clear of the Finish bar"; exit 1; }
adb shell input tap $xy
sleep 3
shot 12e-typing
if ! adb shell dumpsys input_method | grep -q "mInputShown=true"; then
    echo "::error::the keyboard did not come up for the field at $xy"
    exit 1
fi
if ui | grep -qF 'text="Finish Workout"'; then
    echo "::error::the Finish Workout bar stayed up over the field being typed in"
    exit 1
fi
focused_in_view || { echo "::error::the field being typed in is not in view above the keyboard"; exit 1; }
no_keyboard
expect "Finish Workout" keyboard-closed
# back in the middle of a workout asks first, and Keep Going stays on it
no_keyboard
back
expect "Leave Workout?" leave
expect "This workout is still in progress." leave
shot 12b-leave-dialog
tap_node 'text="Keep Going"'
crashed keep-going
if ui | grep -qF "Leave Workout?"; then
    echo "::error::the Leave Workout? dialog is still up after Keep Going"
    exit 1
fi
expect "Finish Workout" kept-going
expect "Bench Press" kept-going
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
expect "WEIGHT PER SESSION" exercise
expect "TIME AT EACH WEIGHT" exercise
shot 18-exercise
# the reps line and the volume bars are further down
swipe up
expect "REPS PER SESSION" exercise-trends
expect "VOLUME PER SESSION" exercise-trends
shot 18b-exercise-trends
crashed exercise-trends
swipe down
swipe down
tap_node 'content-desc="Edit exercise"'
expect "Edit Exercise" edit
# the name field opens with the keyboard up, and the keyboard covers the button
no_keyboard
expect "Update Exercise" edit
shot 19-edit-exercise
crashed exercises

echo "fresh install, import, export, both kinds of workout and every screen: no crashes"
