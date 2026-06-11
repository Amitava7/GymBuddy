import { useCallback, useEffect, useRef, useState } from 'react';
import { Alert } from 'react-native';
import { useRouter, useFocusEffect, useNavigation } from 'expo-router';
import * as db from '../db/database';
import { WorkoutExercise } from '../types/workout';

export function useActiveWorkout(workoutId: number, gymId: number) {
  const router = useRouter();
  const navigation = useNavigation();
  const [workoutName, setWorkoutName] = useState('');
  const [exercises, setExercises] = useState<WorkoutExercise[]>([]);
  const [startTime, setStartTime] = useState<Date | null>(null);
  const [templateId, setTemplateId] = useState<number | null>(null);
  // Set to true right before an intentional navigation (finish / discard)
  // so the back-guard below lets that navigation through without prompting.
  const leavingRef = useRef(false);

  const loadWorkout = useCallback(async () => {
    const workout = await db.getWorkout(workoutId);
    if (!workout) return;
    setWorkoutName(workout.name);
    setStartTime(new Date(workout.started_at + 'Z'));
    setTemplateId(workout.template_id ?? null);

    const exs = await db.getWorkoutExercises(workoutId);
    const withSets: WorkoutExercise[] = [];
    for (const ex of exs) {
      const sets = await db.getWorkoutSets(ex.id);
      withSets.push({ ...ex, sets });
    }
    setExercises(withSets);
  }, [workoutId]);

  useFocusEffect(useCallback(() => { loadWorkout(); }, [loadWorkout]));

  // Mirror the latest exercises into a ref so the back-guard listener (which is
  // registered once) can flush the current values rather than a stale closure.
  const exercisesRef = useRef(exercises);
  exercisesRef.current = exercises;

  const flushUnsavedData = async () => {
    for (const ex of exercisesRef.current) {
      for (const set of ex.sets) {
        await db.updateWorkoutSet(set.id, set.kg ?? undefined, set.reps ?? undefined);
      }
      if (ex.note != null) {
        await db.updateWorkoutExerciseNote(ex.id, ex.note);
      }
    }
  };

  // Block accidental back navigation (Android hardware back, iOS swipe-back,
  // header back) out of an in-progress workout. Intentional exits set
  // leavingRef first so they pass through untouched.
  useEffect(() => {
    const unsubscribe = (navigation as any).addListener('beforeRemove', (e: any) => {
      if (leavingRef.current) return;
      e.preventDefault();
      Alert.alert(
        'Leave workout?',
        "This workout is still in progress. It won't be saved to your history until you tap Finish.",
        [
          { text: 'Stay', style: 'cancel' },
          {
            text: 'Leave',
            style: 'destructive',
            onPress: async () => {
              await flushUnsavedData();
              leavingRef.current = true;
              navigation.dispatch(e.data.action);
            },
          },
        ]
      );
    });
    return unsubscribe;
  }, [navigation]);

  const handleAddExercise = async () => {
    await flushUnsavedData();
    router.push({
      pathname: '/exercises/pick',
      params: { workoutId: workoutId.toString(), gymId: gymId.toString() },
    });
  };

  const handleAddSet = async (weId: number) => {
    const ex = exercises.find((e) => e.id === weId);
    if (!ex) return;
    const nextSetNum = ex.sets.length + 1;
    const newId = await db.addWorkoutSet(weId, nextSetNum);
    setExercises((prev) =>
      prev.map((e) =>
        e.id === weId
          ? { ...e, sets: [...e.sets, { id: newId, workout_exercise_id: weId, set_number: nextSetNum, kg: null, reps: null }] }
          : e
      )
    );
  };

  const handleUpdateSet = (setId: number, field: 'kg' | 'reps', value: string) => {
    const numVal = value === '' ? null : Number(value);
    setExercises((prev) =>
      prev.map((e) => ({
        ...e,
        sets: e.sets.map((s) =>
          s.id === setId ? { ...s, [field]: numVal } : s
        ),
      }))
    );
  };

  const handleDeleteSet = async (setId: number) => {
    await db.deleteWorkoutSet(setId);
    setExercises((prev) =>
      prev.map((e) => ({
        ...e,
        sets: e.sets.filter((s) => s.id !== setId),
      }))
    );
  };

  const handleDeleteExercise = (weId: number, name: string) => {
    Alert.alert('Remove Exercise', `Remove "${name}" from this workout?`, [
      { text: 'Cancel', style: 'cancel' },
      {
        text: 'Remove',
        style: 'destructive',
        onPress: async () => {
          const ex = exercises.find((e) => e.id === weId);
          await db.deleteWorkoutExercise(weId);
          if (templateId && ex) {
            await db.removeTemplateExercise(templateId, ex.exercise_id);
          }
          setExercises((prev) => prev.filter((e) => e.id !== weId));
        },
      },
    ]);
  };

  const handleToggleComplete = async (weId: number, current: number) => {
    const ex = exercises.find((e) => e.id === weId);
    if (ex) {
      for (const set of ex.sets) {
        await db.updateWorkoutSet(set.id, set.kg ?? undefined, set.reps ?? undefined);
      }
      if (ex.note != null) {
        await db.updateWorkoutExerciseNote(weId, ex.note);
      }
    }
    await db.toggleWorkoutExercise(weId, current === 0);
    setExercises((prev) =>
      prev.map((e) => (e.id === weId ? { ...e, is_completed: current === 0 ? 1 : 0 } : e))
    );
  };

  const handleUpdateNote = (weId: number, note: string) => {
    setExercises((prev) =>
      prev.map((e) => (e.id === weId ? { ...e, note } : e))
    );
  };

  const handleFinish = () => {
    Alert.alert('Finish Workout', 'Complete this workout?', [
      { text: 'Keep Going', style: 'cancel' },
      {
        text: 'Cancel Workout',
        style: 'destructive',
        onPress: async () => {
          await db.deleteWorkout(workoutId);
          leavingRef.current = true;
          router.dismissTo(`/gym/${gymId}`);
        },
      },
      {
        text: 'Finish',
        onPress: async () => {
          await flushUnsavedData();
          await db.finishWorkout(workoutId);
          const workout = await db.getWorkout(workoutId);
          if (workout && !workout.template_id) {
            const templateId = await db.createWorkoutTemplate(workout.name, gymId);
            for (const ex of exercises) {
              await db.addTemplateExercise(templateId, ex.exercise_id, ex.sort_order);
            }
            await db.setWorkoutTemplate(workoutId, templateId);
          } else if (workout && workout.template_id) {
            const existing = await db.getTemplateExercises(workout.template_id);
            const existingIds = new Set(existing.map((e) => e.exercise_id));
            for (const ex of exercises) {
              if (!existingIds.has(ex.exercise_id)) {
                await db.addTemplateExercise(workout.template_id, ex.exercise_id, ex.sort_order);
              }
            }
          }
          leavingRef.current = true;
          router.replace(`/gym/${gymId}/workout/summary/${workoutId}`);
        },
      },
    ]);
  };

  return {
    workoutName,
    exercises,
    startTime,
    handleAddExercise,
    handleAddSet,
    handleUpdateSet,
    handleDeleteSet,
    handleDeleteExercise,
    handleToggleComplete,
    handleUpdateNote,
    handleFinish,
  };
}
