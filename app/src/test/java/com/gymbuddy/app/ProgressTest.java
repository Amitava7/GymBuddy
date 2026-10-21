package com.gymbuddy.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/** Time at each weight: when it went up, and how many sessions and how long each weight lasted. */
@RunWith(RobolectricTestRunner.class)
public class ProgressTest {

    /** Sessions three days apart from Jan 1, given oldest first; returned newest first like the database. */
    private static List<Db.ExerciseSession> history(Double... kg) {
        List<Db.ExerciseSession> out = new ArrayList<Db.ExerciseSession>();
        for (int i = 0; i < kg.length; i++) {
            Db.ExerciseSession s = new Db.ExerciseSession();
            s.workoutDate = String.format("2026-01-%02d 18:00:00", 1 + i * 3);
            s.workoutName = "W";
            s.maxKg = kg[i];
            s.maxReps = 10.0;
            out.add(0, s);
        }
        return out;
    }

    @Test
    public void aLighterDayInBetweenDoesNotEndAWeight() {
        Progress.Series s = Progress.series(history(40.0, 40.0, 42.5, 30.0, 42.5, 45.0, 45.0), true);
        List<Progress.Step> steps = Progress.steps(s);
        assertEquals(3, steps.size());

        assertEquals(40, steps.get(0).kg, 0);
        assertEquals(0, steps.get(0).from);
        assertEquals(2, steps.get(0).sessions);
        assertEquals("2026-01-01 18:00:00", steps.get(0).date);
        assertEquals("2026-01-07 18:00:00", steps.get(0).until);

        // 42.5 held through the 30 kg session until 45 on Jan 16
        assertEquals(42.5, steps.get(1).kg, 0);
        assertEquals(3, steps.get(1).sessions);
        assertEquals("2026-01-16 18:00:00", steps.get(1).until);

        assertEquals(45, steps.get(2).kg, 0);
        assertEquals(5, steps.get(2).from);
        assertEquals(2, steps.get(2).sessions);
        assertNull(steps.get(2).until);
    }

    @Test
    public void neverWentUpIsOneWeight() {
        List<Progress.Step> steps = Progress.steps(Progress.series(history(60.0, 55.0, 60.0), true));
        assertEquals(1, steps.size());
        assertEquals(3, steps.get(0).sessions);
    }

    @Test
    public void sessionsWithNoWeightAreLeftOut() {
        Progress.Series s = Progress.series(history(null, 20.0, null, 25.0), true);
        assertEquals(2, s.values.size());
        assertEquals("2026-01-04 18:00:00", s.dates.get(0));
        assertFalse(Progress.series(history(0.0, null, 0.0), true).anyNonZero);
        assertTrue(Progress.series(history(0.0, null, 0.0), false).anyNonZero);
    }

    @Test
    public void howLongReadsNaturally() {
        assertEquals("1 day", Fmt.span(1));
        assertEquals("9 days", Fmt.span(9));
        assertEquals("2 weeks", Fmt.span(14));
        assertEquals("9 weeks", Fmt.span(62));
        assertEquals("2 months", Fmt.span(63));
        assertEquals("24 months", Fmt.span(729));
        assertEquals("2 years", Fmt.span(730));
        assertEquals(9, Fmt.daysBetween("2026-01-07 18:00:00", "2026-01-16 17:00:00"));
        assertEquals("1 session", Progress.sessions(1));
        assertEquals("1 rep", Progress.amount(1, false));
        assertEquals("+2.5 kg over 4 sessions", Progress.change(60, 62.5, true, 4));
        assertEquals("−3 reps over 2 sessions", Progress.change(12, 9, false, 2));
    }

    @Test
    public void chartDatesLeaveOutThisYear() {
        int y = Calendar.getInstance().get(Calendar.YEAR);
        assertEquals("Mar 7", Fmt.day(y + "-03-07 10:00:00"));
        assertEquals("Mar 7, " + (y - 1), Fmt.day((y - 1) + "-03-07 10:00:00"));
    }
}
