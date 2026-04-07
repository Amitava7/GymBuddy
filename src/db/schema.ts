import { sqliteTable, text, integer, real } from 'drizzle-orm/sqlite-core';
import { sql } from 'drizzle-orm';

export const gyms = sqliteTable('gyms', {
  id: integer('id').primaryKey({ autoIncrement: true }),
  name: text('name').notNull(),
  location: text('location'),
  created_at: text('created_at').default(sql`(datetime('now'))`),
});

export const exercises = sqliteTable('exercises', {
  id: integer('id').primaryKey({ autoIncrement: true }),
  name: text('name').notNull(),
  details: text('details'),
  tags: text('tags'),
  created_at: text('created_at').default(sql`(datetime('now'))`),
});

export const workoutTemplates = sqliteTable('workout_templates', {
  id: integer('id').primaryKey({ autoIncrement: true }),
  name: text('name').notNull(),
  gym_id: integer('gym_id').notNull().references(() => gyms.id, { onDelete: 'cascade' }),
  created_at: text('created_at').default(sql`(datetime('now'))`),
});

export const templateExercises = sqliteTable('template_exercises', {
  id: integer('id').primaryKey({ autoIncrement: true }),
  template_id: integer('template_id').notNull().references(() => workoutTemplates.id, { onDelete: 'cascade' }),
  exercise_id: integer('exercise_id').notNull().references(() => exercises.id, { onDelete: 'cascade' }),
  sort_order: integer('sort_order').notNull().default(0),
});

export const workouts = sqliteTable('workouts', {
  id: integer('id').primaryKey({ autoIncrement: true }),
  template_id: integer('template_id').references(() => workoutTemplates.id, { onDelete: 'set null' }),
  gym_id: integer('gym_id').notNull().references(() => gyms.id, { onDelete: 'cascade' }),
  name: text('name').notNull(),
  started_at: text('started_at').default(sql`(datetime('now'))`),
  finished_at: text('finished_at'),
  duration_seconds: integer('duration_seconds').notNull().default(0),
});

export const workoutExercises = sqliteTable('workout_exercises', {
  id: integer('id').primaryKey({ autoIncrement: true }),
  workout_id: integer('workout_id').notNull().references(() => workouts.id, { onDelete: 'cascade' }),
  exercise_id: integer('exercise_id').notNull().references(() => exercises.id, { onDelete: 'cascade' }),
  note: text('note'),
  is_completed: integer('is_completed').notNull().default(0),
  sort_order: integer('sort_order').notNull().default(0),
});

export const workoutSets = sqliteTable('workout_sets', {
  id: integer('id').primaryKey({ autoIncrement: true }),
  workout_exercise_id: integer('workout_exercise_id').notNull().references(() => workoutExercises.id, { onDelete: 'cascade' }),
  set_number: integer('set_number').notNull(),
  kg: real('kg'),
  reps: integer('reps'),
});
