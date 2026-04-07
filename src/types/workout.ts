export type SetData = {
  id: number;
  workout_exercise_id: number;
  set_number: number;
  kg: number | null;
  reps: number | null;
};

export type WorkoutExercise = {
  id: number;
  workout_id: number;
  exercise_id: number;
  note: string | null;
  is_completed: number;
  sort_order: number;
  name: string;
  details: string | null;
  sets: SetData[];
};
