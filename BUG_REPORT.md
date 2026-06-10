# GymBuddy — Bug Audit Report

Audit of user-facing bugs found by reviewing the full codebase (all screens under `app/`, hooks, components, and the database layer). Bugs are sorted by severity. Line references point to the current code on this branch.

---

## Critical

### 1. In-progress workouts become permanently unreachable, and typed set data is lost, if the user leaves the active workout screen

**Where:** `app/gym/[gymId]/workout/[workoutId].tsx:33`, `src/hooks/useActiveWorkout.ts:32-41`, `app/gym/[gymId]/index.tsx`

The active workout screen hides the header back button (`headerBackVisible: false`), but nothing blocks the Android hardware/gesture back or the iOS swipe-back gesture. Once the user navigates away:

- There is **no way to resume** the workout. The gym screen only lists templates, and the history screen filters on `finished_at IS NOT NULL` (`src/db/database.ts:194`), so an unfinished workout appears nowhere in the UI. The row and all its exercises/sets sit in the database forever, invisible.
- Any kg/reps/notes typed since the last "flush" are **silently lost**. Set values live only in React state and are only persisted to SQLite when the user taps "Add Exercise", toggles an exercise complete, or finishes the workout (`flushUnsavedData` in `useActiveWorkout.ts:32`). Leaving the screen (back gesture, app killed by the OS, phone dies mid-session) discards them.

**Steps to reproduce:**
1. Create a gym → "New Workout" → name it → Start.
2. Add an exercise and type kg/reps into a set (do not toggle the exercise complete).
3. Press the Android back button (or swipe back on iOS).
4. Observe: you are back at the gym screen with no banner/entry to resume; the typed values are gone; the workout never appears in History.

**Fix:**
- On the gym screen (and/or home screen), query for workouts with `finished_at IS NULL` and show a "Resume workout in progress" card that navigates back to `/gym/[gymId]/workout/[workoutId]`. Optionally offer to discard it.
- Persist set/note edits as they happen (e.g., debounce `db.updateWorkoutSet` inside `handleUpdateSet`) instead of relying on flush points.
- Optionally intercept back navigation on the active workout screen (`usePreventRemove` from `@react-navigation/native`) with a "Workout in progress" confirmation.

---

### 2. "Cancel Workout" inside the *Finish* confirmation deletes the entire workout with one tap and no confirmation

**Where:** `src/hooks/useActiveWorkout.ts:127-137`

Tapping "Finish Workout" opens an alert titled "Finish Workout — Complete this workout?" with three buttons: *Keep Going*, *Cancel Workout*, *Finish*. A user who taps **Cancel Workout** intending to "cancel this dialog" (a very natural reading) instantly and permanently deletes the workout and every set they logged — `db.deleteWorkout()` cascades to `workout_exercises` and `workout_sets`. There is no second confirmation and no undo.

**Steps to reproduce:**
1. Start a workout, log several exercises and sets.
2. Tap "Finish Workout".
3. Tap "Cancel Workout" (expecting to dismiss the dialog).
4. Observe: returned to the gym screen; the whole session is permanently gone.

**Fix:** Remove the destructive option from the finish dialog. Put "Discard workout" behind its own explicit entry point (e.g., a trash icon in the header) with its own clearly-worded confirmation ("Discard this workout? All logged sets will be permanently deleted."). At minimum, rename the button to "Discard Workout" and add a second confirmation.

---

## High

### 3. Deleting an exercise from the library silently rewrites workout history

**Where:** `app/exercises/index.tsx:38-50`, `src/db/database.ts:79-81`, schema FKs in `src/db/schema.ts:46` (`onDelete: 'cascade'`)

`workout_exercises.exercise_id` cascades on delete, and `PRAGMA foreign_keys = ON` is set. Long-pressing an exercise in the library and confirming the bare `Delete "Bench Press"?` alert deletes that exercise from **every past workout**, including all logged sets. Historical summaries, totals, and personal records silently change; there is no warning that the exercise has history.

**Steps to reproduce:**
1. Complete a workout containing "Bench Press" with a few sets. View it in History → Summary (sets are visible).
2. Go to Home → Exercises, long-press "Bench Press", confirm Delete.
3. Re-open the same workout summary: the exercise and its sets have vanished; set/volume stats are reduced.

**Fix:** Before deleting, count references in `workout_exercises`. If the exercise has history, either warn explicitly ("Used in 12 workouts — deleting will remove it from your history") or use a soft-delete (`archived` flag) so the library hides it but history is preserved.

---

### 4. Decimal-comma input (European locales) and other non-numeric input becomes `NaN` and wipes the set value

**Where:** `src/hooks/useActiveWorkout.ts:65-75` (`Number(value)`), `src/components/workout/SetRow.tsx:33` (`keyboardType="decimal-pad"`)

On devices set to a locale that uses a decimal comma (German, French, etc.), the decimal pad produces "12,5". `Number('12,5')` is `NaN`, which is stored in component state and then written to SQLite on the next flush. The user sees the value they typed, but the saved value is garbage/null — weight data silently lost, and `NaN` propagates into volume/record calculations for the session.

**Steps to reproduce:**
1. Set the device language/region to German.
2. Start a workout, add an exercise, type `12,5` into the KG field.
3. Toggle the exercise complete (triggers the save), navigate away and back.
4. Observe: the kg value is empty/invalid; summary volume excludes it.

**Fix:** In `handleUpdateSet`, normalize the input (`value.replace(',', '.')`) and validate with `Number.isFinite(numVal)` before accepting; keep the previous value (or null) on invalid input. Same guard in `flushUnsavedData`/`updateWorkoutSet`.

---

## Medium

### 5. Removing an exercise from today's session permanently edits the saved template

**Where:** `src/hooks/useActiveWorkout.ts:87-103` (`removeTemplateExercise`)

If a workout was started from a saved template, deleting an exercise from the *current session* also deletes it from the template. The confirmation text only says `Remove "X" from this workout?` — it doesn't mention the template. A user skipping squats today because the rack is busy loses squats from "Leg Day" forever; the next time they start from the template the exercise is missing with no explanation.

**Steps to reproduce:**
1. Create a template-based workout (finish an ad-hoc workout once, which saves it as a template), then start it again from "Saved Workouts".
2. Delete one exercise from the active session and finish.
3. Start the template again: the deleted exercise is gone from the template.

**Fix:** Don't mutate the template on session-level removal. If template syncing is desired, ask explicitly ("Also remove from the 'Leg Day' template?").

---

### 6. Finishing an ad-hoc workout always creates a new template — duplicates accumulate

**Where:** `src/hooks/useActiveWorkout.ts:144-149`

Every workout started via "New Workout" creates a brand-new template named after the workout on finish, unconditionally. Users who habitually start "Push Day" manually end up with many identical "Push Day" entries under "Saved Workouts". An empty workout (zero exercises) also creates an empty, useless template.

**Steps to reproduce:**
1. New Workout → "Push Day" → add an exercise → Finish. Repeat twice.
2. Gym screen now lists two (or more) "Push Day" templates.

**Fix:** Ask on finish ("Save as template?"), or reuse an existing template with the same name for that gym, and skip template creation when no exercises were logged.

---

### 7. Double-taps create duplicate workouts, duplicate exercises, and can pop the wrong screen

**Where:** `app/gym/[gymId]/workout/new.tsx:12-16`, `app/gym/[gymId]/index.tsx:45-67`, `app/exercises/pick.tsx:40-60`

None of the async tap handlers guard against re-entry:

- Double-tapping "Start Workout" or a template card creates **two** workout rows; only one is opened, the other becomes an invisible orphan (compounds bug #1). Starting from a template performs many sequential awaits, so the window for a second tap is large.
- Double-tapping an exercise in the picker inserts it twice and calls `router.back()` twice, which can pop the active workout screen itself.

**Steps to reproduce:** Rapidly tap "Start Workout" twice (easier on a slower device), then check the database / orphan count; or rapidly tap an exercise in the picker and observe the duplicate entry and unexpected navigation.

**Fix:** Add an in-flight guard (`useRef` boolean or disabled state) to each async handler; disable the button while the operation runs.

---

### 8. Pre-fill of previous weights never falls back outside the template

**Where:** `src/db/database.ts:292-332` (`getLastWorkoutDataForExercise`)

When a workout has a `template_id`, the lookup only searches past workouts of that same template. The fallback query is gated on `!lastExercise && !templateId`, so it can never run when a template id was passed. Adding an exercise to a template workout that you've done many times in ad-hoc workouts pre-fills nothing (a single empty set) instead of your last numbers.

**Steps to reproduce:**
1. Do an ad-hoc workout with "Bench Press" at 80 kg × 8 and finish (this creates template A).
2. Create a separate template B and start a workout from it; add "Bench Press" via the picker.
3. Observe: no pre-filled sets, even though bench history exists.

**Fix:** Change the fallback condition to `if (!lastExercise)` so the general lookup runs whenever the template-scoped lookup finds nothing.

---

## Low

### 9. Personal-records card hides legitimate zero values

**Where:** `app/exercises/[exerciseId].tsx:58` — `records.max_kg || records.max_reps` treats `0` as "no records", so an exercise logged only with bodyweight (0 kg) and 0-rep placeholders shows no records section even though sets exist. **Fix:** check `!= null` instead of truthiness.

### 10. `%` and `_` act as wildcards in exercise search

**Where:** `src/db/database.ts:48` — the search string is interpolated into `LIKE '%…%'` unescaped. Typing `_` or `%` matches everything. Harmless (parameterized, no SQL injection) but confusing. **Fix:** escape `%`/`_` with an `ESCAPE` clause.

### 11. Deleting all exercises re-seeds the 70 defaults on next launch

**Where:** `src/db/seed.ts:82-89` — seeding is keyed on `COUNT(*) == 0`, not a "seeded" flag. A user who intentionally clears the library gets it all back after restarting the app. **Fix:** record a `seeded` marker (e.g., a meta table or async storage flag).

### 12. Timestamp parsing relies on non-ISO date strings

**Where:** `src/hooks/useActiveWorkout.ts:18`, `app/.../history.tsx:16`, `app/.../summary/[workoutId].tsx:61` — SQLite's `datetime('now')` produces `"YYYY-MM-DD HH:MM:SS"`, and the code parses `new Date(str + 'Z')`. The space-separated format is not ISO 8601; engines are not required to parse it (current Hermes does, but it's implementation-defined). If parsing ever fails the workout timer renders `NaN:NaN` and dates show "Invalid Date". **Fix:** normalize before parsing: `new Date(str.replace(' ', 'T') + 'Z')`.

### 13. Dead/misleading code and missing error handling (general)

- `app/gym/[gymId]/workout/summary/[workoutId].tsx:95`: the `ex.is_completed ? '✓ ' : ''` ternary is dead — incomplete exercises are filtered out at line 32, so the checkmark is always shown.
- No database call anywhere is wrapped in try/catch; a single SQLite error surfaces as an unhandled promise rejection with no user feedback. Adding a small error boundary / toast around the db layer would prevent silent failures.

---

## Summary table

| # | Severity | Bug | Area |
|---|----------|-----|------|
| 1 | Critical | In-progress workouts orphaned; unsaved set data lost on back navigation | Active workout |
| 2 | Critical | "Cancel Workout" in finish dialog deletes session with one tap | Active workout |
| 3 | High | Deleting a library exercise rewrites workout history | Exercises |
| 4 | High | Decimal-comma input saved as NaN (data loss in non-US locales) | Set entry |
| 5 | Medium | Removing an exercise from a session mutates the saved template | Templates |
| 6 | Medium | Duplicate templates created on every ad-hoc finish | Templates |
| 7 | Medium | Double-tap creates duplicate workouts/exercises | Navigation |
| 8 | Medium | Previous-weights pre-fill never falls back outside the template | Pre-fill |
| 9 | Low | Zero values hide the personal-records card | Exercise detail |
| 10 | Low | `%`/`_` act as wildcards in search | Search |
| 11 | Low | Library re-seeds after the user empties it | Seeding |
| 12 | Low | Non-ISO date parsing (engine-dependent) | Dates |
| 13 | Low | Dead code & missing error handling | General |
