import { drizzle } from 'drizzle-orm/expo-sqlite';
import { openDatabaseSync } from 'expo-sqlite';
import { eq, like, desc, sql, and, isNotNull, count } from 'drizzle-orm';
import {
  gyms,
  exercises,
  workoutTemplates,
  templateExercises,
  workouts,
  workoutExercises,
  workoutSets,
} from './schema';

// --- Database connection ---

const expoDb = openDatabaseSync('gymbuddy.db');
expoDb.execSync('PRAGMA journal_mode = WAL;');
expoDb.execSync('PRAGMA foreign_keys = ON;');

export const db = drizzle(expoDb);

// --- Gyms ---

export async function getGyms() {
  return db.select().from(gyms).orderBy(gyms.name).all();
}

export async function createGym(name: string, location?: string) {
  const inserted = db
    .insert(gyms)
    .values({ name, location: location || null })
    .returning({ id: gyms.id })
    .get();
  return inserted.id;
}

export async function deleteGym(id: number) {
  db.delete(gyms).where(eq(gyms.id, id)).run();
}

// --- Exercises ---

export const MUSCLE_TAGS = ['chest', 'back', 'shoulders', 'arms', 'legs', 'core', 'glutes'] as const;
export const EQUIPMENT_TAGS = ['barbell', 'dumbbell', 'machine', 'cable', 'bodyweight', 'kettlebell'] as const;

export async function getExercises(search?: string, tags?: string[]) {
  const conditions = [];
  if (search) conditions.push(like(exercises.name, `%${search}%`));
  if (tags && tags.length > 0) {
    for (const tag of tags) conditions.push(like(exercises.tags, `%${tag}%`));
  }
  const query = db.select().from(exercises);
  if (conditions.length > 0) {
    return query.where(and(...conditions)).orderBy(exercises.name).all();
  }
  return query.orderBy(exercises.name).all();
}

export async function getExercise(id: number) {
  return db.select().from(exercises).where(eq(exercises.id, id)).get() ?? null;
}

export async function createExercise(name: string, details?: string, tags?: string) {
  const inserted = db
    .insert(exercises)
    .values({ name, details: details || null, tags: tags || null })
    .returning({ id: exercises.id })
    .get();
  return inserted.id;
}

export async function updateExercise(id: number, name: string, details?: string, tags?: string) {
  db.update(exercises)
    .set({ name, details: details || null, tags: tags || null })
    .where(eq(exercises.id, id))
    .run();
}

export async function deleteExercise(id: number) {
  db.delete(exercises).where(eq(exercises.id, id)).run();
}

// --- Workout Templates ---

export async function getWorkoutTemplates(gymId: number) {
  return db
    .select()
    .from(workoutTemplates)
    .where(eq(workoutTemplates.gym_id, gymId))
    .orderBy(desc(workoutTemplates.created_at))
    .all();
}

export async function createWorkoutTemplate(name: string, gymId: number) {
  const inserted = db
    .insert(workoutTemplates)
    .values({ name, gym_id: gymId })
    .returning({ id: workoutTemplates.id })
    .get();
  return inserted.id;
}

export async function addTemplateExercise(templateId: number, exerciseId: number, sortOrder: number) {
  db.insert(templateExercises)
    .values({
      template_id: templateId,
      exercise_id: exerciseId,
      sort_order: sortOrder,
    })
    .run();
}

export async function getTemplateExercises(templateId: number) {
  return db
    .select({
      id: templateExercises.id,
      template_id: templateExercises.template_id,
      exercise_id: templateExercises.exercise_id,
      sort_order: templateExercises.sort_order,
      name: exercises.name,
      details: exercises.details,
    })
    .from(templateExercises)
    .innerJoin(exercises, eq(templateExercises.exercise_id, exercises.id))
    .where(eq(templateExercises.template_id, templateId))
    .orderBy(templateExercises.sort_order)
    .all();
}

export async function deleteWorkoutTemplate(id: number) {
  db.delete(workoutTemplates).where(eq(workoutTemplates.id, id)).run();
}

// --- Workouts ---

export async function startWorkout(name: string, gymId: number, templateId?: number) {
  const inserted = db
    .insert(workouts)
    .values({ name, gym_id: gymId, template_id: templateId || null })
    .returning({ id: workouts.id })
    .get();
  return inserted.id;
}

export async function finishWorkout(workoutId: number) {
  db.update(workouts)
    .set({
      finished_at: sql`datetime('now')`,
      duration_seconds: sql<number>`CAST((julianday(datetime('now')) - julianday(${workouts.started_at})) * 86400 AS INTEGER)`,
    })
    .where(eq(workouts.id, workoutId))
    .run();
}

export async function setWorkoutTemplate(workoutId: number, templateId: number) {
  db.update(workouts)
    .set({ template_id: templateId })
    .where(eq(workouts.id, workoutId))
    .run();
}

export async function deleteWorkout(id: number) {
  db.delete(workouts).where(eq(workouts.id, id)).run();
}

export async function getWorkout(id: number) {
  return db.select().from(workouts).where(eq(workouts.id, id)).get() ?? null;
}

export async function getWorkoutHistory(gymId?: number) {
  const baseQuery = db
    .select({
      id: workouts.id,
      name: workouts.name,
      gym_id: workouts.gym_id,
      gym_name: gyms.name,
      started_at: workouts.started_at,
      finished_at: workouts.finished_at,
      duration_seconds: workouts.duration_seconds,
      exercise_count: sql<number>`(SELECT COUNT(*) FROM workout_exercises we WHERE we.workout_id = ${workouts.id})`,
      total_sets: sql<number>`(SELECT COUNT(*) FROM workout_sets ws JOIN workout_exercises we ON ws.workout_exercise_id = we.id WHERE we.workout_id = ${workouts.id})`,
    })
    .from(workouts)
    .innerJoin(gyms, eq(workouts.gym_id, gyms.id));

  if (gymId) {
    return baseQuery
      .where(and(isNotNull(workouts.finished_at), eq(workouts.gym_id, gymId)))
      .orderBy(desc(workouts.started_at))
      .all();
  }

  return baseQuery
    .where(isNotNull(workouts.finished_at))
    .orderBy(desc(workouts.started_at))
    .all();
}

// --- Workout Exercises ---

export async function addWorkoutExercise(workoutId: number, exerciseId: number, sortOrder: number) {
  const inserted = db
    .insert(workoutExercises)
    .values({ workout_id: workoutId, exercise_id: exerciseId, sort_order: sortOrder })
    .returning({ id: workoutExercises.id })
    .get();
  return inserted.id;
}

export async function getWorkoutExercises(workoutId: number) {
  return db
    .select({
      id: workoutExercises.id,
      workout_id: workoutExercises.workout_id,
      exercise_id: workoutExercises.exercise_id,
      note: workoutExercises.note,
      is_completed: workoutExercises.is_completed,
      sort_order: workoutExercises.sort_order,
      name: exercises.name,
      details: exercises.details,
    })
    .from(workoutExercises)
    .innerJoin(exercises, eq(workoutExercises.exercise_id, exercises.id))
    .where(eq(workoutExercises.workout_id, workoutId))
    .orderBy(workoutExercises.sort_order)
    .all();
}

export async function toggleWorkoutExercise(id: number, completed: boolean) {
  db.update(workoutExercises)
    .set({ is_completed: completed ? 1 : 0 })
    .where(eq(workoutExercises.id, id))
    .run();
}

export async function updateWorkoutExerciseNote(id: number, note: string) {
  db.update(workoutExercises)
    .set({ note })
    .where(eq(workoutExercises.id, id))
    .run();
}

export async function deleteWorkoutExercise(id: number) {
  db.delete(workoutSets).where(eq(workoutSets.workout_exercise_id, id)).run();
  db.delete(workoutExercises).where(eq(workoutExercises.id, id)).run();
}

// --- Workout Sets ---

export async function addWorkoutSet(workoutExerciseId: number, setNumber: number, kg?: number, reps?: number) {
  const inserted = db
    .insert(workoutSets)
    .values({
      workout_exercise_id: workoutExerciseId,
      set_number: setNumber,
      kg: kg ?? null,
      reps: reps ?? null,
    })
    .returning({ id: workoutSets.id })
    .get();
  return inserted.id;
}

export async function updateWorkoutSet(id: number, kg?: number, reps?: number) {
  db.update(workoutSets)
    .set({ kg: kg ?? null, reps: reps ?? null })
    .where(eq(workoutSets.id, id))
    .run();
}

export async function deleteWorkoutSet(id: number) {
  db.delete(workoutSets).where(eq(workoutSets.id, id)).run();
}

export async function getWorkoutSets(workoutExerciseId: number) {
  return db
    .select()
    .from(workoutSets)
    .where(eq(workoutSets.workout_exercise_id, workoutExerciseId))
    .orderBy(workoutSets.set_number)
    .all();
}

// --- Exercise History & Records ---

export async function getLastWorkoutDataForExercise(exerciseId: number, templateId?: number | null) {
  let lastExercise: { id: number; note: string | null } | undefined;

  if (templateId) {
    lastExercise = db
      .select({
        id: workoutExercises.id,
        note: workoutExercises.note,
      })
      .from(workoutExercises)
      .innerJoin(workouts, eq(workoutExercises.workout_id, workouts.id))
      .where(
        and(
          eq(workoutExercises.exercise_id, exerciseId),
          isNotNull(workouts.finished_at),
          eq(workouts.template_id, templateId),
        )
      )
      .orderBy(desc(workouts.started_at))
      .limit(1)
      .get();
  }

  if (!lastExercise && !templateId) {
    lastExercise = db
      .select({
        id: workoutExercises.id,
        note: workoutExercises.note,
      })
      .from(workoutExercises)
      .innerJoin(workouts, eq(workoutExercises.workout_id, workouts.id))
      .where(
        and(
          eq(workoutExercises.exercise_id, exerciseId),
          isNotNull(workouts.finished_at),
        )
      )
      .orderBy(desc(workouts.started_at))
      .limit(1)
      .get();
  }

  if (!lastExercise) return null;

  const sets = db
    .select({
      set_number: workoutSets.set_number,
      kg: workoutSets.kg,
      reps: workoutSets.reps,
    })
    .from(workoutSets)
    .where(eq(workoutSets.workout_exercise_id, lastExercise.id))
    .orderBy(workoutSets.set_number)
    .all();

  return { note: lastExercise.note, sets };
}

export async function getExerciseHistory(exerciseId: number) {
  return db
    .select({
      workout_date: workouts.started_at,
      workout_name: workouts.name,
      max_kg: sql<number>`MAX(${workoutSets.kg})`,
      max_reps: sql<number>`MAX(${workoutSets.reps})`,
      total_sets: count(workoutSets.id),
      total_volume: sql<number>`SUM(COALESCE(${workoutSets.kg}, 0) * COALESCE(${workoutSets.reps}, 0))`,
    })
    .from(workoutSets)
    .innerJoin(workoutExercises, eq(workoutSets.workout_exercise_id, workoutExercises.id))
    .innerJoin(workouts, eq(workoutExercises.workout_id, workouts.id))
    .where(and(eq(workoutExercises.exercise_id, exerciseId), isNotNull(workouts.finished_at)))
    .groupBy(workouts.id)
    .orderBy(desc(workouts.started_at))
    .all();
}

export async function getExerciseRecords(exerciseId: number) {
  return db
    .select({
      max_kg: sql<number | null>`MAX(${workoutSets.kg})`,
      max_reps: sql<number | null>`MAX(${workoutSets.reps})`,
      max_volume: sql<number | null>`MAX(COALESCE(${workoutSets.kg}, 0) * COALESCE(${workoutSets.reps}, 0))`,
    })
    .from(workoutSets)
    .innerJoin(workoutExercises, eq(workoutSets.workout_exercise_id, workoutExercises.id))
    .innerJoin(workouts, eq(workoutExercises.workout_id, workouts.id))
    .where(and(eq(workoutExercises.exercise_id, exerciseId), isNotNull(workouts.finished_at)))
    .get() ?? null;
}
