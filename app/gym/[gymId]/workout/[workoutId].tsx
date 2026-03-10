import { useCallback, useState, useEffect, useRef } from 'react';
import {
  View,
  Text,
  ScrollView,
  TouchableOpacity,
  TextInput,
  StyleSheet,
  Alert,
} from 'react-native';
import { useLocalSearchParams, useRouter, useFocusEffect, Stack } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { Colors } from '../../../../src/constants/colors';
import * as db from '../../../../src/db/database';

type WorkoutExercise = {
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

type SetData = {
  id: number;
  workout_exercise_id: number;
  set_number: number;
  kg: number | null;
  reps: number | null;
};

export default function ActiveWorkoutScreen() {
  const { gymId, workoutId } = useLocalSearchParams<{ gymId: string; workoutId: string }>();
  const router = useRouter();
  const wId = Number(workoutId);
  const gId = Number(gymId);

  const [workoutName, setWorkoutName] = useState('');
  const [exercises, setExercises] = useState<WorkoutExercise[]>([]);
  const [elapsed, setElapsed] = useState(0);
  const [startTime, setStartTime] = useState<Date | null>(null);
  const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const loadWorkout = useCallback(async () => {
    const workout = await db.getWorkout(wId);
    if (!workout) return;
    setWorkoutName(workout.name);
    setStartTime(new Date(workout.started_at + 'Z'));

    const exs = await db.getWorkoutExercises(wId);
    const withSets: WorkoutExercise[] = [];
    for (const ex of exs) {
      const sets = await db.getWorkoutSets(ex.id);
      withSets.push({ ...ex, sets });
    }
    setExercises(withSets);
  }, [wId]);

  useFocusEffect(useCallback(() => { loadWorkout(); }, [loadWorkout]));

  useEffect(() => {
    intervalRef.current = setInterval(() => {
      if (startTime) {
        setElapsed(Math.floor((Date.now() - startTime.getTime()) / 1000));
      }
    }, 1000);
    return () => { if (intervalRef.current) clearInterval(intervalRef.current); };
  }, [startTime]);

  const formatTime = (s: number) => {
    const h = Math.floor(s / 3600);
    const m = Math.floor((s % 3600) / 60);
    const sec = s % 60;
    return `${h > 0 ? h + ':' : ''}${m.toString().padStart(2, '0')}:${sec.toString().padStart(2, '0')}`;
  };

  const handleAddExercise = () => {
    router.push({
      pathname: '/exercises/pick',
      params: { workoutId: wId.toString(), gymId: gId.toString() },
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
          await db.deleteWorkout(wId);
          router.replace(`/gym/${gymId}`);
        },
      },
      {
        text: 'Finish',
        onPress: async () => {
          for (const ex of exercises) {
            for (const set of ex.sets) {
              await db.updateWorkoutSet(set.id, set.kg ?? undefined, set.reps ?? undefined);
            }
            if (ex.note != null) {
              await db.updateWorkoutExerciseNote(ex.id, ex.note);
            }
          }
          await db.finishWorkout(wId);
          const workout = await db.getWorkout(wId);
          if (workout && !workout.template_id) {
            const templateId = await db.createWorkoutTemplate(workout.name, gId);
            for (const ex of exercises) {
              await db.addTemplateExercise(templateId, ex.exercise_id, ex.sort_order);
            }
          }
          router.replace(`/gym/${gymId}/workout/summary/${wId}`);
        },
      },
    ]);
  };

  return (
    <View style={styles.container}>
      <Stack.Screen options={{ title: workoutName, headerBackVisible: false }} />

      <View style={styles.timer}>
        <Ionicons name="time-outline" size={18} color={Colors.primary} />
        <Text style={styles.timerText}>{formatTime(elapsed)}</Text>
      </View>

      <ScrollView style={styles.scroll} contentContainerStyle={styles.scrollContent}>
        {exercises.map((ex) => (
          <View
            key={ex.id}
            style={[styles.exerciseCard, ex.is_completed === 1 && styles.exerciseCompleted]}
          >
            <View style={styles.exerciseHeader}>
              <TouchableOpacity
                style={styles.checkBtn}
                onPress={() => handleToggleComplete(ex.id, ex.is_completed)}
              >
                <Ionicons
                  name={ex.is_completed ? 'checkmark-circle' : 'ellipse-outline'}
                  size={26}
                  color={ex.is_completed ? Colors.success : Colors.textLight}
                />
              </TouchableOpacity>
              <Text style={[styles.exerciseName, ex.is_completed === 1 && styles.exerciseNameDone]}>
                {ex.name}
              </Text>
            </View>

            <TextInput
              style={styles.noteInput}
              placeholder="Add note..."
              placeholderTextColor={Colors.textLight}
              value={ex.note || ''}
              onChangeText={(text) => handleUpdateNote(ex.id, text)}
              multiline
            />

            <View style={styles.setsHeader}>
              <Text style={styles.setHeaderText}>SET</Text>
              <Text style={styles.setHeaderText}>KG</Text>
              <Text style={styles.setHeaderText}>REPS</Text>
              <Text style={{ width: 30 }} />
            </View>

            {ex.sets.map((set) => (
              <View key={set.id} style={styles.setRow}>
                <Text style={styles.setNumber}>{set.set_number}</Text>
                <TextInput
                  style={styles.setInput}
                  placeholder="0"
                  placeholderTextColor={Colors.textLight}
                  value={set.kg != null ? String(set.kg) : ''}
                  onChangeText={(v) => handleUpdateSet(set.id, 'kg', v)}
                  keyboardType="numeric"
                />
                <TextInput
                  style={styles.setInput}
                  placeholder="0"
                  placeholderTextColor={Colors.textLight}
                  value={set.reps != null ? String(set.reps) : ''}
                  onChangeText={(v) => handleUpdateSet(set.id, 'reps', v)}
                  keyboardType="numeric"
                />
                <TouchableOpacity onPress={() => handleDeleteSet(set.id)}>
                  <Ionicons name="close-circle-outline" size={20} color={Colors.danger} />
                </TouchableOpacity>
              </View>
            ))}

            <TouchableOpacity style={styles.addSetBtn} onPress={() => handleAddSet(ex.id)}>
              <Ionicons name="add" size={16} color={Colors.primary} />
              <Text style={styles.addSetText}>Add Set</Text>
            </TouchableOpacity>
          </View>
        ))}

        <TouchableOpacity style={styles.addExerciseBtn} onPress={handleAddExercise} activeOpacity={0.7}>
          <Ionicons name="add-circle-outline" size={20} color={Colors.primary} />
          <Text style={styles.addExerciseText}>Add Exercise</Text>
        </TouchableOpacity>
      </ScrollView>

      <TouchableOpacity style={styles.finishBtn} onPress={handleFinish} activeOpacity={0.8}>
        <Ionicons name="checkmark-done" size={20} color={Colors.background} />
        <Text style={styles.finishBtnText}>Finish Workout</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: Colors.background },
  timer: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 6,
    paddingVertical: 12,
    backgroundColor: Colors.surface,
    borderBottomWidth: 1,
    borderBottomColor: Colors.border,
  },
  timerText: { fontSize: 20, fontWeight: '700', color: Colors.primary, fontVariant: ['tabular-nums'] },
  scroll: { flex: 1 },
  scrollContent: { padding: 20, paddingBottom: 100 },
  exerciseCard: {
    backgroundColor: Colors.surface,
    borderRadius: 14,
    padding: 16,
    marginBottom: 12,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  exerciseCompleted: { borderLeftWidth: 3, borderLeftColor: Colors.success },
  exerciseHeader: { flexDirection: 'row', alignItems: 'center', gap: 10, marginBottom: 8 },
  checkBtn: {},
  exerciseName: { fontSize: 17, fontWeight: '700', color: Colors.text, flex: 1 },
  exerciseNameDone: { textDecorationLine: 'line-through', color: Colors.textSecondary },
  noteInput: {
    borderWidth: 1,
    borderColor: Colors.border,
    borderRadius: 10,
    padding: 10,
    fontSize: 14,
    color: Colors.text,
    marginBottom: 12,
    minHeight: 36,
    backgroundColor: Colors.background,
  },
  setsHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 4,
    marginBottom: 6,
  },
  setHeaderText: {
    flex: 1,
    fontSize: 11,
    fontWeight: '700',
    color: Colors.textLight,
    textAlign: 'center',
    letterSpacing: 1,
  },
  setRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 6,
    gap: 4,
  },
  setNumber: {
    flex: 1,
    textAlign: 'center',
    fontSize: 15,
    fontWeight: '600',
    color: Colors.textSecondary,
  },
  setInput: {
    flex: 1,
    borderWidth: 1,
    borderColor: Colors.border,
    borderRadius: 8,
    padding: 10,
    textAlign: 'center',
    fontSize: 16,
    backgroundColor: Colors.background,
    color: Colors.text,
  },
  addSetBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 4,
    paddingVertical: 8,
    marginTop: 4,
  },
  addSetText: { color: Colors.primary, fontSize: 14, fontWeight: '600' },
  addExerciseBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 6,
    paddingVertical: 18,
    backgroundColor: Colors.surface,
    borderRadius: 14,
    borderWidth: 1,
    borderColor: Colors.primary + '30',
    borderStyle: 'dashed',
  },
  addExerciseText: { color: Colors.primary, fontSize: 16, fontWeight: '600' },
  finishBtn: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
    backgroundColor: Colors.primary,
    paddingVertical: 18,
    paddingBottom: 34,
  },
  finishBtnText: { color: Colors.background, fontSize: 18, fontWeight: '700' },
});
