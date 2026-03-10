import { useCallback, useState } from 'react';
import { View, Text, ScrollView, StyleSheet, TouchableOpacity } from 'react-native';
import { useLocalSearchParams, useRouter, useFocusEffect, Stack } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { Colors } from '../../../../../src/constants/colors';
import * as db from '../../../../../src/db/database';

function formatDuration(seconds: number) {
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  const s = seconds % 60;
  if (h > 0) return `${h}h ${m}m`;
  return `${m}m ${s}s`;
}

export default function WorkoutSummaryScreen() {
  const { gymId, workoutId } = useLocalSearchParams<{ gymId: string; workoutId: string }>();
  const router = useRouter();
  const wId = Number(workoutId);

  const [workout, setWorkout] = useState<any>(null);
  const [exercises, setExercises] = useState<any[]>([]);

  useFocusEffect(
    useCallback(() => {
      (async () => {
        const w = await db.getWorkout(wId);
        setWorkout(w);
        const exs = await db.getWorkoutExercises(wId);
        const withSets = [];
        for (const ex of exs) {
          if (!ex.is_completed) continue;
          const sets = await db.getWorkoutSets(ex.id);
          withSets.push({ ...ex, sets });
        }
        setExercises(withSets);
      })();
    }, [wId])
  );

  if (!workout) return null;

  const totalSets = exercises.reduce((sum, e) => sum + e.sets.length, 0);
  const totalVolume = exercises.reduce(
    (sum, e) =>
      sum + e.sets.reduce((s: number, set: any) => s + (set.kg || 0) * (set.reps || 0), 0),
    0
  );

  return (
    <View style={styles.container}>
      <Stack.Screen options={{ title: 'Workout Complete', headerBackVisible: false }} />

      <ScrollView contentContainerStyle={styles.content}>
        <View style={styles.header}>
          <View style={styles.trophyWrap}>
            <Ionicons name="trophy" size={40} color={Colors.warning} />
          </View>
          <Text style={styles.title}>{workout.name}</Text>
          <Text style={styles.date}>
            {new Date(workout.started_at + 'Z').toLocaleDateString('en-US', {
              weekday: 'long',
              month: 'long',
              day: 'numeric',
            })}
          </Text>
        </View>

        <View style={styles.statsRow}>
          <View style={styles.statCard}>
            <Ionicons name="time-outline" size={22} color={Colors.primary} />
            <Text style={styles.statValue}>{formatDuration(workout.duration_seconds)}</Text>
            <Text style={styles.statLabel}>Duration</Text>
          </View>
          <View style={styles.statCard}>
            <Ionicons name="barbell-outline" size={22} color={Colors.primary} />
            <Text style={styles.statValue}>{exercises.length}</Text>
            <Text style={styles.statLabel}>Exercises</Text>
          </View>
          <View style={styles.statCard}>
            <Ionicons name="layers-outline" size={22} color={Colors.primary} />
            <Text style={styles.statValue}>{totalSets}</Text>
            <Text style={styles.statLabel}>Sets</Text>
          </View>
          <View style={styles.statCard}>
            <Ionicons name="fitness-outline" size={22} color={Colors.primary} />
            <Text style={styles.statValue}>{Math.round(totalVolume)}</Text>
            <Text style={styles.statLabel}>Volume</Text>
          </View>
        </View>

        {exercises.map((ex) => (
          <View key={ex.id} style={styles.exerciseCard}>
            <Text style={styles.exerciseName}>
              {ex.is_completed ? '\u2713 ' : ''}{ex.name}
            </Text>
            {ex.note ? <Text style={styles.note}>{ex.note}</Text> : null}
            {ex.sets.map((set: any) => (
              <Text key={set.id} style={styles.setText}>
                Set {set.set_number}: {set.kg ?? 0} kg x {set.reps ?? 0} reps
              </Text>
            ))}
          </View>
        ))}
      </ScrollView>

      <TouchableOpacity
        style={styles.doneBtn}
        onPress={() => router.replace(`/gym/${gymId}`)}
        activeOpacity={0.8}
      >
        <Text style={styles.doneBtnText}>Back to Gym</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: Colors.background },
  content: { padding: 20, paddingBottom: 100 },
  header: { alignItems: 'center', paddingVertical: 24 },
  trophyWrap: {
    width: 72,
    height: 72,
    borderRadius: 36,
    backgroundColor: Colors.warning + '15',
    justifyContent: 'center',
    alignItems: 'center',
  },
  title: { fontSize: 24, fontWeight: '800', color: Colors.text, marginTop: 14 },
  date: { fontSize: 14, color: Colors.textSecondary, marginTop: 4 },
  statsRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 10,
    marginBottom: 24,
  },
  statCard: {
    flex: 1,
    minWidth: '40%',
    backgroundColor: Colors.surface,
    borderRadius: 14,
    padding: 16,
    alignItems: 'center',
    borderWidth: 1,
    borderColor: Colors.border,
  },
  statValue: { fontSize: 22, fontWeight: '800', color: Colors.text, marginTop: 4 },
  statLabel: { fontSize: 11, color: Colors.textSecondary, marginTop: 2, textTransform: 'uppercase', letterSpacing: 1 },
  exerciseCard: {
    backgroundColor: Colors.surface,
    borderRadius: 14,
    padding: 16,
    marginBottom: 10,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  exerciseName: { fontSize: 16, fontWeight: '700', color: Colors.text, marginBottom: 6 },
  note: { fontSize: 13, color: Colors.textSecondary, marginBottom: 6, fontStyle: 'italic' },
  setText: { fontSize: 14, color: Colors.textSecondary, marginLeft: 8, marginBottom: 2 },
  doneBtn: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    backgroundColor: Colors.primary,
    paddingVertical: 18,
    paddingBottom: 34,
    alignItems: 'center',
  },
  doneBtnText: { color: Colors.background, fontSize: 18, fontWeight: '700' },
});
