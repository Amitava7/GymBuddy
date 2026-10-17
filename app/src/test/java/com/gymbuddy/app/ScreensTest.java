package com.gymbuddy.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.shadows.ShadowAlertDialog;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Opens every screen on top of an imported backup, walks the main flows and
 * fails on any crash. With GB_SHOTS=dir it also saves each screen as a PNG.
 */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xhdpi")
public class ScreensTest {

    private Db db;

    @Before
    public void setUp() throws Exception {
        Db.reset();
        db = Db.get(RuntimeEnvironment.getApplication());
        Backup.importBackup(db.getWritableDatabase(), Backup.parse(BackupTest.backupText()), true);
    }

    @After
    public void tearDown() {
        Db.reset();
    }

    // --------------------------------------------------------------- helpers

    private static <T extends Activity> T start(Class<T> c, Object... extras) {
        Intent i = new Intent(RuntimeEnvironment.getApplication(), c);
        for (int k = 0; k + 1 < extras.length; k += 2) i.putExtra((String) extras[k], (Long) extras[k + 1]);
        T a = Robolectric.buildActivity(c, i).setup().get();
        idle();
        return a;
    }

    private static <T extends Activity> T startIntent(Class<T> c, Intent i) {
        T a = Robolectric.buildActivity(c, i).setup().get();
        idle();
        return a;
    }

    private static void idle() {
        shadowOf(Looper.getMainLooper()).idle();
    }

    private static void shot(View root, String name) throws Exception {
        String dir = System.getProperty("gb.shots", "");
        if (dir.isEmpty()) return;
        idle();
        Bitmap b = Bitmap.createBitmap(root.getWidth(), root.getHeight(), Bitmap.Config.ARGB_8888);
        root.draw(new Canvas(b));
        new File(dir).mkdirs();
        FileOutputStream out = new FileOutputStream(new File(dir, name + ".png"));
        b.compress(Bitmap.CompressFormat.PNG, 100, out);
        out.close();
    }

    private static void shot(Activity a, String name) throws Exception {
        shot(a.getWindow().getDecorView(), name);
    }

    private static void texts(View v, List<String> out) {
        if (v.getVisibility() != View.VISIBLE) return;
        if (v instanceof TextView) out.add(((TextView) v).getText().toString());
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) texts(g.getChildAt(i), out);
        }
    }

    private static List<String> texts(Activity a) {
        List<String> out = new ArrayList<String>();
        texts(a.getWindow().getDecorView(), out);
        return out;
    }

    private static void assertShows(Activity a, String... wanted) {
        List<String> t = texts(a);
        for (String w : wanted) assertTrue("missing \"" + w + "\" in " + t, t.contains(w));
    }

    private static View find(View v, String text) {
        if (v instanceof TextView && text.equals(((TextView) v).getText().toString())
                && v.getVisibility() == View.VISIBLE) return v;
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                View f = find(g.getChildAt(i), text);
                if (f != null) return f;
            }
        }
        return null;
    }

    /** Taps the nearest clickable view around the given text. */
    private static void tap(Activity a, String text) {
        View v = find(a.getWindow().getDecorView(), text);
        assertNotNull("nothing says \"" + text + "\"", v);
        while (!v.isClickable()) v = (View) v.getParent();
        v.performClick();
        idle();
    }

    private static AlertDialog dialog(String title) {
        AlertDialog d = (AlertDialog) ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull("no dialog", d);
        assertEquals(title, shadowOf(d).getTitle().toString());
        return d;
    }

    private static AlertDialog waitForDialog(String title) throws Exception {
        for (int i = 0; i < 200; i++) {
            idle();
            AlertDialog d = (AlertDialog) ShadowAlertDialog.getLatestAlertDialog();
            if (d != null && d.isShowing() && title.equals(shadowOf(d).getTitle().toString())) return d;
            Thread.sleep(25);
        }
        return dialog(title);
    }

    private static Intent next(Activity a) {
        Intent i = shadowOf(a).getNextStartedActivity();
        assertNotNull("nothing was started", i);
        return i;
    }

    private Db.Gym firstGym() {
        return db.getGyms().get(0);
    }

    // ----------------------------------------------------------------- tests

    @Test
    public void home() throws Exception {
        MainActivity a = start(MainActivity.class);
        assertShows(a, "GYM", "BUDDY", "TRACK. LIFT. REPEAT.", "Your Gyms", "Exercises", firstGym().name);
        shot(a, "01-home");
        // the + button opens Add Gym, with the cursor in the name field
        findByDescription(a.getWindow().getDecorView(), "Add").performClick();
        idle();
        assertShows(a, "Add Gym", "Cancel", "Add");
        shot(a, "02-add-gym");
        EditText name = (EditText) a.getWindow().getDecorView().findFocus();
        assertNotNull(name);
        name.setText("Brand new gym");
        tap(a, "Add");
        assertShows(a, "Brand new gym");
    }

    private static View findByDescription(View v, String d) {
        if (d.contentEquals(v.getContentDescription() == null ? "" : v.getContentDescription())) return v;
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                View f = findByDescription(g.getChildAt(i), d);
                if (f != null) return f;
            }
        }
        return null;
    }

    @Test
    public void savedWorkoutToSummary() throws Exception {
        Db.Gym g = firstGym();
        GymActivity gym = start(GymActivity.class, "gymId", g.id);
        Db.Template t = db.getWorkoutTemplates(g.id).get(0);
        assertShows(gym, g.name, "New Workout", "History", "Saved Workouts", t.name, "Tap to start");
        shot(gym, "03-gym");

        tap(gym, t.name);
        Intent toWorkout = next(gym);
        WorkoutActivity w = startIntent(WorkoutActivity.class, toWorkout);
        assertShows(w, t.name, "Finish Workout", "Add Exercise", "Add Set", "SET", "KG", "REPS");
        shot(w, "04-workout");

        // tick the first exercise off, then finish
        long workoutId = toWorkout.getLongExtra("workoutId", 0);
        Db.WorkoutExercise first = db.getWorkoutExercisesWithSets(workoutId).get(0);
        assertTrue("sets are copied from last time", first.sets.size() >= 1);
        db.toggleWorkoutExercise(first.id, true);
        w = startIntent(WorkoutActivity.class, toWorkout);
        shot(w, "05-workout-ticked");
        tap(w, "Finish Workout");
        AlertDialog d = dialog("Finish Workout");
        assertEquals("Keep Going", d.getButton(AlertDialog.BUTTON_NEUTRAL).getText().toString());
        assertEquals("Cancel Workout", d.getButton(AlertDialog.BUTTON_NEGATIVE).getText().toString());
        assertEquals("Finish", d.getButton(AlertDialog.BUTTON_POSITIVE).getText().toString());
        shot(d.getWindow().getDecorView(), "06-finish-dialog");
        d.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        idle();
        assertNotNull(db.getWorkout(workoutId).finishedAt);

        SummaryActivity s = startIntent(SummaryActivity.class, next(w));
        assertShows(s, "Workout Complete", "Back to Gym", "DURATION", "EXERCISES", "SETS", "VOLUME");
        shot(s, "07-summary");
    }

    @Test
    public void newWorkoutAndPickExercise() throws Exception {
        Db.Gym g = firstGym();
        NewWorkoutActivity n = start(NewWorkoutActivity.class, "gymId", g.id);
        assertShows(n, "New Workout", "WORKOUT NAME", "Start Workout");
        shot(n, "08-new-workout");
        EditText name = (EditText) n.getWindow().getDecorView().findFocus();
        name.setText("Arms day");
        tap(n, "Start Workout");
        Intent toWorkout = next(n);
        long workoutId = toWorkout.getLongExtra("workoutId", 0);

        PickExerciseActivity p = start(PickExerciseActivity.class, "workoutId", workoutId, "gymId", g.id);
        assertShows(p, "Pick Exercise", "Create New Exercise", "Chest", "Kettlebell");
        shot(p, "09-pick");
        ListView list = (ListView) findList(p.getWindow().getDecorView());
        assertTrue(list.getAdapter().getCount() > 60);
        tap(p, "Bench Press");
        assertEquals(1, db.getWorkoutExercises(workoutId).size());

        WorkoutActivity w = startIntent(WorkoutActivity.class, toWorkout);
        assertShows(w, "Arms day", "Bench Press");
    }

    private static View findList(View v) {
        if (v instanceof ListView && v.getVisibility() == View.VISIBLE) return v;
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                View f = findList(g.getChildAt(i));
                if (f != null) return f;
            }
        }
        return null;
    }

    @Test
    public void historyAndPastSummary() throws Exception {
        Db.Gym g = firstGym();
        HistoryActivity h = start(HistoryActivity.class, "gymId", g.id);
        assertShows(h, "History");
        ListView list = (ListView) findList(h.getWindow().getDecorView());
        assertEquals(db.getWorkoutHistory(g.id).size(), list.getAdapter().getCount());
        shot(h, "10-history");
        long id = db.getWorkoutHistory(g.id).get(0).id;
        SummaryActivity s = start(SummaryActivity.class, "gymId", g.id, "workoutId", id);
        shot(s, "11-past-summary");
    }

    @Test
    public void exercises() throws Exception {
        ExercisesActivity e = start(ExercisesActivity.class);
        assertShows(e, "Exercises", "Chest", "Back", "Bodyweight");
        shot(e, "12-exercises");
        tap(e, "Legs");
        shot(e, "13-exercises-legs");

        // the most logged exercise has records and a chart
        long best = 0;
        int most = -1;
        for (Db.Exercise x : db.getExercises(null, null)) {
            int n = db.getExerciseHistory(x.id).size();
            if (n > most) {
                most = n;
                best = x.id;
            }
        }
        ExerciseActivity d = start(ExerciseActivity.class, "exerciseId", best);
        assertShows(d, "Personal Records", "Max Weight", "History & Trends", "VOLUME PER SESSION");
        shot(d, "14-exercise-detail");

        ExerciseFormActivity f = start(ExerciseFormActivity.class, "exerciseId", 12L);
        assertShows(f, "Edit Exercise", "EXERCISE NAME", "MUSCLE GROUPS", "EQUIPMENT", "Update Exercise");
        shot(f, "15-exercise-form");
        ExerciseFormActivity nf = start(ExerciseFormActivity.class);
        assertShows(nf, "New Exercise", "Create Exercise");
    }

    @Test
    public void backupImportThroughTheDialogs() throws Exception {
        BackupActivity b = start(BackupActivity.class);
        assertShows(b, "Backup & Restore", "Export Data", "Import Data");
        shot(b, "16-backup");

        File f = new File(RuntimeEnvironment.getApplication().getCacheDir(), "in.json");
        FileOutputStream out = new FileOutputStream(f);
        out.write(BackupTest.backupText().getBytes(StandardCharsets.UTF_8));
        out.close();
        Intent view = new Intent(Intent.ACTION_VIEW);
        view.setClass(RuntimeEnvironment.getApplication(), BackupActivity.class);
        view.setDataAndType(Uri.fromFile(f), "application/json");
        b = startIntent(BackupActivity.class, view);
        AlertDialog d = waitForDialog("Import Data");
        assertEquals("Cancel", d.getButton(AlertDialog.BUTTON_NEUTRAL).getText().toString());
        assertEquals("Merge", d.getButton(AlertDialog.BUTTON_NEGATIVE).getText().toString());
        assertEquals("Replace", d.getButton(AlertDialog.BUTTON_POSITIVE).getText().toString());
        String msg = shadowOf(d).getMessage().toString();
        assertTrue(msg, msg.startsWith("Backup from "));
        assertTrue(msg, msg.contains(" gyms, ") && msg.contains("Merge keeps your current data"));
        shot(d.getWindow().getDecorView(), "17-import-dialog");
        d.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        AlertDialog sure = waitForDialog("Replace all data?");
        sure.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        AlertDialog done = waitForDialog("Import complete");
        String result = shadowOf(done).getMessage().toString();
        assertTrue(result, result.startsWith("Added ") && result.endsWith(" sets."));

        File exported = b.writeExport();
        assertTrue(exported.getName().startsWith("gymbuddy-backup-"));
        Backup.Data again = Backup.parse(new String(java.nio.file.Files.readAllBytes(exported.toPath()),
                StandardCharsets.UTF_8));
        assertEquals(Backup.parse(BackupTest.backupText()).rows("workout_sets").size(),
                again.rows("workout_sets").size());
    }
}
