package com.gymbuddy.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.app.Application;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The backup format against the real SQLite code. Runs on the committed
 * sample, or on a real export with GB_BACKUP=/path/to/backup.json.
 */
@RunWith(RobolectricTestRunner.class)
public class BackupTest {

    private Application app;
    private Db db;

    @Before
    public void setUp() {
        Db.reset();
        app = RuntimeEnvironment.getApplication();
        db = Db.get(app);
    }

    @After
    public void tearDown() {
        Db.reset();
    }

    static String backupText() throws Exception {
        String path = System.getProperty("gb.backup", "");
        InputStream in = path.isEmpty()
                ? BackupTest.class.getClassLoader().getResourceAsStream("sample-backup.json")
                : new FileInputStream(new File(path));
        assertNotNull("sample-backup.json missing", in);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] b = new byte[8192];
            int n;
            while ((n = in.read(b)) > 0) out.write(b, 0, n);
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } finally {
            in.close();
        }
    }

    private long count(String table) {
        Cursor c = db.getReadableDatabase().rawQuery("SELECT COUNT(*) FROM " + table, null);
        try {
            c.moveToFirst();
            return c.getLong(0);
        } finally {
            c.close();
        }
    }

    @Test
    public void freshInstallIsSeeded() {
        assertEquals(68, count("exercises"));
        assertEquals(0, count("gyms"));
    }

    /** Replace, then export: the file that comes out is the file that went in. */
    @Test
    public void replaceThenExportGivesTheSameFileBack() throws Exception {
        String text = backupText();
        Backup.Data data = Backup.parse(text);
        Backup.Summary s = Backup.importBackup(db.getWritableDatabase(), data, true);
        assertEquals(data.rows("gyms").size(), s.gyms);
        assertEquals(data.rows("workout_sets").size(), s.sets);
        for (String t : Backup.TABLES) assertEquals(t, data.rows(t).size(), count(t));

        Map<String, Object> again = Backup.build(db.getReadableDatabase(), Fmt.iso(data.exportedAt));
        assertEquals(data.exportedAt, again.get("exported_at"));
        assertEquals(text, Json.stringify(again));
    }

    /**
     * Merge into a fresh install: the seeded exercises are matched by name,
     * duplicate workouts (same gym, name and start) are skipped, and merging the
     * same file twice adds nothing.
     */
    @Test
    public void mergeIntoFreshInstallAndAgain() throws Exception {
        Backup.Data data = Backup.parse(backupText());
        Backup.Summary first = Backup.importBackup(db.getWritableDatabase(), data, false);

        Set<String> names = new HashSet<String>();
        for (Map<String, Object> e : data.rows("exercises")) names.add(Backup.norm(e.get("name")));
        assertEquals(data.rows("gyms").size(), first.gyms);
        assertEquals(names.size(), count("exercises"));
        Set<String> keys = new HashSet<String>();
        for (Map<String, Object> w : data.rows("workouts")) {
            keys.add(w.get("gym_id") + "|" + Backup.norm(w.get("name")) + "|" + w.get("started_at"));
        }
        assertEquals(keys.size(), first.workouts);
        assertEquals(keys.size(), count("workouts"));
        assertEquals(first.sets, count("workout_sets"));
        assertEquals(data.rows("workout_templates").size(), first.templates);

        Backup.Summary second = Backup.importBackup(db.getWritableDatabase(), data, false);
        assertEquals("0 gyms, 0 exercises, 0 templates, 0 workouts, 0 sets", second.describe());
    }

    /** A merge keeps what was already there and lines the history up with it. */
    @Test
    public void mergeKeepsLocalData() throws Exception {
        long gym = db.createGym("My gym", null);
        long w = db.startWorkout("Local", gym, null);
        db.finishWorkout(w);
        Backup.importBackup(db.getWritableDatabase(), Backup.parse(backupText()), false);
        assertNotNull(db.getWorkout(w));
        assertEquals(1, db.getWorkoutHistory(gym).size());
    }

    @Test
    public void exportImportRoundTripThroughThisApp() throws Exception {
        Backup.importBackup(db.getWritableDatabase(), Backup.parse(backupText()), true);
        String exported = Json.stringify(Backup.build(db.getReadableDatabase(), new java.util.Date()));
        Db.reset();
        app.deleteDatabase(new File(new File(app.getFilesDir(), "SQLite"), "gymbuddy.db").getPath());
        db = Db.get(app);
        Backup.Data back = Backup.parse(exported);
        Backup.importBackup(db.getWritableDatabase(), back, true);
        for (String t : Backup.TABLES) assertEquals(t, back.rows(t).size(), count(t));
    }

    @Test
    public void rejectsWhatIsNotABackup() {
        expect("{nope", "The selected file is not valid JSON.");
        expect("[1,2]", "This file is not a GymBuddy backup.");
        expect("{\"app\":\"other\",\"version\":1,\"data\":{}}", "This file is not a GymBuddy backup.");
        expect("{\"app\":\"gymbuddy\",\"version\":2,\"data\":{}}",
                "This backup was created by a newer version of GymBuddy. Please update the app.");
        expect("{\"app\":\"gymbuddy\",\"version\":1,\"data\":{\"gyms\":[]}}",
                "Backup is missing the \"exercises\" table.");
    }

    private static void expect(String json, String message) {
        try {
            Backup.parse(json);
            fail("accepted " + json);
        } catch (IllegalArgumentException e) {
            assertEquals(message, e.getMessage());
        }
    }

    @Test
    public void numbersPrintLikeJavaScript() {
        assertEquals("5", Json.number(5.0));
        assertEquals("12.5", Json.number(12.5));
        assertEquals("0.0005", Json.number(0.0005));
        assertEquals("0", Json.number(-0.0));
        assertEquals("1008", Json.number(1008L));
        assertEquals("[]", Json.stringify(new java.util.ArrayList<Object>()));
    }

    /** The queries behind the screens, checked against SQL written out longhand. */
    @Test
    public void screensQueriesMatchTheData() throws Exception {
        Backup.importBackup(db.getWritableDatabase(), Backup.parse(backupText()), true);
        SQLiteDatabase sql = db.getReadableDatabase();
        for (Db.Gym g : db.getGyms()) {
            Cursor c = sql.rawQuery("SELECT COUNT(*) FROM workouts WHERE gym_id=? AND finished_at IS NOT NULL",
                    new String[]{String.valueOf(g.id)});
            c.moveToFirst();
            List<Db.HistoryItem> h = db.getWorkoutHistory(g.id);
            assertEquals(c.getLong(0), h.size());
            c.close();
            for (int i = 1; i < h.size(); i++) {
                assertTrue(h.get(i - 1).startedAt.compareTo(h.get(i).startedAt) >= 0);
            }
        }
        // the most used exercise has records and a history
        Cursor c = sql.rawQuery("SELECT we.exercise_id FROM workout_sets ws JOIN workout_exercises we"
                + " ON ws.workout_exercise_id = we.id JOIN workouts w ON we.workout_id = w.id"
                + " WHERE w.finished_at IS NOT NULL GROUP BY 1 ORDER BY COUNT(*) DESC LIMIT 1", null);
        c.moveToFirst();
        long ex = c.getLong(0);
        c.close();
        Db.Records r = db.getExerciseRecords(ex);
        assertNotNull(r.maxKg);
        assertTrue(db.getExerciseHistory(ex).size() > 0);
        assertNotNull(db.getLastWorkoutDataForExercise(ex, null, 0));
        assertNull(db.getLastWorkoutDataForExercise(ex, 987654L, 0));
    }

    @Test
    public void datesLookLikeTheOldScreens() {
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"));
        assertEquals("Tuesday, April 7", Fmt.longDay("2026-04-07 16:45:50"));
        assertEquals("Apr 7, 2026", Fmt.shortDate("2026-04-07 16:45:50"));
        assertTrue(Fmt.historyDate("2026-04-07 16:45:50"), Fmt.historyDate("2026-04-07 16:45:50")
                .matches("Apr 7, 2026, 0?4:45[\\s\\u202f]PM"));
        assertEquals("25m 35s", Fmt.durationLong(1535));
        assertEquals("1h 12m", Fmt.durationShort(4361));
        assertEquals("25m", Fmt.durationShort(1535));
        assertEquals("1:12:41", Fmt.clock(4361));
        assertEquals("00:07", Fmt.clock(7));
        assertEquals("Invalid Date", Fmt.longDay(null));
    }
}
