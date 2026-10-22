package com.gymbuddy.app;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Insets;
import android.graphics.Rect;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.EditText;
import android.widget.ScrollView;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Whatever field you type in stays in sight when the keyboard comes up: not
 * under the keyboard, and not under a bar pinned to the bottom of the screen.
 * The keyboard is simulated the way Android reports it to an edge-to-edge
 * window: as an IME inset along the bottom.
 */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xhdpi")
public class KeyboardTest {

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

    private static void idle() {
        shadowOf(Looper.getMainLooper()).idle();
    }

    private static <T extends Activity> T start(Class<T> c, Object... extras) {
        Intent i = new Intent(RuntimeEnvironment.getApplication(), c);
        for (int k = 0; k + 1 < extras.length; k += 2) i.putExtra((String) extras[k], (Long) extras[k + 1]);
        T a = Robolectric.buildActivity(c, i).setup().get();
        idle();
        return a;
    }

    private static int dp(Activity a, float v) {
        return Ui.dp(a, v);
    }

    /** Status bar 24dp, gesture bar 16dp, and the keyboard over the bottom 40% of the screen. */
    private static int keyboardUp(Screen s) {
        int h = s.root.getHeight();
        int ime = (int) (h * 0.4f);
        WindowInsets in = new WindowInsets.Builder()
                .setInsets(WindowInsets.Type.statusBars(), Insets.of(0, dp(s, 24), 0, 0))
                .setInsets(WindowInsets.Type.navigationBars(), Insets.of(0, 0, 0, dp(s, 16)))
                .setInsets(WindowInsets.Type.ime(), Insets.of(0, 0, 0, ime))
                .setVisible(WindowInsets.Type.ime(), true)
                .build();
        s.root.dispatchApplyWindowInsets(in);
        idle();
        s.root.measure(View.MeasureSpec.makeMeasureSpec(s.root.getWidth(), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY));
        s.root.layout(0, 0, s.root.getWidth(), h);
        settle(s.root);
        return h - ime;
    }

    /** Runs the scroll views' smooth-scroll animations to the end, as the frames on a phone would. */
    private static void settle(View root) {
        for (int frame = 0; frame < 60; frame++) {
            shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(16));
            computeScroll(root);
        }
    }

    private static void computeScroll(View v) {
        if (v instanceof ScrollView) v.computeScroll();
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) computeScroll(g.getChildAt(i));
        }
    }

    private static Rect onScreen(View v) {
        int[] at = new int[2];
        v.getLocationInWindow(at);
        return new Rect(at[0], at[1], at[0] + v.getWidth(), at[1] + v.getHeight());
    }

    /**
     * Taps the field (focus), brings the keyboard up, and checks the whole
     * field is above the keyboard, inside the scroll view showing it, and not
     * covered by anything drawn over it.
     */
    private static void assertTypingVisible(Screen s, EditText field, String name) throws Exception {
        assertTrue(name + ": the field takes no focus", field.requestFocus());
        idle();
        int keyboardTop = keyboardUp(s);
        shot(s, name);
        Rect r = onScreen(field);
        assertTrue(name + ": the field " + r + " is under the keyboard at " + keyboardTop, r.bottom <= keyboardTop);
        for (View p = (View) field.getParent(); p != s.root; p = (View) p.getParent()) {
            if (p instanceof ScrollView) {
                Rect sr = onScreen(p);
                assertTrue(name + ": the field " + r + " is scrolled out of " + sr, sr.contains(r));
            }
        }
        // anything later in the content frame (a bar pinned to the bottom) must not sit on it
        int mine = -1;
        for (int i = 0; i < s.content.getChildCount(); i++) {
            if (isAncestor(s.content.getChildAt(i), field)) mine = i;
        }
        for (int i = mine + 1; i < s.content.getChildCount(); i++) {
            View over = s.content.getChildAt(i);
            if (over.getVisibility() != View.VISIBLE) continue;
            assertTrue(name + ": " + over + " at " + onScreen(over) + " covers the field " + r,
                    !Rect.intersects(onScreen(over), r));
        }
    }

    private static boolean isAncestor(View a, View v) {
        for (View p = v; p != null; p = p.getParent() instanceof View ? (View) p.getParent() : null) {
            if (p == a) return true;
        }
        return false;
    }

    private static void shot(Screen s, String name) throws Exception {
        String dir = System.getProperty("gb.shots", "");
        if (dir.isEmpty()) return;
        android.graphics.Bitmap b = android.graphics.Bitmap.createBitmap(s.root.getWidth(), s.root.getHeight(),
                android.graphics.Bitmap.Config.ARGB_8888);
        s.root.draw(new android.graphics.Canvas(b));
        new File(dir).mkdirs();
        FileOutputStream out = new FileOutputStream(new File(dir, "kb-" + name + ".png"));
        b.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out);
        out.close();
    }

    private static void inputs(View v, List<EditText> out) {
        if (v.getVisibility() != View.VISIBLE) return;
        if (v instanceof EditText) out.add((EditText) v);
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) inputs(g.getChildAt(i), out);
        }
    }

    private static List<EditText> inputs(Screen s) {
        List<EditText> out = new ArrayList<EditText>();
        inputs(s.content, out);
        return out;
    }

    // ----------------------------------------------------------------- tests

    /** The reps of the last set of the last exercise, scrolled to the bottom of the screen first. */
    @Test
    public void repsAtTheBottomOfAWorkout() throws Exception {
        Db.Gym g = db.getGyms().get(0);
        Db.Template t = db.getWorkoutTemplates(g.id).get(0);
        long id = GymActivity.startFromTemplate(db, t);
        WorkoutActivity w = start(WorkoutActivity.class, "gymId", g.id, "workoutId", id);
        List<EditText> fields = inputs(w);
        EditText reps = fields.get(fields.size() - 1);
        // as far down as it goes, so the last field sits just above the Finish bar
        ScrollView scroll = null;
        for (View p = (View) reps.getParent(); scroll == null; p = (View) p.getParent()) {
            if (p instanceof ScrollView) scroll = (ScrollView) p;
        }
        scroll.fullScroll(View.FOCUS_DOWN);
        idle();
        assertTypingVisible(w, reps, "workout-reps");
    }

    @Test
    public void aNoteHalfWayDownAWorkout() throws Exception {
        Db.Gym g = db.getGyms().get(0);
        Db.Template t = db.getWorkoutTemplates(g.id).get(0);
        long id = GymActivity.startFromTemplate(db, t);
        WorkoutActivity w = start(WorkoutActivity.class, "gymId", g.id, "workoutId", id);
        EditText note = null;
        for (EditText e : inputs(w)) {
            if (onScreen(e).top > w.root.getHeight() * 0.6 && "Add note...".contentEquals(e.getHint())) {
                note = e;
                break;
            }
        }
        assertNotNull("no note field low on the screen", note);
        assertTypingVisible(w, note, "workout-note");
    }

    @Test
    public void aNewWorkoutsName() throws Exception {
        NewWorkoutActivity n = start(NewWorkoutActivity.class, "gymId", db.getGyms().get(0).id);
        assertTypingVisible(n, inputs(n).get(0), "new-workout-name");
    }

    @Test
    public void aNewSavedWorkoutsName() throws Exception {
        GymActivity g = start(GymActivity.class, "gymId", db.getGyms().get(0).id);
        View add = find(g.root, "New workout template");
        assertNotNull(add);
        add.performClick();
        idle();
        assertTypingVisible(g, inputs(g).get(0), "new-template-name");
    }

    @Test
    public void aNewExercisesNameWhilePicking() throws Exception {
        long id = db.startWorkout("W", db.getGyms().get(0).id, null);
        PickExerciseActivity p = start(PickExerciseActivity.class, "workoutId", id);
        View create = null;
        for (View v : clickables(p.root)) {
            if (v instanceof ViewGroup && texts(v).contains("Create New Exercise")) create = v;
        }
        create.performClick();
        idle();
        List<EditText> fields = inputs(p);
        assertTypingVisible(p, fields.get(fields.size() - 1), "pick-new-exercise");
    }

    @Test
    public void anExercisesDetails() throws Exception {
        ExerciseFormActivity f = start(ExerciseFormActivity.class, "exerciseId", 12L);
        List<EditText> fields = inputs(f);
        assertTypingVisible(f, fields.get(fields.size() - 1), "exercise-details");
    }

    @Test
    public void aNewGymsLocation() throws Exception {
        MainActivity m = start(MainActivity.class);
        find(m.root, "Add").performClick();
        idle();
        List<EditText> fields = inputs(m);
        assertTypingVisible(m, fields.get(fields.size() - 1), "new-gym-location");
    }

    private static View find(View v, String description) {
        if (description.contentEquals(v.getContentDescription() == null ? "" : v.getContentDescription())) return v;
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) {
                View f = find(g.getChildAt(i), description);
                if (f != null) return f;
            }
        }
        return null;
    }

    private static List<View> clickables(View v) {
        List<View> out = new ArrayList<View>();
        if (v.isClickable()) out.add(v);
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) out.addAll(clickables(g.getChildAt(i)));
        }
        return out;
    }

    private static List<String> texts(View v) {
        List<String> out = new ArrayList<String>();
        if (v instanceof android.widget.TextView) out.add(((android.widget.TextView) v).getText().toString());
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) out.addAll(texts(g.getChildAt(i)));
        }
        return out;
    }
}
