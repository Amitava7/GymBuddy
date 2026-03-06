import * as SQLite from 'expo-sqlite';

let db: SQLite.SQLiteDatabase | null = null;

export async function getDb(): Promise<SQLite.SQLiteDatabase> {
  if (db) return db;
  db = await SQLite.openDatabaseAsync('gymbuddy.db');
  await initDb(db);
  return db;
}

async function initDb(database: SQLite.SQLiteDatabase) {
  await database.execAsync(`
    PRAGMA journal_mode = WAL;
    PRAGMA foreign_keys = ON;

    CREATE TABLE IF NOT EXISTS gyms (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      name TEXT NOT NULL,
      location TEXT,
      created_at TEXT DEFAULT (datetime('now'))
    );

    CREATE TABLE IF NOT EXISTS exercises (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      name TEXT NOT NULL,
      details TEXT,
      created_at TEXT DEFAULT (datetime('now'))
    );

    CREATE TABLE IF NOT EXISTS workout_templates (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      name TEXT NOT NULL,
      gym_id INTEGER NOT NULL,
      created_at TEXT DEFAULT (datetime('now')),
      FOREIGN KEY (gym_id) REFERENCES gyms(id) ON DELETE CASCADE
    );

    CREATE TABLE IF NOT EXISTS template_exercises (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      template_id INTEGER NOT NULL,
      exercise_id INTEGER NOT NULL,
      sort_order INTEGER DEFAULT 0,
      FOREIGN KEY (template_id) REFERENCES workout_templates(id) ON DELETE CASCADE,
      FOREIGN KEY (exercise_id) REFERENCES exercises(id) ON DELETE CASCADE
    );

    CREATE TABLE IF NOT EXISTS workouts (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      template_id INTEGER,
      gym_id INTEGER NOT NULL,
      name TEXT NOT NULL,
      started_at TEXT DEFAULT (datetime('now')),
      finished_at TEXT,
      duration_seconds INTEGER DEFAULT 0,
      FOREIGN KEY (template_id) REFERENCES workout_templates(id) ON DELETE SET NULL,
      FOREIGN KEY (gym_id) REFERENCES gyms(id) ON DELETE CASCADE
    );

    CREATE TABLE IF NOT EXISTS workout_exercises (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      workout_id INTEGER NOT NULL,
      exercise_id INTEGER NOT NULL,
      note TEXT,
      is_completed INTEGER DEFAULT 0,
      sort_order INTEGER DEFAULT 0,
      FOREIGN KEY (workout_id) REFERENCES workouts(id) ON DELETE CASCADE,
      FOREIGN KEY (exercise_id) REFERENCES exercises(id) ON DELETE CASCADE
    );

    CREATE TABLE IF NOT EXISTS workout_sets (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      workout_exercise_id INTEGER NOT NULL,
      set_number INTEGER NOT NULL,
      kg REAL,
      reps INTEGER,
      FOREIGN KEY (workout_exercise_id) REFERENCES workout_exercises(id) ON DELETE CASCADE
    );
  `);
}

// --- Gym operations ---

export async function getGyms() {
  const database = await getDb();
  return database.getAllAsync<{ id: number; name: string; location: string | null }>(
    'SELECT * FROM gyms ORDER BY name'
  );
}

export async function createGym(name: string, location?: string) {
  const database = await getDb();
  const result = await database.runAsync(
    'INSERT INTO gyms (name, location) VALUES (?, ?)',
    name,
    location || null
  );
  return result.lastInsertRowId;
}

export async function deleteGym(id: number) {
  const database = await getDb();
  await database.runAsync('DELETE FROM gyms WHERE id = ?', id);
}

// --- Exercise operations ---

export async function getExercises(search?: string) {
  const database = await getDb();
  if (search) {
    return database.getAllAsync<{ id: number; name: string; details: string | null }>(
      'SELECT * FROM exercises WHERE name LIKE ? ORDER BY name',
      `%${search}%`
    );
  }
  return database.getAllAsync<{ id: number; name: string; details: string | null }>(
    'SELECT * FROM exercises ORDER BY name'
  );
}

export async function getExercise(id: number) {
  const database = await getDb();
  return database.getFirstAsync<{ id: number; name: string; details: string | null }>(
    'SELECT * FROM exercises WHERE id = ?',
    id
  );
}

export async function createExercise(name: string, details?: string) {
  const database = await getDb();
  const result = await database.runAsync(
    'INSERT INTO exercises (name, details) VALUES (?, ?)',
    name,
    details || null
  );
  return result.lastInsertRowId;
}

export async function updateExercise(id: number, name: string, details?: string) {
  const database = await getDb();
  await database.runAsync(
    'UPDATE exercises SET name = ?, details = ? WHERE id = ?',
    name,
    details || null,
    id
  );
}

export async function deleteExercise(id: number) {
  const database = await getDb();
  await database.runAsync('DELETE FROM exercises WHERE id = ?', id);
}

// --- Workout Template operations ---

export async function getWorkoutTemplates(gymId: number) {
  const database = await getDb();
  return database.getAllAsync<{ id: number; name: string; gym_id: number; created_at: string }>(
    'SELECT * FROM workout_templates WHERE gym_id = ? ORDER BY created_at DESC',
    gymId
  );
}

export async function createWorkoutTemplate(name: string, gymId: number) {
  const database = await getDb();
  const result = await database.runAsync(
    'INSERT INTO workout_templates (name, gym_id) VALUES (?, ?)',
    name,
    gymId
  );
  return result.lastInsertRowId;
}

export async function addTemplateExercise(templateId: number, exerciseId: number, sortOrder: number) {
  const database = await getDb();
  await database.runAsync(
    'INSERT INTO template_exercises (template_id, exercise_id, sort_order) VALUES (?, ?, ?)',
    templateId,
    exerciseId,
    sortOrder
  );
}

export async function getTemplateExercises(templateId: number) {
  const database = await getDb();
  return database.getAllAsync<{
    id: number;
    template_id: number;
    exercise_id: number;
    sort_order: number;
    name: string;
    details: string | null;
  }>(
    `SELECT te.*, e.name, e.details FROM template_exercises te
     JOIN exercises e ON te.exercise_id = e.id
     WHERE te.template_id = ?
     ORDER BY te.sort_order`,
    templateId
  );
}

export async function deleteWorkoutTemplate(id: number) {
  const database = await getDb();
  await database.runAsync('DELETE FROM workout_templates WHERE id = ?', id);
}

// --- Workout operations ---

export async function startWorkout(name: string, gymId: number, templateId?: number) {
  const database = await getDb();
  const result = await database.runAsync(
    'INSERT INTO workouts (name, gym_id, template_id) VALUES (?, ?, ?)',
    name,
    gymId,
    templateId || null
  );
  return result.lastInsertRowId;
}

export async function finishWorkout(workoutId: number) {
  const database = await getDb();
  await database.runAsync(
    `UPDATE workouts SET finished_at = datetime('now'),
     duration_seconds = CAST((julianday(datetime('now')) - julianday(started_at)) * 86400 AS INTEGER)
     WHERE id = ?`,
    workoutId
  );
}

export async function getWorkout(id: number) {
  const database = await getDb();
  return database.getFirstAsync<{
    id: number;
    template_id: number | null;
    gym_id: number;
    name: string;
    started_at: string;
    finished_at: string | null;
    duration_seconds: number;
  }>('SELECT * FROM workouts WHERE id = ?', id);
}

export async function getWorkoutHistory(gymId?: number) {
  const database = await getDb();
  if (gymId) {
    return database.getAllAsync<{
      id: number;
      name: string;
      gym_id: number;
      gym_name: string;
      started_at: string;
      finished_at: string | null;
      duration_seconds: number;
      exercise_count: number;
      total_sets: number;
    }>(
      `SELECT w.*, g.name as gym_name,
       (SELECT COUNT(*) FROM workout_exercises we WHERE we.workout_id = w.id) as exercise_count,
       (SELECT COUNT(*) FROM workout_sets ws JOIN workout_exercises we ON ws.workout_exercise_id = we.id WHERE we.workout_id = w.id) as total_sets
       FROM workouts w JOIN gyms g ON w.gym_id = g.id
       WHERE w.finished_at IS NOT NULL AND w.gym_id = ?
       ORDER BY w.started_at DESC`,
      gymId
    );
  }
  return database.getAllAsync<{
    id: number;
    name: string;
    gym_id: number;
    gym_name: string;
    started_at: string;
    finished_at: string | null;
    duration_seconds: number;
    exercise_count: number;
    total_sets: number;
  }>(
    `SELECT w.*, g.name as gym_name,
     (SELECT COUNT(*) FROM workout_exercises we WHERE we.workout_id = w.id) as exercise_count,
     (SELECT COUNT(*) FROM workout_sets ws JOIN workout_exercises we ON ws.workout_exercise_id = we.id WHERE we.workout_id = w.id) as total_sets
     FROM workouts w JOIN gyms g ON w.gym_id = g.id
     WHERE w.finished_at IS NOT NULL
     ORDER BY w.started_at DESC`
  );
}

// --- Workout Exercise operations ---

export async function addWorkoutExercise(workoutId: number, exerciseId: number, sortOrder: number) {
  const database = await getDb();
  const result = await database.runAsync(
    'INSERT INTO workout_exercises (workout_id, exercise_id, sort_order) VALUES (?, ?, ?)',
    workoutId,
    exerciseId,
    sortOrder
  );
  return result.lastInsertRowId;
}

export async function getWorkoutExercises(workoutId: number) {
  const database = await getDb();
  return database.getAllAsync<{
    id: number;
    workout_id: number;
    exercise_id: number;
    note: string | null;
    is_completed: number;
    sort_order: number;
    name: string;
    details: string | null;
  }>(
    `SELECT we.*, e.name, e.details FROM workout_exercises we
     JOIN exercises e ON we.exercise_id = e.id
     WHERE we.workout_id = ?
     ORDER BY we.sort_order`,
    workoutId
  );
}

export async function toggleWorkoutExercise(id: number, completed: boolean) {
  const database = await getDb();
  await database.runAsync(
    'UPDATE workout_exercises SET is_completed = ? WHERE id = ?',
    completed ? 1 : 0,
    id
  );
}

export async function updateWorkoutExerciseNote(id: number, note: string) {
  const database = await getDb();
  await database.runAsync(
    'UPDATE workout_exercises SET note = ? WHERE id = ?',
    note,
    id
  );
}

// --- Workout Set operations ---

export async function addWorkoutSet(workoutExerciseId: number, setNumber: number, kg?: number, reps?: number) {
  const database = await getDb();
  const result = await database.runAsync(
    'INSERT INTO workout_sets (workout_exercise_id, set_number, kg, reps) VALUES (?, ?, ?, ?)',
    workoutExerciseId,
    setNumber,
    kg ?? null,
    reps ?? null
  );
  return result.lastInsertRowId;
}

export async function updateWorkoutSet(id: number, kg?: number, reps?: number) {
  const database = await getDb();
  await database.runAsync(
    'UPDATE workout_sets SET kg = ?, reps = ? WHERE id = ?',
    kg ?? null,
    reps ?? null,
    id
  );
}

export async function deleteWorkoutSet(id: number) {
  const database = await getDb();
  await database.runAsync('DELETE FROM workout_sets WHERE id = ?', id);
}

export async function getWorkoutSets(workoutExerciseId: number) {
  const database = await getDb();
  return database.getAllAsync<{
    id: number;
    workout_exercise_id: number;
    set_number: number;
    kg: number | null;
    reps: number | null;
  }>(
    'SELECT * FROM workout_sets WHERE workout_exercise_id = ? ORDER BY set_number',
    workoutExerciseId
  );
}

// --- Exercise history / trends ---

export async function getExerciseHistory(exerciseId: number) {
  const database = await getDb();
  return database.getAllAsync<{
    workout_date: string;
    workout_name: string;
    max_kg: number;
    max_reps: number;
    total_sets: number;
    total_volume: number;
  }>(
    `SELECT
       w.started_at as workout_date,
       w.name as workout_name,
       MAX(ws.kg) as max_kg,
       MAX(ws.reps) as max_reps,
       COUNT(ws.id) as total_sets,
       SUM(COALESCE(ws.kg, 0) * COALESCE(ws.reps, 0)) as total_volume
     FROM workout_sets ws
     JOIN workout_exercises we ON ws.workout_exercise_id = we.id
     JOIN workouts w ON we.workout_id = w.id
     WHERE we.exercise_id = ? AND w.finished_at IS NOT NULL
     GROUP BY w.id
     ORDER BY w.started_at DESC`,
    exerciseId
  );
}

export async function getExerciseRecords(exerciseId: number) {
  const database = await getDb();
  return database.getFirstAsync<{
    max_kg: number | null;
    max_reps: number | null;
    max_volume: number | null;
  }>(
    `SELECT
       MAX(ws.kg) as max_kg,
       MAX(ws.reps) as max_reps,
       MAX(COALESCE(ws.kg, 0) * COALESCE(ws.reps, 0)) as max_volume
     FROM workout_sets ws
     JOIN workout_exercises we ON ws.workout_exercise_id = we.id
     JOIN workouts w ON we.workout_id = w.id
     WHERE we.exercise_id = ? AND w.finished_at IS NOT NULL`,
    exerciseId
  );
}
