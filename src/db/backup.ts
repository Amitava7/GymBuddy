import { File, Paths } from 'expo-file-system';
import * as Sharing from 'expo-sharing';
import * as DocumentPicker from 'expo-document-picker';
import { db } from './database';
import {
  gyms,
  exercises,
  workoutTemplates,
  templateExercises,
  workouts,
  workoutExercises,
  workoutSets,
} from './schema';

// Bump when the backup shape changes; older versions must stay importable.
export const BACKUP_VERSION = 1;
const BACKUP_APP = 'gymbuddy';

type Gym = typeof gyms.$inferSelect;
type Exercise = typeof exercises.$inferSelect;
type WorkoutTemplate = typeof workoutTemplates.$inferSelect;
type TemplateExercise = typeof templateExercises.$inferSelect;
type Workout = typeof workouts.$inferSelect;
type WorkoutExercise = typeof workoutExercises.$inferSelect;
type WorkoutSet = typeof workoutSets.$inferSelect;

export type BackupData = {
  app: typeof BACKUP_APP;
  version: number;
  exported_at: string;
  data: {
    gyms: Gym[];
    exercises: Exercise[];
    workout_templates: WorkoutTemplate[];
    template_exercises: TemplateExercise[];
    workouts: Workout[];
    workout_exercises: WorkoutExercise[];
    workout_sets: WorkoutSet[];
  };
};

export type ImportMode = 'replace' | 'merge';

export type ImportSummary = {
  gyms: number;
  exercises: number;
  templates: number;
  workouts: number;
  sets: number;
};

// --- Export ---

export function buildBackup(): BackupData {
  return {
    app: BACKUP_APP,
    version: BACKUP_VERSION,
    exported_at: new Date().toISOString(),
    data: {
      gyms: db.select().from(gyms).all(),
      exercises: db.select().from(exercises).all(),
      workout_templates: db.select().from(workoutTemplates).all(),
      template_exercises: db.select().from(templateExercises).all(),
      workouts: db.select().from(workouts).all(),
      workout_exercises: db.select().from(workoutExercises).all(),
      workout_sets: db.select().from(workoutSets).all(),
    },
  };
}

export async function exportBackup() {
  const backup = buildBackup();
  const stamp = backup.exported_at.slice(0, 19).replace(/[:T]/g, '-');
  const file = new File(Paths.cache, `gymbuddy-backup-${stamp}.json`);
  if (file.exists) file.delete();
  file.create();
  file.write(JSON.stringify(backup, null, 2));

  if (!(await Sharing.isAvailableAsync())) {
    throw new Error('Sharing is not available on this device.');
  }
  await Sharing.shareAsync(file.uri, {
    mimeType: 'application/json',
    UTI: 'public.json',
    dialogTitle: 'Export GymBuddy data',
  });
}

// --- Import ---

/** Opens the document picker and returns the parsed backup, or null if cancelled. */
export async function pickBackupFile(): Promise<BackupData | null> {
  const result = await DocumentPicker.getDocumentAsync({
    type: ['application/json', 'text/plain', '*/*'],
    copyToCacheDirectory: true,
  });
  if (result.canceled || !result.assets?.length) return null;

  const text = await new File(result.assets[0].uri).text();
  let parsed: unknown;
  try {
    parsed = JSON.parse(text);
  } catch {
    throw new Error('The selected file is not valid JSON.');
  }
  return validateBackup(parsed);
}

function isArray(value: unknown): value is unknown[] {
  return Array.isArray(value);
}

export function validateBackup(input: unknown): BackupData {
  const obj = input as Partial<BackupData> | null;
  if (!obj || typeof obj !== 'object' || obj.app !== BACKUP_APP || !obj.data) {
    throw new Error('This file is not a GymBuddy backup.');
  }
  if (typeof obj.version !== 'number' || obj.version > BACKUP_VERSION) {
    throw new Error('This backup was created by a newer version of GymBuddy. Please update the app.');
  }
  const d = obj.data;
  const tables = [
    'gyms',
    'exercises',
    'workout_templates',
    'template_exercises',
    'workouts',
    'workout_exercises',
    'workout_sets',
  ] as const;
  for (const table of tables) {
    if (!isArray(d[table])) throw new Error(`Backup is missing the "${table}" table.`);
  }
  return obj as BackupData;
}

export function summarizeBackup(backup: BackupData): ImportSummary {
  const d = backup.data;
  return {
    gyms: d.gyms.length,
    exercises: d.exercises.length,
    templates: d.workout_templates.length,
    workouts: d.workouts.length,
    sets: d.workout_sets.length,
  };
}

/**
 * Restores a backup.
 * - `replace`: wipes all existing data and restores the backup exactly (original ids kept).
 * - `merge`: keeps existing data and adds the backup on top. Gyms, exercises and templates
 *   are matched by name so they are not duplicated, and workouts already present (same gym,
 *   name and start time) are skipped, so importing the same file twice is safe.
 */
export function importBackup(backup: BackupData, mode: ImportMode): ImportSummary {
  return mode === 'replace' ? replaceAll(backup) : mergeInto(backup);
}

function replaceAll(backup: BackupData): ImportSummary {
  const d = backup.data;
  db.transaction((tx) => {
    // Children first so foreign keys stay valid.
    tx.delete(workoutSets).run();
    tx.delete(workoutExercises).run();
    tx.delete(workouts).run();
    tx.delete(templateExercises).run();
    tx.delete(workoutTemplates).run();
    tx.delete(exercises).run();
    tx.delete(gyms).run();

    for (const row of d.gyms) tx.insert(gyms).values(row).run();
    for (const row of d.exercises) tx.insert(exercises).values(row).run();
    for (const row of d.workout_templates) tx.insert(workoutTemplates).values(row).run();
    for (const row of d.template_exercises) tx.insert(templateExercises).values(row).run();
    for (const row of d.workouts) tx.insert(workouts).values(row).run();
    for (const row of d.workout_exercises) tx.insert(workoutExercises).values(row).run();
    for (const row of d.workout_sets) tx.insert(workoutSets).values(row).run();
  });
  return summarizeBackup(backup);
}

const norm = (s: string | null | undefined) => (s ?? '').trim().toLowerCase();

function mergeInto(backup: BackupData): ImportSummary {
  const d = backup.data;
  const summary: ImportSummary = { gyms: 0, exercises: 0, templates: 0, workouts: 0, sets: 0 };

  db.transaction((tx) => {
    // Backup id -> local id
    const gymMap = new Map<number, number>();
    const exerciseMap = new Map<number, number>();
    const templateMap = new Map<number, number>();
    const workoutMap = new Map<number, number>();
    const workoutExerciseMap = new Map<number, number>();

    const existingGyms = new Map(tx.select().from(gyms).all().map((g) => [norm(g.name), g.id]));
    for (const g of d.gyms) {
      let id = existingGyms.get(norm(g.name));
      if (id === undefined) {
        id = tx
          .insert(gyms)
          .values({ name: g.name, location: g.location, created_at: g.created_at })
          .returning({ id: gyms.id })
          .get().id;
        existingGyms.set(norm(g.name), id);
        summary.gyms++;
      }
      gymMap.set(g.id, id);
    }

    const existingExercises = new Map(
      tx.select().from(exercises).all().map((e) => [norm(e.name), e.id])
    );
    for (const e of d.exercises) {
      let id = existingExercises.get(norm(e.name));
      if (id === undefined) {
        id = tx
          .insert(exercises)
          .values({ name: e.name, details: e.details, tags: e.tags, created_at: e.created_at })
          .returning({ id: exercises.id })
          .get().id;
        existingExercises.set(norm(e.name), id);
        summary.exercises++;
      }
      exerciseMap.set(e.id, id);
    }

    const templateKey = (gymId: number, name: string) => `${gymId}|${norm(name)}`;
    const existingTemplates = new Map(
      tx.select().from(workoutTemplates).all().map((t) => [templateKey(t.gym_id, t.name), t.id])
    );
    const newTemplates = new Set<number>();
    for (const t of d.workout_templates) {
      const gymId = gymMap.get(t.gym_id);
      if (gymId === undefined) continue;
      const key = templateKey(gymId, t.name);
      let id = existingTemplates.get(key);
      if (id === undefined) {
        id = tx
          .insert(workoutTemplates)
          .values({ name: t.name, gym_id: gymId, created_at: t.created_at })
          .returning({ id: workoutTemplates.id })
          .get().id;
        existingTemplates.set(key, id);
        newTemplates.add(id);
        summary.templates++;
      }
      templateMap.set(t.id, id);
    }

    // Only fill exercises for templates we just created; existing templates keep their own list.
    for (const te of d.template_exercises) {
      const templateId = templateMap.get(te.template_id);
      const exerciseId = exerciseMap.get(te.exercise_id);
      if (templateId === undefined || exerciseId === undefined || !newTemplates.has(templateId)) continue;
      tx.insert(templateExercises)
        .values({ template_id: templateId, exercise_id: exerciseId, sort_order: te.sort_order })
        .run();
    }

    const workoutKey = (gymId: number, name: string, startedAt: string | null) =>
      `${gymId}|${norm(name)}|${startedAt ?? ''}`;
    const existingWorkouts = new Set(
      tx.select().from(workouts).all().map((w) => workoutKey(w.gym_id, w.name, w.started_at))
    );
    for (const w of d.workouts) {
      const gymId = gymMap.get(w.gym_id);
      if (gymId === undefined) continue;
      const key = workoutKey(gymId, w.name, w.started_at);
      if (existingWorkouts.has(key)) continue;
      const id = tx
        .insert(workouts)
        .values({
          name: w.name,
          gym_id: gymId,
          template_id: w.template_id != null ? templateMap.get(w.template_id) ?? null : null,
          started_at: w.started_at,
          finished_at: w.finished_at,
          duration_seconds: w.duration_seconds,
        })
        .returning({ id: workouts.id })
        .get().id;
      existingWorkouts.add(key);
      workoutMap.set(w.id, id);
      summary.workouts++;
    }

    for (const we of d.workout_exercises) {
      const workoutId = workoutMap.get(we.workout_id);
      const exerciseId = exerciseMap.get(we.exercise_id);
      if (workoutId === undefined || exerciseId === undefined) continue;
      const id = tx
        .insert(workoutExercises)
        .values({
          workout_id: workoutId,
          exercise_id: exerciseId,
          note: we.note,
          is_completed: we.is_completed,
          sort_order: we.sort_order,
        })
        .returning({ id: workoutExercises.id })
        .get().id;
      workoutExerciseMap.set(we.id, id);
    }

    for (const s of d.workout_sets) {
      const workoutExerciseId = workoutExerciseMap.get(s.workout_exercise_id);
      if (workoutExerciseId === undefined) continue;
      tx.insert(workoutSets)
        .values({
          workout_exercise_id: workoutExerciseId,
          set_number: s.set_number,
          kg: s.kg,
          reps: s.reps,
        })
        .run();
      summary.sets++;
    }
  });

  return summary;
}
