package com.gymbuddy.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Saved workouts carry forward, never backward: what changes in one workout
 * shows up the next time that saved workout is started, and nowhere else.
 */
@RunWith(RobolectricTestRunner.class)
public class TemplatesTest {

    private Db db;
    private long gym;

    @Before
    public void setUp() {
        Db.reset();
        db = Db.get(RuntimeEnvironment.getApplication());
        gym = db.createGym("Gym", null);
    }

    @After
    public void tearDown() {
        Db.reset();
    }

    // --------------------------------------------------------------- helpers

    private long exercise(String name) {
        for (Db.Exercise e : db.getExercises(name, null)) {
            if (e.name.equals(name)) return e.id;
        }
        throw new AssertionError("no exercise " + name);
    }

    private Db.Template template(String name, Long... exercises) {
        Db.Template t = new Db.Template();
        t.id = db.createWorkoutTemplate(name, gym);
        t.gymId = gym;
        t.name = name;
        db.setTemplateExercises(t.id, Arrays.asList(exercises));
        return t;
    }

    /** Does a saved workout: slot i gets notes[i] and the sets kg, reps, kg, reps... in sets[i]. */
    private long log(Db.Template t, String[] notes, double[][] sets) {
        long id = GymActivity.startFromTemplate(db, t);
        List<Db.WorkoutExercise> slots = db.getWorkoutExercisesWithSets(id);
        assertEquals(notes.length, slots.size());
        for (int i = 0; i < slots.size(); i++) {
            Db.WorkoutExercise ex = slots.get(i);
            for (Db.WorkoutSet s : ex.sets) db.deleteWorkoutSet(s.id);
            for (int k = 0; k + 1 < sets[i].length; k += 2) {
                db.addWorkoutSet(ex.id, k / 2 + 1, sets[i][k], sets[i][k + 1]);
            }
            db.updateWorkoutExerciseNote(ex.id, notes[i]);
        }
        WorkoutActivity.complete(db, id);
        return id;
    }

    private long log(Db.Template t, String note, double... sets) {
        return log(t, new String[]{note}, new double[][]{sets});
    }

    /** What the next start of this saved workout is filled in with. */
    private List<Db.WorkoutExercise> next(Db.Template t) {
        long id = GymActivity.startFromTemplate(db, t);
        List<Db.WorkoutExercise> out = db.getWorkoutExercisesWithSets(id);
        db.deleteWorkout(id);
        return out;
    }

    private static void assertSlot(Db.WorkoutExercise ex, String note, double... sets) {
        assertEquals(note, ex.note);
        List<Double> got = new ArrayList<Double>();
        for (Db.WorkoutSet s : ex.sets) {
            got.add(s.kg);
            got.add(s.reps);
        }
        List<Double> want = new ArrayList<Double>();
        for (double v : sets) want.add(v);
        assertEquals(want, got);
    }

    private List<Long> templateExercises(Db.Template t) {
        List<Long> out = new ArrayList<Long>();
        for (Db.TemplateExercise te : db.getTemplateExercises(t.id)) out.add(te.exerciseId);
        return out;
    }

    // ----------------------------------------------------------------- tests

    @Test
    public void twoSavedWorkoutsWithTheSameExerciseKeepTheirOwnSetsAndNotes() {
        long bench = exercise("Bench Press");
        Db.Template push = template("Push", bench);
        Db.Template chest = template("Chest", bench);

        long firstPush = log(push, "Seat 4", 60, 10, 60, 8);
        log(chest, "Wide grip", 40, 15);
        assertSlot(next(push).get(0), "Seat 4", 60, 10, 60, 8);
        assertSlot(next(chest).get(0), "Wide grip", 40, 15);

        // a heavier Push day moves Push forward and leaves Chest alone
        log(push, "Seat 5", 65, 6, 65, 6, 65, 5);
        assertSlot(next(push).get(0), "Seat 5", 65, 6, 65, 6, 65, 5);
        assertSlot(next(chest).get(0), "Wide grip", 40, 15);

        // and the first Push day is still what was done that day
        assertSlot(db.getWorkoutExercisesWithSets(firstPush).get(0), "Seat 4", 60, 10, 60, 8);
        assertEquals(3, db.getExerciseHistory(bench).size());
    }

    @Test
    public void theSameExerciseTwiceInOneWorkoutKeepsBothSlots() {
        long curl = exercise("Leg Curl");
        Db.Template legs = template("Legs", curl, exercise("Leg Press"), curl);

        log(legs, new String[]{"warm up", "sled", "top set"},
                new double[][]{{30, 12}, {100, 10}, {50, 6, 50, 5}});
        List<Db.WorkoutExercise> again = next(legs);
        assertSlot(again.get(0), "warm up", 30, 12);
        assertSlot(again.get(1), "sled", 100, 10);
        assertSlot(again.get(2), "top set", 50, 6, 50, 5);
    }

    @Test
    public void removingAnExerciseChangesTheSavedWorkoutFromThatWorkoutOn() {
        long bench = exercise("Bench Press"), fly = exercise("Cable Fly");
        Db.Template push = template("Push", bench, fly);
        long first = log(push, new String[]{"", "slow"}, new double[][]{{60, 10}, {15, 12}});

        // removed, then the workout is cancelled: nothing happened
        long cancelled = GymActivity.startFromTemplate(db, push);
        db.deleteWorkoutExercise(db.getWorkoutExercises(cancelled).get(1).id);
        db.deleteWorkout(cancelled);
        assertEquals(Arrays.asList(bench, fly), templateExercises(push));

        // removed, then finished: gone from here on, still in the first workout
        long done = GymActivity.startFromTemplate(db, push);
        db.deleteWorkoutExercise(db.getWorkoutExercises(done).get(1).id);
        WorkoutActivity.complete(db, done);
        assertEquals(Arrays.asList(bench), templateExercises(push));
        assertEquals(2, db.getWorkoutExercises(first).size());
        assertSlot(db.getWorkoutExercisesWithSets(first).get(1), "slow", 15, 12);
        assertEquals(1, db.getExerciseHistory(fly).size());
    }

    @Test
    public void addedExercisesJoinTheSavedWorkoutInOrder() {
        long bench = exercise("Bench Press"), dip = exercise("Chest Dip");
        Db.Template push = template("Push", bench);
        long id = GymActivity.startFromTemplate(db, push);
        db.addWorkoutExercise(id, dip, 1);
        db.addWorkoutExercise(id, bench, 2);
        WorkoutActivity.complete(db, id);
        assertEquals(Arrays.asList(bench, dip, bench), templateExercises(push));
    }

    @Test
    public void aWorkoutFromScratchBecomesASavedWorkout() {
        long squat = exercise("Squat");
        long id = db.startWorkout("Leg day", gym, null);
        db.addWorkoutExercise(id, squat, 0);
        WorkoutActivity.complete(db, id);
        Db.Workout w = db.getWorkout(id);
        assertTrue(w.finishedAt != null && w.templateId != null);
        List<Db.Template> saved = db.getWorkoutTemplates(gym);
        assertEquals(1, saved.size());
        assertEquals("Leg day", saved.get(0).name);
        assertEquals(Arrays.asList(squat), templateExercises(saved.get(0)));
    }

    @Test
    public void gridlinesFallOnRoundNumbers() {
        assertEquals(1, LineChart.niceStep(0.4), 0);
        assertEquals(2, LineChart.niceStep(1.3), 0);
        assertEquals(2.5, LineChart.niceStep(2.2), 0);
        assertEquals(5, LineChart.niceStep(3), 0);
        assertEquals(10, LineChart.niceStep(7.5), 0);
        assertEquals(25, LineChart.niceStep(21), 0);
    }
}
