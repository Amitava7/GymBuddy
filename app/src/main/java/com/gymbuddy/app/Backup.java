package com.gymbuddy.app;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

/**
 * Export and import of everything, in the React Native app's backup format
 * (version 1). A file exported by either app imports into the other.
 */
final class Backup {

    // Bump when the backup shape changes; older versions must stay importable.
    static final int VERSION = 1;
    static final String APP = "gymbuddy";

    static final String[] TABLES = {
            "gyms", "exercises", "workout_templates", "template_exercises",
            "workouts", "workout_exercises", "workout_sets",
    };

    /** Columns per table, in the order the old app wrote them. */
    static final String[][] COLUMNS = {
            {"id", "name", "location", "created_at"},
            {"id", "name", "details", "tags", "created_at"},
            {"id", "name", "gym_id", "created_at"},
            {"id", "template_id", "exercise_id", "sort_order"},
            {"id", "template_id", "gym_id", "name", "started_at", "finished_at", "duration_seconds"},
            {"id", "workout_id", "exercise_id", "note", "is_completed", "sort_order"},
            {"id", "workout_exercise_id", "set_number", "kg", "reps"},
    };

    private Backup() {
    }

    /** A parsed, validated backup. Rows map column to Long, Double, String, Boolean or null. */
    static final class Data {
        Object exportedAt;
        final Map<String, List<Map<String, Object>>> tables =
                new LinkedHashMap<String, List<Map<String, Object>>>();

        List<Map<String, Object>> rows(String table) {
            return tables.get(table);
        }
    }

    static final class Summary {
        int gyms, exercises, templates, workouts, sets;

        String describe() {
            return gyms + " gyms, " + exercises + " exercises, " + templates + " templates, "
                    + workouts + " workouts, " + sets + " sets";
        }
    }

    // ---------------------------------------------------------------- export

    static Map<String, Object> build(SQLiteDatabase db, Date now) {
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        for (int t = 0; t < TABLES.length; t++) {
            data.put(TABLES[t], dump(db, TABLES[t], COLUMNS[t]));
        }
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("app", APP);
        out.put("version", VERSION);
        out.put("exported_at", isoUtc(now));
        out.put("data", data);
        return out;
    }

    private static List<Object> dump(SQLiteDatabase db, String table, String[] cols) {
        List<Object> rows = new ArrayList<Object>();
        Cursor c = db.rawQuery("SELECT " + String.join(", ", cols) + " FROM " + table + " ORDER BY id", null);
        try {
            while (c.moveToNext()) {
                Map<String, Object> row = new LinkedHashMap<String, Object>();
                for (int i = 0; i < cols.length; i++) {
                    Object v;
                    switch (c.getType(i)) {
                        case Cursor.FIELD_TYPE_NULL: v = null; break;
                        case Cursor.FIELD_TYPE_INTEGER: v = c.getLong(i); break;
                        case Cursor.FIELD_TYPE_FLOAT: v = c.getDouble(i); break;
                        default: v = c.getString(i);
                    }
                    row.put(cols[i], v);
                }
                rows.add(row);
            }
        } finally {
            c.close();
        }
        return rows;
    }

    static String isoUtc(Date d) {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        return f.format(d);
    }

    /** gymbuddy-backup-2026-10-15-22-54-35.json, as the old app named it. */
    static String fileName(String exportedAt) {
        return "gymbuddy-backup-" + exportedAt.substring(0, 19).replace(':', '-').replace('T', '-') + ".json";
    }

    // ---------------------------------------------------------------- import

    static Data parse(String text) {
        if (text.startsWith("﻿")) text = text.substring(1);
        Object root;
        try {
            root = new JSONTokener(text).nextValue();
        } catch (JSONException e) {
            throw new IllegalArgumentException("The selected file is not valid JSON.");
        }
        return validate(root);
    }

    private static Data validate(Object root) {
        if (!(root instanceof JSONObject)) throw notABackup();
        JSONObject o = (JSONObject) root;
        Object data = o.opt("data");
        if (!APP.equals(o.opt("app")) || !truthy(data)) throw notABackup();
        Object version = o.opt("version");
        if (!(version instanceof Number) || ((Number) version).doubleValue() > VERSION) {
            throw new IllegalArgumentException(
                    "This backup was created by a newer version of GymBuddy. Please update the app.");
        }
        Data out = new Data();
        out.exportedAt = plain(o.opt("exported_at"));
        for (String table : TABLES) {
            Object arr = data instanceof JSONObject ? ((JSONObject) data).opt(table) : null;
            if (!(arr instanceof JSONArray)) {
                throw new IllegalArgumentException("Backup is missing the \"" + table + "\" table.");
            }
            JSONArray a = (JSONArray) arr;
            List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>(a.length());
            for (int i = 0; i < a.length(); i++) {
                Object item = a.opt(i);
                if (!(item instanceof JSONObject)) {
                    throw new IllegalArgumentException("Backup has a broken row in the \"" + table + "\" table.");
                }
                JSONObject jo = (JSONObject) item;
                Map<String, Object> row = new HashMap<String, Object>();
                for (Iterator<String> it = jo.keys(); it.hasNext(); ) {
                    String k = it.next();
                    row.put(k, plain(jo.opt(k)));
                }
                rows.add(row);
            }
            out.tables.put(table, rows);
        }
        return out;
    }

    private static IllegalArgumentException notABackup() {
        return new IllegalArgumentException("This file is not a GymBuddy backup.");
    }

    private static boolean truthy(Object v) {
        if (v == null || v == JSONObject.NULL) return false;
        if (v instanceof Boolean) return (Boolean) v;
        if (v instanceof Number) return ((Number) v).doubleValue() != 0;
        if (v instanceof String) return !((String) v).isEmpty();
        return true;
    }

    /** org.json values to what goes in a column. Whole numbers become Long. */
    private static Object plain(Object v) {
        if (v == null || v == JSONObject.NULL) return null;
        if (v instanceof Integer || v instanceof Long) return ((Number) v).longValue();
        if (v instanceof Number) {
            double d = ((Number) v).doubleValue();
            if (d == Math.rint(d) && Math.abs(d) < 9007199254740992d) return (long) d;
            return d;
        }
        if (v instanceof String || v instanceof Boolean) return v;
        return v.toString();
    }

    static Summary summarize(Data d) {
        Summary s = new Summary();
        s.gyms = d.rows("gyms").size();
        s.exercises = d.rows("exercises").size();
        s.templates = d.rows("workout_templates").size();
        s.workouts = d.rows("workouts").size();
        s.sets = d.rows("workout_sets").size();
        return s;
    }

    /**
     * Restores a backup.
     * - replace: wipes all existing data and restores the backup exactly (original ids kept).
     * - merge: keeps existing data and adds the backup on top. Gyms, exercises and templates
     *   are matched by name so they are not duplicated, and workouts already present (same gym,
     *   name and start time) are skipped, so importing the same file twice is safe.
     */
    static Summary importBackup(SQLiteDatabase db, Data d, boolean replace) {
        db.beginTransaction();
        try {
            Summary s = replace ? replaceAll(db, d) : mergeInto(db, d);
            db.setTransactionSuccessful();
            return s;
        } finally {
            db.endTransaction();
        }
    }

    private static Summary replaceAll(SQLiteDatabase db, Data d) {
        // Children first so foreign keys stay valid.
        for (int t = TABLES.length - 1; t >= 0; t--) db.delete(TABLES[t], null, null);
        for (int t = 0; t < TABLES.length; t++) {
            for (Map<String, Object> row : d.rows(TABLES[t])) {
                ContentValues v = new ContentValues();
                for (String col : COLUMNS[t]) copy(v, col, row, col);
                db.insertOrThrow(TABLES[t], null, v);
            }
        }
        return summarize(d);
    }

    private static Summary mergeInto(SQLiteDatabase db, Data d) {
        Summary summary = new Summary();
        // Backup id -> local id
        Map<Object, Long> gymMap = new HashMap<Object, Long>();
        Map<Object, Long> exerciseMap = new HashMap<Object, Long>();
        Map<Object, Long> templateMap = new HashMap<Object, Long>();
        Map<Object, Long> workoutMap = new HashMap<Object, Long>();
        Map<Object, Long> workoutExerciseMap = new HashMap<Object, Long>();

        Map<String, Long> existingGyms = byName(db, "SELECT id, name FROM gyms");
        for (Map<String, Object> g : d.rows("gyms")) {
            Long id = existingGyms.get(norm(g.get("name")));
            if (id == null) {
                ContentValues v = new ContentValues();
                copy(v, "name", g, "name");
                copy(v, "location", g, "location");
                copy(v, "created_at", g, "created_at");
                id = db.insertOrThrow("gyms", null, v);
                existingGyms.put(norm(g.get("name")), id);
                summary.gyms++;
            }
            gymMap.put(g.get("id"), id);
        }

        Map<String, Long> existingExercises = byName(db, "SELECT id, name FROM exercises");
        for (Map<String, Object> e : d.rows("exercises")) {
            Long id = existingExercises.get(norm(e.get("name")));
            if (id == null) {
                ContentValues v = new ContentValues();
                copy(v, "name", e, "name");
                copy(v, "details", e, "details");
                copy(v, "tags", e, "tags");
                copy(v, "created_at", e, "created_at");
                id = db.insertOrThrow("exercises", null, v);
                existingExercises.put(norm(e.get("name")), id);
                summary.exercises++;
            }
            exerciseMap.put(e.get("id"), id);
        }

        Map<String, Long> existingTemplates = new HashMap<String, Long>();
        Cursor c = db.rawQuery("SELECT id, gym_id, name FROM workout_templates", null);
        try {
            while (c.moveToNext()) {
                existingTemplates.put(c.getLong(1) + "|" + norm(c.getString(2)), c.getLong(0));
            }
        } finally {
            c.close();
        }
        Set<Long> newTemplates = new HashSet<Long>();
        for (Map<String, Object> t : d.rows("workout_templates")) {
            Long gymId = gymMap.get(t.get("gym_id"));
            if (gymId == null) continue;
            String key = gymId + "|" + norm(t.get("name"));
            Long id = existingTemplates.get(key);
            if (id == null) {
                ContentValues v = new ContentValues();
                copy(v, "name", t, "name");
                v.put("gym_id", gymId);
                copy(v, "created_at", t, "created_at");
                id = db.insertOrThrow("workout_templates", null, v);
                existingTemplates.put(key, id);
                newTemplates.add(id);
                summary.templates++;
            }
            templateMap.put(t.get("id"), id);
        }

        // Only fill exercises for templates we just created; existing templates keep their own list.
        for (Map<String, Object> te : d.rows("template_exercises")) {
            Long templateId = templateMap.get(te.get("template_id"));
            Long exerciseId = exerciseMap.get(te.get("exercise_id"));
            if (templateId == null || exerciseId == null || !newTemplates.contains(templateId)) continue;
            ContentValues v = new ContentValues();
            v.put("template_id", templateId);
            v.put("exercise_id", exerciseId);
            copy(v, "sort_order", te, "sort_order");
            db.insertOrThrow("template_exercises", null, v);
        }

        Set<String> existingWorkouts = new HashSet<String>();
        c = db.rawQuery("SELECT gym_id, name, started_at FROM workouts", null);
        try {
            while (c.moveToNext()) {
                existingWorkouts.add(c.getLong(0) + "|" + norm(c.getString(1)) + "|"
                        + (c.isNull(2) ? "" : c.getString(2)));
            }
        } finally {
            c.close();
        }
        for (Map<String, Object> w : d.rows("workouts")) {
            Long gymId = gymMap.get(w.get("gym_id"));
            if (gymId == null) continue;
            Object started = w.get("started_at");
            String key = gymId + "|" + norm(w.get("name")) + "|" + (started == null ? "" : started);
            if (existingWorkouts.contains(key)) continue;
            ContentValues v = new ContentValues();
            copy(v, "name", w, "name");
            v.put("gym_id", gymId);
            Object tid = w.get("template_id");
            Long templateId = tid == null ? null : templateMap.get(tid);
            if (templateId == null) v.putNull("template_id");
            else v.put("template_id", templateId);
            copy(v, "started_at", w, "started_at");
            copy(v, "finished_at", w, "finished_at");
            copy(v, "duration_seconds", w, "duration_seconds");
            long id = db.insertOrThrow("workouts", null, v);
            existingWorkouts.add(key);
            workoutMap.put(w.get("id"), id);
            summary.workouts++;
        }

        for (Map<String, Object> we : d.rows("workout_exercises")) {
            Long workoutId = workoutMap.get(we.get("workout_id"));
            Long exerciseId = exerciseMap.get(we.get("exercise_id"));
            if (workoutId == null || exerciseId == null) continue;
            ContentValues v = new ContentValues();
            v.put("workout_id", workoutId);
            v.put("exercise_id", exerciseId);
            copy(v, "note", we, "note");
            copy(v, "is_completed", we, "is_completed");
            copy(v, "sort_order", we, "sort_order");
            long id = db.insertOrThrow("workout_exercises", null, v);
            workoutExerciseMap.put(we.get("id"), id);
        }

        for (Map<String, Object> s : d.rows("workout_sets")) {
            Long workoutExerciseId = workoutExerciseMap.get(s.get("workout_exercise_id"));
            if (workoutExerciseId == null) continue;
            ContentValues v = new ContentValues();
            v.put("workout_exercise_id", workoutExerciseId);
            copy(v, "set_number", s, "set_number");
            copy(v, "kg", s, "kg");
            copy(v, "reps", s, "reps");
            db.insertOrThrow("workout_sets", null, v);
            summary.sets++;
        }
        return summary;
    }

    private static Map<String, Long> byName(SQLiteDatabase db, String sql) {
        Map<String, Long> out = new HashMap<String, Long>();
        Cursor c = db.rawQuery(sql, null);
        try {
            while (c.moveToNext()) out.put(norm(c.getString(1)), c.getLong(0));
        } finally {
            c.close();
        }
        return out;
    }

    static String norm(Object s) {
        return s == null ? "" : String.valueOf(s).strip().toLowerCase(Locale.ROOT);
    }

    /**
     * Copies a backup field into a row. A field missing from the backup is left
     * out so the column default applies; a field that is there but null is
     * written as NULL. That is how drizzle treated undefined and null.
     */
    private static void copy(ContentValues v, String col, Map<String, Object> row, String key) {
        if (!row.containsKey(key)) return;
        Object x = row.get(key);
        if (x == null) v.putNull(col);
        else if (x instanceof Long) v.put(col, (Long) x);
        else if (x instanceof Double) v.put(col, (Double) x);
        else if (x instanceof Boolean) v.put(col, ((Boolean) x) ? 1L : 0L);
        else v.put(col, x.toString());
    }
}
