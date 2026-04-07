import { db } from './database';
import { exercises } from './schema';
import { count } from 'drizzle-orm';

const SEED_EXERCISES: { name: string; details?: string; tags: string }[] = [
  // Chest
  { name: 'Bench Press', tags: 'chest,barbell' },
  { name: 'Incline Bench Press', tags: 'chest,barbell' },
  { name: 'Decline Bench Press', tags: 'chest,barbell' },
  { name: 'Dumbbell Bench Press', tags: 'chest,dumbbell' },
  { name: 'Incline Dumbbell Press', tags: 'chest,dumbbell' },
  { name: 'Dumbbell Fly', tags: 'chest,dumbbell' },
  { name: 'Cable Fly', tags: 'chest,cable' },
  { name: 'Chest Press Machine', tags: 'chest,machine' },
  { name: 'Pec Deck', tags: 'chest,machine' },
  { name: 'Push Up', tags: 'chest,bodyweight' },
  { name: 'Chest Dip', tags: 'chest,bodyweight' },
  // Back
  { name: 'Deadlift', details: 'Keep back straight, drive through heels', tags: 'back,barbell,legs' },
  { name: 'Romanian Deadlift', tags: 'back,barbell,legs' },
  { name: 'Barbell Row', tags: 'back,barbell' },
  { name: 'Dumbbell Row', tags: 'back,dumbbell' },
  { name: 'Pull Up', tags: 'back,bodyweight' },
  { name: 'Chin Up', tags: 'back,arms,bodyweight' },
  { name: 'Lat Pulldown', tags: 'back,cable,machine' },
  { name: 'Seated Cable Row', tags: 'back,cable' },
  { name: 'T-Bar Row', tags: 'back,machine' },
  { name: 'Face Pull', tags: 'back,shoulders,cable' },
  { name: 'Good Morning', tags: 'back,barbell' },
  { name: 'Back Extension', tags: 'back,bodyweight' },
  // Shoulders
  { name: 'Overhead Press', tags: 'shoulders,barbell' },
  { name: 'Dumbbell Shoulder Press', tags: 'shoulders,dumbbell' },
  { name: 'Arnold Press', tags: 'shoulders,dumbbell' },
  { name: 'Lateral Raise', tags: 'shoulders,dumbbell' },
  { name: 'Front Raise', tags: 'shoulders,dumbbell' },
  { name: 'Rear Delt Fly', tags: 'shoulders,dumbbell' },
  { name: 'Cable Lateral Raise', tags: 'shoulders,cable' },
  { name: 'Machine Shoulder Press', tags: 'shoulders,machine' },
  { name: 'Upright Row', tags: 'shoulders,barbell' },
  { name: 'Shrugs', tags: 'shoulders,barbell' },
  // Arms
  { name: 'Barbell Curl', tags: 'arms,barbell' },
  { name: 'Dumbbell Curl', tags: 'arms,dumbbell' },
  { name: 'Hammer Curl', tags: 'arms,dumbbell' },
  { name: 'Preacher Curl', tags: 'arms,machine' },
  { name: 'Cable Curl', tags: 'arms,cable' },
  { name: 'Concentration Curl', tags: 'arms,dumbbell' },
  { name: 'Close Grip Bench Press', tags: 'arms,chest,barbell' },
  { name: 'Skull Crusher', tags: 'arms,barbell' },
  { name: 'Tricep Pushdown', tags: 'arms,cable' },
  { name: 'Overhead Tricep Extension', tags: 'arms,dumbbell' },
  { name: 'Tricep Dip', tags: 'arms,bodyweight' },
  { name: 'Diamond Push Up', tags: 'arms,chest,bodyweight' },
  // Legs
  { name: 'Squat', details: 'Keep knees tracking over toes, chest up', tags: 'legs,barbell' },
  { name: 'Front Squat', tags: 'legs,barbell' },
  { name: 'Sumo Deadlift', tags: 'legs,barbell' },
  { name: 'Leg Press', tags: 'legs,machine' },
  { name: 'Leg Extension', tags: 'legs,machine' },
  { name: 'Leg Curl', tags: 'legs,machine' },
  { name: 'Calf Raise', tags: 'legs,machine' },
  { name: 'Lunges', tags: 'legs,dumbbell' },
  { name: 'Bulgarian Split Squat', tags: 'legs,dumbbell' },
  { name: 'Goblet Squat', tags: 'legs,kettlebell' },
  { name: 'Hip Thrust', tags: 'legs,glutes,barbell' },
  { name: 'Glute Bridge', tags: 'legs,glutes,bodyweight' },
  { name: 'Step Up', tags: 'legs,dumbbell' },
  { name: 'Kettlebell Swing', tags: 'legs,glutes,kettlebell' },
  // Core
  { name: 'Plank', tags: 'core,bodyweight' },
  { name: 'Side Plank', tags: 'core,bodyweight' },
  { name: 'Crunch', tags: 'core,bodyweight' },
  { name: 'Sit Up', tags: 'core,bodyweight' },
  { name: 'Leg Raise', tags: 'core,bodyweight' },
  { name: 'Russian Twist', tags: 'core,bodyweight' },
  { name: 'Ab Rollout', tags: 'core,bodyweight' },
  { name: 'Cable Crunch', tags: 'core,cable' },
  { name: 'Hanging Knee Raise', tags: 'core,bodyweight' },
];

export async function seedExercisesIfNeeded() {
  const result = db.select({ total: count() }).from(exercises).get();
  if (result && result.total > 0) return;

  for (const ex of SEED_EXERCISES) {
    db.insert(exercises).values({ name: ex.name, details: ex.details ?? null, tags: ex.tags }).run();
  }
}
