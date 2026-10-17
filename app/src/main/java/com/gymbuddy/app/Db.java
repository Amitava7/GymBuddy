package com.gymbuddy.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Everything on disk. The schema is the one the React Native app created with
 * drizzle, column for column, and the file lives where expo-sqlite kept it
 * (files/SQLite/gymbuddy.db), so a backup from either app means the same thing.
 */
final class Db extends SQLiteOpenHelper {

    static final String[] MUSCLE_TAGS = {"chest", "back", "shoulders", "arms", "legs", "core", "glutes"};
    static final String[] EQUIPMENT_TAGS = {"barbell", "dumbbell", "machine", "cable", "bodyweight", "kettlebell"};

    private static Db sInstance;

    static synchronized Db get(Context ctx) {
        if (sInstance == null) sInstance = new Db(ctx.getApplicationContext());
        return sInstance;
    }

    /** Tests open a fresh database per run. */
    static synchronized void reset() {
        if (sInstance != null) sInstance.close();
        sInstance = null;
    }

    private Db(Context ctx) {
        super(ctx, file(ctx), null, 1);
        setWriteAheadLoggingEnabled(true);
    }

    private static String file(Context ctx) {
        File dir = new File(ctx.getFilesDir(), "SQLite");
        dir.mkdirs();
        return new File(dir, "gymbuddy.db").getPath();
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // The drizzle migration, verbatim. IF NOT EXISTS keeps a database the
        // Expo app left behind exactly as it was.
        db.execSQL("CREATE TABLE IF NOT EXISTS `exercises` (" +
                "`id` integer PRIMARY KEY AUTOINCREMENT NOT NULL," +
                "`name` text NOT NULL," +
                "`details` text," +
                "`tags` text," +
                "`created_at` text DEFAULT (datetime('now')))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `gyms` (" +
                "`id` integer PRIMARY KEY AUTOINCREMENT NOT NULL," +
                "`name` text NOT NULL," +
                "`location` text," +
                "`created_at` text DEFAULT (datetime('now')))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `template_exercises` (" +
                "`id` integer PRIMARY KEY AUTOINCREMENT NOT NULL," +
                "`template_id` integer NOT NULL," +
                "`exercise_id` integer NOT NULL," +
                "`sort_order` integer DEFAULT 0 NOT NULL," +
                "FOREIGN KEY (`template_id`) REFERENCES `workout_templates`(`id`) ON UPDATE no action ON DELETE cascade," +
                "FOREIGN KEY (`exercise_id`) REFERENCES `exercises`(`id`) ON UPDATE no action ON DELETE cascade)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `workout_exercises` (" +
                "`id` integer PRIMARY KEY AUTOINCREMENT NOT NULL," +
                "`workout_id` integer NOT NULL," +
                "`exercise_id` integer NOT NULL," +
                "`note` text," +
                "`is_completed` integer DEFAULT 0 NOT NULL," +
                "`sort_order` integer DEFAULT 0 NOT NULL," +
                "FOREIGN KEY (`workout_id`) REFERENCES `workouts`(`id`) ON UPDATE no action ON DELETE cascade," +
                "FOREIGN KEY (`exercise_id`) REFERENCES `exercises`(`id`) ON UPDATE no action ON DELETE cascade)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `workout_sets` (" +
                "`id` integer PRIMARY KEY AUTOINCREMENT NOT NULL," +
                "`workout_exercise_id` integer NOT NULL," +
                "`set_number` integer NOT NULL," +
                "`kg` real," +
                "`reps` integer," +
                "FOREIGN KEY (`workout_exercise_id`) REFERENCES `workout_exercises`(`id`) ON UPDATE no action ON DELETE cascade)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `workout_templates` (" +
                "`id` integer PRIMARY KEY AUTOINCREMENT NOT NULL," +
                "`name` text NOT NULL," +
                "`gym_id` integer NOT NULL," +
                "`created_at` text DEFAULT (datetime('now'))," +
                "FOREIGN KEY (`gym_id`) REFERENCES `gyms`(`id`) ON UPDATE no action ON DELETE cascade)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `workouts` (" +
                "`id` integer PRIMARY KEY AUTOINCREMENT NOT NULL," +
                "`template_id` integer," +
                "`gym_id` integer NOT NULL," +
                "`name` text NOT NULL," +
                "`started_at` text DEFAULT (datetime('now'))," +
                "`finished_at` text," +
                "`duration_seconds` integer DEFAULT 0 NOT NULL," +
                "FOREIGN KEY (`template_id`) REFERENCES `workout_templates`(`id`) ON UPDATE no action ON DELETE set null," +
                "FOREIGN KEY (`gym_id`) REFERENCES `gyms`(`id`) ON UPDATE no action ON DELETE cascade)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int from, int to) {
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        seedExercisesIfNeeded(db);
    }

    // ------------------------------------------------------------------ rows

    static final class Gym {
        long id;
        String name, location;
    }

    static final class Exercise {
        long id;
        String name, details, tags;
    }

    static final class Template {
        long id, gymId;
        String name;
    }

    static final class TemplateExercise {
        long id, templateId, exerciseId;
        int sortOrder;
        String name, details;
    }

    static final class Workout {
        long id, gymId, durationSeconds;
        Long templateId;
        String name, startedAt, finishedAt;
    }

    static final class HistoryItem {
        long id, durationSeconds;
        String name, startedAt;
        int exerciseCount, totalSets;
    }

    static final class WorkoutExercise {
        long id, workoutId, exerciseId;
        String note, name, details;
        int isCompleted, sortOrder;
        List<WorkoutSet> sets = new ArrayList<WorkoutSet>();
    }

    static final class WorkoutSet {
        long id, workoutExerciseId;
        int setNumber;
        Double kg, reps;
    }

    static final class LastData {
        String note;
        List<WorkoutSet> sets = new ArrayList<WorkoutSet>();
    }

    static final class ExerciseSession {
        String workoutDate, workoutName;
        Double maxKg, maxReps;
        int totalSets;
        double totalVolume;
    }

    static final class Records {
        Double maxKg, maxReps, maxVolume;
    }

    // --------------------------------------------------------------- helpers

    private SQLiteDatabase r() {
        return getReadableDatabase();
    }

    private SQLiteDatabase w() {
        return getWritableDatabase();
    }

    private static String[] args(Object... a) {
        String[] out = new String[a.length];
        for (int i = 0; i < a.length; i++) out[i] = String.valueOf(a[i]);
        return out;
    }

    static String str(Cursor c, int i) {
        return c.isNull(i) ? null : c.getString(i);
    }

    static Double dbl(Cursor c, int i) {
        return c.isNull(i) ? null : c.getDouble(i);
    }

    static Long lng(Cursor c, int i) {
        return c.isNull(i) ? null : c.getLong(i);
    }

    private static String orNull(String s) {
        return s == null || s.isEmpty() ? null : s;
    }

    // ------------------------------------------------------------------ gyms

    List<Gym> getGyms() {
        List<Gym> out = new ArrayList<Gym>();
        Cursor c = r().rawQuery("SELECT id, name, location FROM gyms ORDER BY name", null);
        try {
            while (c.moveToNext()) {
                Gym g = new Gym();
                g.id = c.getLong(0);
                g.name = c.getString(1);
                g.location = str(c, 2);
                out.add(g);
            }
        } finally {
            c.close();
        }
        return out;
    }

    long createGym(String name, String location) {
        ContentValues v = new ContentValues();
        v.put("name", name);
        v.put("location", orNull(location));
        return w().insertOrThrow("gyms", null, v);
    }

    void deleteGym(long id) {
        w().delete("gyms", "id=?", args(id));
    }

    // ------------------------------------------------------------- exercises

    List<Exercise> getExercises(String search, List<String> tags) {
        StringBuilder where = new StringBuilder();
        List<String> a = new ArrayList<String>();
        if (search != null && !search.isEmpty()) {
            where.append("name LIKE ?");
            a.add("%" + search + "%");
        }
        if (tags != null) {
            for (String t : tags) {
                if (where.length() > 0) where.append(" AND ");
                where.append("tags LIKE ?");
                a.add("%" + t + "%");
            }
        }
        String sql = "SELECT id, name, details, tags FROM exercises"
                + (where.length() > 0 ? " WHERE " + where : "") + " ORDER BY name";
        List<Exercise> out = new ArrayList<Exercise>();
        Cursor c = r().rawQuery(sql, a.toArray(new String[0]));
        try {
            while (c.moveToNext()) out.add(exercise(c));
        } finally {
            c.close();
        }
        return out;
    }

    private static Exercise exercise(Cursor c) {
        Exercise e = new Exercise();
        e.id = c.getLong(0);
        e.name = c.getString(1);
        e.details = str(c, 2);
        e.tags = str(c, 3);
        return e;
    }

    Exercise getExercise(long id) {
        Cursor c = r().rawQuery("SELECT id, name, details, tags FROM exercises WHERE id=?", args(id));
        try {
            return c.moveToFirst() ? exercise(c) : null;
        } finally {
            c.close();
        }
    }

    long createExercise(String name, String details, String tags) {
        ContentValues v = new ContentValues();
        v.put("name", name);
        v.put("details", orNull(details));
        v.put("tags", orNull(tags));
        return w().insertOrThrow("exercises", null, v);
    }

    void updateExercise(long id, String name, String details, String tags) {
        ContentValues v = new ContentValues();
        v.put("name", name);
        v.put("details", orNull(details));
        v.put("tags", orNull(tags));
        w().update("exercises", v, "id=?", args(id));
    }

    void deleteExercise(long id) {
        w().delete("exercises", "id=?", args(id));
    }

    // ------------------------------------------------------------- templates

    List<Template> getWorkoutTemplates(long gymId) {
        List<Template> out = new ArrayList<Template>();
        Cursor c = r().rawQuery("SELECT id, name, gym_id FROM workout_templates WHERE gym_id=?"
                + " ORDER BY created_at DESC, id DESC", args(gymId));
        try {
            while (c.moveToNext()) {
                Template t = new Template();
                t.id = c.getLong(0);
                t.name = c.getString(1);
                t.gymId = c.getLong(2);
                out.add(t);
            }
        } finally {
            c.close();
        }
        return out;
    }

    long createWorkoutTemplate(String name, long gymId) {
        ContentValues v = new ContentValues();
        v.put("name", name);
        v.put("gym_id", gymId);
        return w().insertOrThrow("workout_templates", null, v);
    }

    void addTemplateExercise(long templateId, long exerciseId, int sortOrder) {
        ContentValues v = new ContentValues();
        v.put("template_id", templateId);
        v.put("exercise_id", exerciseId);
        v.put("sort_order", sortOrder);
        w().insertOrThrow("template_exercises", null, v);
    }

    List<TemplateExercise> getTemplateExercises(long templateId) {
        List<TemplateExercise> out = new ArrayList<TemplateExercise>();
        Cursor c = r().rawQuery("SELECT te.id, te.template_id, te.exercise_id, te.sort_order, e.name, e.details"
                + " FROM template_exercises te JOIN exercises e ON te.exercise_id = e.id"
                + " WHERE te.template_id=? ORDER BY te.sort_order", args(templateId));
        try {
            while (c.moveToNext()) {
                TemplateExercise t = new TemplateExercise();
                t.id = c.getLong(0);
                t.templateId = c.getLong(1);
                t.exerciseId = c.getLong(2);
                t.sortOrder = c.getInt(3);
                t.name = c.getString(4);
                t.details = str(c, 5);
                out.add(t);
            }
        } finally {
            c.close();
        }
        return out;
    }

    void deleteWorkoutTemplate(long id) {
        w().delete("workout_templates", "id=?", args(id));
    }

    void removeTemplateExercise(long templateId, long exerciseId) {
        w().delete("template_exercises", "template_id=? AND exercise_id=?", args(templateId, exerciseId));
    }

    // -------------------------------------------------------------- workouts

    long startWorkout(String name, long gymId, Long templateId) {
        ContentValues v = new ContentValues();
        v.put("name", name);
        v.put("gym_id", gymId);
        if (templateId != null && templateId != 0) v.put("template_id", templateId);
        else v.putNull("template_id");
        return w().insertOrThrow("workouts", null, v);
    }

    void finishWorkout(long id) {
        w().execSQL("UPDATE workouts SET finished_at = datetime('now'),"
                + " duration_seconds = CAST((julianday(datetime('now')) - julianday(started_at)) * 86400 AS INTEGER)"
                + " WHERE id=?", new Object[]{id});
    }

    void setWorkoutTemplate(long workoutId, long templateId) {
        ContentValues v = new ContentValues();
        v.put("template_id", templateId);
        w().update("workouts", v, "id=?", args(workoutId));
    }

    void deleteWorkout(long id) {
        w().delete("workouts", "id=?", args(id));
    }

    Workout getWorkout(long id) {
        Cursor c = r().rawQuery("SELECT id, template_id, gym_id, name, started_at, finished_at, duration_seconds"
                + " FROM workouts WHERE id=?", args(id));
        try {
            if (!c.moveToFirst()) return null;
            Workout wk = new Workout();
            wk.id = c.getLong(0);
            wk.templateId = lng(c, 1);
            wk.gymId = c.getLong(2);
            wk.name = c.getString(3);
            wk.startedAt = str(c, 4);
            wk.finishedAt = str(c, 5);
            wk.durationSeconds = c.getLong(6);
            return wk;
        } finally {
            c.close();
        }
    }

    /** Finished workouts at one gym, newest first. */
    List<HistoryItem> getWorkoutHistory(long gymId) {
        List<HistoryItem> out = new ArrayList<HistoryItem>();
        Cursor c = r().rawQuery("SELECT w.id, w.name, w.started_at, w.duration_seconds,"
                + " (SELECT COUNT(*) FROM workout_exercises we WHERE we.workout_id = w.id AND we.is_completed = 1),"
                + " (SELECT COUNT(*) FROM workout_sets ws JOIN workout_exercises we ON ws.workout_exercise_id = we.id"
                + "   WHERE we.workout_id = w.id AND we.is_completed = 1)"
                + " FROM workouts w JOIN gyms g ON w.gym_id = g.id"
                + " WHERE w.finished_at IS NOT NULL AND w.gym_id = ?"
                + " ORDER BY w.started_at DESC", args(gymId));
        try {
            while (c.moveToNext()) {
                HistoryItem h = new HistoryItem();
                h.id = c.getLong(0);
                h.name = c.getString(1);
                h.startedAt = str(c, 2);
                h.durationSeconds = c.getLong(3);
                h.exerciseCount = c.getInt(4);
                h.totalSets = c.getInt(5);
                out.add(h);
            }
        } finally {
            c.close();
        }
        return out;
    }

    // ----------------------------------------------------- workout exercises

    long addWorkoutExercise(long workoutId, long exerciseId, int sortOrder) {
        ContentValues v = new ContentValues();
        v.put("workout_id", workoutId);
        v.put("exercise_id", exerciseId);
        v.put("sort_order", sortOrder);
        return w().insertOrThrow("workout_exercises", null, v);
    }

    List<WorkoutExercise> getWorkoutExercises(long workoutId) {
        List<WorkoutExercise> out = new ArrayList<WorkoutExercise>();
        Cursor c = r().rawQuery("SELECT we.id, we.workout_id, we.exercise_id, we.note, we.is_completed,"
                + " we.sort_order, e.name, e.details"
                + " FROM workout_exercises we JOIN exercises e ON we.exercise_id = e.id"
                + " WHERE we.workout_id=? ORDER BY we.sort_order", args(workoutId));
        try {
            while (c.moveToNext()) {
                WorkoutExercise e = new WorkoutExercise();
                e.id = c.getLong(0);
                e.workoutId = c.getLong(1);
                e.exerciseId = c.getLong(2);
                e.note = str(c, 3);
                e.isCompleted = c.getInt(4);
                e.sortOrder = c.getInt(5);
                e.name = c.getString(6);
                e.details = str(c, 7);
                out.add(e);
            }
        } finally {
            c.close();
        }
        return out;
    }

    /** The exercises with their sets filled in. */
    List<WorkoutExercise> getWorkoutExercisesWithSets(long workoutId) {
        List<WorkoutExercise> out = getWorkoutExercises(workoutId);
        for (WorkoutExercise e : out) e.sets = getWorkoutSets(e.id);
        return out;
    }

    void toggleWorkoutExercise(long id, boolean completed) {
        ContentValues v = new ContentValues();
        v.put("is_completed", completed ? 1 : 0);
        w().update("workout_exercises", v, "id=?", args(id));
    }

    void updateWorkoutExerciseNote(long id, String note) {
        ContentValues v = new ContentValues();
        v.put("note", note);
        w().update("workout_exercises", v, "id=?", args(id));
    }

    void deleteWorkoutExercise(long id) {
        w().delete("workout_sets", "workout_exercise_id=?", args(id));
        w().delete("workout_exercises", "id=?", args(id));
    }

    // ------------------------------------------------------------------ sets

    long addWorkoutSet(long workoutExerciseId, int setNumber, Double kg, Double reps) {
        ContentValues v = new ContentValues();
        v.put("workout_exercise_id", workoutExerciseId);
        v.put("set_number", setNumber);
        putNumber(v, "kg", kg);
        putNumber(v, "reps", reps);
        return w().insertOrThrow("workout_sets", null, v);
    }

    /** Writes one field of a set as it is typed: "kg" or "reps". */
    void updateWorkoutSet(long id, String field, Double value) {
        ContentValues v = new ContentValues();
        putNumber(v, field, value);
        w().update("workout_sets", v, "id=?", args(id));
    }

    private static void putNumber(ContentValues v, String key, Double value) {
        if (value == null) v.putNull(key);
        else if (value == Math.rint(value) && Math.abs(value) < 1e15) v.put(key, value.longValue());
        else v.put(key, value);
    }

    void deleteWorkoutSet(long id) {
        w().delete("workout_sets", "id=?", args(id));
    }

    List<WorkoutSet> getWorkoutSets(long workoutExerciseId) {
        List<WorkoutSet> out = new ArrayList<WorkoutSet>();
        Cursor c = r().rawQuery("SELECT id, workout_exercise_id, set_number, kg, reps FROM workout_sets"
                + " WHERE workout_exercise_id=? ORDER BY set_number", args(workoutExerciseId));
        try {
            while (c.moveToNext()) {
                WorkoutSet s = new WorkoutSet();
                s.id = c.getLong(0);
                s.workoutExerciseId = c.getLong(1);
                s.setNumber = c.getInt(2);
                s.kg = dbl(c, 3);
                s.reps = dbl(c, 4);
                out.add(s);
            }
        } finally {
            c.close();
        }
        return out;
    }

    // ---------------------------------------------------- history & records

    /**
     * What you did last time: the note and sets from the most recent finished
     * workout with this exercise. With a template, only workouts from that
     * template count.
     */
    LastData getLastWorkoutDataForExercise(long exerciseId, Long templateId) {
        boolean byTemplate = templateId != null && templateId != 0;
        Cursor c = r().rawQuery("SELECT we.id, we.note FROM workout_exercises we"
                        + " JOIN workouts w ON we.workout_id = w.id"
                        + " WHERE we.exercise_id=? AND w.finished_at IS NOT NULL"
                        + (byTemplate ? " AND w.template_id=?" : "")
                        + " ORDER BY w.started_at DESC LIMIT 1",
                byTemplate ? args(exerciseId, templateId) : args(exerciseId));
        long weId;
        LastData out = new LastData();
        try {
            if (!c.moveToFirst()) return null;
            weId = c.getLong(0);
            out.note = str(c, 1);
        } finally {
            c.close();
        }
        out.sets = getWorkoutSets(weId);
        return out;
    }

    List<ExerciseSession> getExerciseHistory(long exerciseId) {
        List<ExerciseSession> out = new ArrayList<ExerciseSession>();
        Cursor c = r().rawQuery("SELECT w.started_at, w.name, MAX(ws.kg), MAX(ws.reps), COUNT(ws.id),"
                + " SUM(COALESCE(ws.kg, 0) * COALESCE(ws.reps, 0))"
                + " FROM workout_sets ws"
                + " JOIN workout_exercises we ON ws.workout_exercise_id = we.id"
                + " JOIN workouts w ON we.workout_id = w.id"
                + " WHERE we.exercise_id=? AND w.finished_at IS NOT NULL"
                + " GROUP BY w.id ORDER BY w.started_at DESC", args(exerciseId));
        try {
            while (c.moveToNext()) {
                ExerciseSession s = new ExerciseSession();
                s.workoutDate = str(c, 0);
                s.workoutName = c.getString(1);
                s.maxKg = dbl(c, 2);
                s.maxReps = dbl(c, 3);
                s.totalSets = c.getInt(4);
                s.totalVolume = c.isNull(5) ? 0 : c.getDouble(5);
                out.add(s);
            }
        } finally {
            c.close();
        }
        return out;
    }

    Records getExerciseRecords(long exerciseId) {
        Cursor c = r().rawQuery("SELECT MAX(ws.kg), MAX(ws.reps),"
                + " MAX(COALESCE(ws.kg, 0) * COALESCE(ws.reps, 0))"
                + " FROM workout_sets ws"
                + " JOIN workout_exercises we ON ws.workout_exercise_id = we.id"
                + " JOIN workouts w ON we.workout_id = w.id"
                + " WHERE we.exercise_id=? AND w.finished_at IS NOT NULL", args(exerciseId));
        try {
            if (!c.moveToFirst()) return null;
            Records rec = new Records();
            rec.maxKg = dbl(c, 0);
            rec.maxReps = dbl(c, 1);
            rec.maxVolume = dbl(c, 2);
            return rec;
        } finally {
            c.close();
        }
    }

    // ------------------------------------------------------------------ seed

    private static final String[][] SEED = {
            // Chest
            {"Bench Press", null, "chest,barbell"},
            {"Incline Bench Press", null, "chest,barbell"},
            {"Decline Bench Press", null, "chest,barbell"},
            {"Dumbbell Bench Press", null, "chest,dumbbell"},
            {"Incline Dumbbell Press", null, "chest,dumbbell"},
            {"Dumbbell Fly", null, "chest,dumbbell"},
            {"Cable Fly", null, "chest,cable"},
            {"Chest Press Machine", null, "chest,machine"},
            {"Pec Deck", null, "chest,machine"},
            {"Push Up", null, "chest,bodyweight"},
            {"Chest Dip", null, "chest,bodyweight"},
            // Back
            {"Deadlift", "Keep back straight, drive through heels", "back,barbell,legs"},
            {"Romanian Deadlift", null, "back,barbell,legs"},
            {"Barbell Row", null, "back,barbell"},
            {"Dumbbell Row", null, "back,dumbbell"},
            {"Pull Up", null, "back,bodyweight"},
            {"Chin Up", null, "back,arms,bodyweight"},
            {"Lat Pulldown", null, "back,cable,machine"},
            {"Seated Cable Row", null, "back,cable"},
            {"T-Bar Row", null, "back,machine"},
            {"Face Pull", null, "back,shoulders,cable"},
            {"Good Morning", null, "back,barbell"},
            {"Back Extension", null, "back,bodyweight"},
            // Shoulders
            {"Overhead Press", null, "shoulders,barbell"},
            {"Dumbbell Shoulder Press", null, "shoulders,dumbbell"},
            {"Arnold Press", null, "shoulders,dumbbell"},
            {"Lateral Raise", null, "shoulders,dumbbell"},
            {"Front Raise", null, "shoulders,dumbbell"},
            {"Rear Delt Fly", null, "shoulders,dumbbell"},
            {"Cable Lateral Raise", null, "shoulders,cable"},
            {"Machine Shoulder Press", null, "shoulders,machine"},
            {"Upright Row", null, "shoulders,barbell"},
            {"Shrugs", null, "shoulders,barbell"},
            // Arms
            {"Barbell Curl", null, "arms,barbell"},
            {"Dumbbell Curl", null, "arms,dumbbell"},
            {"Hammer Curl", null, "arms,dumbbell"},
            {"Preacher Curl", null, "arms,machine"},
            {"Cable Curl", null, "arms,cable"},
            {"Concentration Curl", null, "arms,dumbbell"},
            {"Close Grip Bench Press", null, "arms,chest,barbell"},
            {"Skull Crusher", null, "arms,barbell"},
            {"Tricep Pushdown", null, "arms,cable"},
            {"Overhead Tricep Extension", null, "arms,dumbbell"},
            {"Tricep Dip", null, "arms,bodyweight"},
            {"Diamond Push Up", null, "arms,chest,bodyweight"},
            // Legs
            {"Squat", "Keep knees tracking over toes, chest up", "legs,barbell"},
            {"Front Squat", null, "legs,barbell"},
            {"Sumo Deadlift", null, "legs,barbell"},
            {"Leg Press", null, "legs,machine"},
            {"Leg Extension", null, "legs,machine"},
            {"Leg Curl", null, "legs,machine"},
            {"Calf Raise", null, "legs,machine"},
            {"Lunges", null, "legs,dumbbell"},
            {"Bulgarian Split Squat", null, "legs,dumbbell"},
            {"Goblet Squat", null, "legs,kettlebell"},
            {"Hip Thrust", null, "legs,glutes,barbell"},
            {"Glute Bridge", null, "legs,glutes,bodyweight"},
            {"Step Up", null, "legs,dumbbell"},
            {"Kettlebell Swing", null, "legs,glutes,kettlebell"},
            // Core
            {"Plank", null, "core,bodyweight"},
            {"Side Plank", null, "core,bodyweight"},
            {"Crunch", null, "core,bodyweight"},
            {"Sit Up", null, "core,bodyweight"},
            {"Leg Raise", null, "core,bodyweight"},
            {"Russian Twist", null, "core,bodyweight"},
            {"Ab Rollout", null, "core,bodyweight"},
            {"Cable Crunch", null, "core,cable"},
            {"Hanging Knee Raise", null, "core,bodyweight"},
    };

    /** A fresh install starts with the same 68 exercises the old app seeded. */
    private static void seedExercisesIfNeeded(SQLiteDatabase db) {
        if (db.isReadOnly()) return;
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM exercises", null);
        try {
            if (c.moveToFirst() && c.getLong(0) > 0) return;
        } finally {
            c.close();
        }
        db.beginTransaction();
        try {
            for (String[] ex : SEED) {
                ContentValues v = new ContentValues();
                v.put("name", ex[0]);
                v.put("details", ex[1]);
                v.put("tags", ex[2]);
                db.insertOrThrow("exercises", null, v);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }
}
