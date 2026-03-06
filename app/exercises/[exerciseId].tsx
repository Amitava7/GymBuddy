import { useCallback, useState } from 'react';
import { View, Text, ScrollView, TouchableOpacity, StyleSheet } from 'react-native';
import { useLocalSearchParams, useRouter, useFocusEffect, Stack } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { Colors } from '../../src/constants/colors';
import * as db from '../../src/db/database';

function formatDate(dateStr: string) {
  const d = new Date(dateStr + 'Z');
  return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
}

export default function ExerciseDetailScreen() {
  const { exerciseId } = useLocalSearchParams<{ exerciseId: string }>();
  const router = useRouter();
  const id = Number(exerciseId);

  const [exercise, setExercise] = useState<any>(null);
  const [history, setHistory] = useState<any[]>([]);
  const [records, setRecords] = useState<any>(null);

  useFocusEffect(
    useCallback(() => {
      (async () => {
        const ex = await db.getExercise(id);
        setExercise(ex);
        const hist = await db.getExerciseHistory(id);
        setHistory(hist);
        const rec = await db.getExerciseRecords(id);
        setRecords(rec);
      })();
    }, [id])
  );

  if (!exercise) return null;

  return (
    <View style={styles.container}>
      <Stack.Screen options={{ title: exercise.name }} />

      <ScrollView contentContainerStyle={styles.content}>
        <View style={styles.header}>
          <Text style={styles.title}>{exercise.name}</Text>
          <TouchableOpacity
            onPress={() => router.push({ pathname: '/exercises/form', params: { exerciseId: id.toString() } })}
          >
            <Ionicons name="create-outline" size={24} color={Colors.primary} />
          </TouchableOpacity>
        </View>

        {exercise.details ? (
          <View style={styles.detailsCard}>
            <Text style={styles.detailsLabel}>Details</Text>
            <Text style={styles.detailsText}>{exercise.details}</Text>
          </View>
        ) : null}

        {records && (records.max_kg || records.max_reps) ? (
          <View style={styles.recordsSection}>
            <Text style={styles.sectionTitle}>Personal Records</Text>
            <View style={styles.recordsRow}>
              {records.max_kg != null && (
                <View style={styles.recordCard}>
                  <Ionicons name="trophy" size={22} color={Colors.warning} />
                  <Text style={styles.recordValue}>{records.max_kg} kg</Text>
                  <Text style={styles.recordLabel}>Max Weight</Text>
                </View>
              )}
              {records.max_reps != null && (
                <View style={styles.recordCard}>
                  <Ionicons name="trophy" size={22} color={Colors.warning} />
                  <Text style={styles.recordValue}>{records.max_reps}</Text>
                  <Text style={styles.recordLabel}>Max Reps</Text>
                </View>
              )}
              {records.max_volume != null && (
                <View style={styles.recordCard}>
                  <Ionicons name="trophy" size={22} color={Colors.warning} />
                  <Text style={styles.recordValue}>{Math.round(records.max_volume)}</Text>
                  <Text style={styles.recordLabel}>Max Volume</Text>
                </View>
              )}
            </View>
          </View>
        ) : null}

        <View style={styles.historySection}>
          <Text style={styles.sectionTitle}>History & Trends</Text>
          {history.length === 0 ? (
            <Text style={styles.noHistory}>No workout data yet for this exercise</Text>
          ) : (
            <>
              {/* Simple bar chart showing volume trend */}
              <View style={styles.chart}>
                <Text style={styles.chartTitle}>Volume per Session</Text>
                <View style={styles.bars}>
                  {history.slice(0, 10).reverse().map((h, i) => {
                    const maxVol = Math.max(...history.slice(0, 10).map((x) => x.total_volume || 1));
                    const height = Math.max(((h.total_volume || 0) / maxVol) * 100, 4);
                    return (
                      <View key={i} style={styles.barContainer}>
                        <View style={[styles.bar, { height }]} />
                        <Text style={styles.barLabel}>{formatDate(h.workout_date).split(',')[0]}</Text>
                      </View>
                    );
                  })}
                </View>
              </View>

              {history.map((h, i) => (
                <View key={i} style={styles.historyCard}>
                  <View style={styles.historyHeader}>
                    <Text style={styles.historyDate}>{formatDate(h.workout_date)}</Text>
                    <Text style={styles.historyWorkout}>{h.workout_name}</Text>
                  </View>
                  <View style={styles.historyStats}>
                    <Text style={styles.historyStat}>Max: {h.max_kg ?? 0} kg</Text>
                    <Text style={styles.historyStat}>Reps: {h.max_reps ?? 0}</Text>
                    <Text style={styles.historyStat}>{h.total_sets} sets</Text>
                    <Text style={styles.historyStat}>Vol: {Math.round(h.total_volume)}</Text>
                  </View>
                </View>
              ))}
            </>
          )}
        </View>
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: Colors.background },
  content: { padding: 16 },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 16,
  },
  title: { fontSize: 24, fontWeight: '800', color: Colors.text },
  detailsCard: {
    backgroundColor: Colors.surface,
    borderRadius: 12,
    padding: 16,
    marginBottom: 20,
    elevation: 1,
  },
  detailsLabel: { fontSize: 13, fontWeight: '700', color: Colors.textSecondary, marginBottom: 6 },
  detailsText: { fontSize: 15, color: Colors.text, lineHeight: 22 },
  recordsSection: { marginBottom: 20 },
  sectionTitle: { fontSize: 18, fontWeight: '700', color: Colors.text, marginBottom: 12 },
  recordsRow: { flexDirection: 'row', gap: 10 },
  recordCard: {
    flex: 1,
    backgroundColor: Colors.surface,
    borderRadius: 12,
    padding: 14,
    alignItems: 'center',
    elevation: 1,
  },
  recordValue: { fontSize: 20, fontWeight: '800', color: Colors.text, marginTop: 4 },
  recordLabel: { fontSize: 11, color: Colors.textSecondary, marginTop: 2 },
  historySection: { marginBottom: 20 },
  noHistory: { fontSize: 14, color: Colors.textSecondary, textAlign: 'center', marginTop: 20 },
  chart: {
    backgroundColor: Colors.surface,
    borderRadius: 12,
    padding: 16,
    marginBottom: 16,
    elevation: 1,
  },
  chartTitle: { fontSize: 14, fontWeight: '600', color: Colors.textSecondary, marginBottom: 12 },
  bars: { flexDirection: 'row', alignItems: 'flex-end', justifyContent: 'space-around', height: 110 },
  barContainer: { alignItems: 'center', flex: 1 },
  bar: {
    width: 20,
    backgroundColor: Colors.primary,
    borderRadius: 4,
    minHeight: 4,
  },
  barLabel: { fontSize: 8, color: Colors.textLight, marginTop: 4, textAlign: 'center' },
  historyCard: {
    backgroundColor: Colors.surface,
    borderRadius: 10,
    padding: 14,
    marginBottom: 8,
    elevation: 1,
  },
  historyHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: 6,
  },
  historyDate: { fontSize: 13, fontWeight: '600', color: Colors.text },
  historyWorkout: { fontSize: 13, color: Colors.textSecondary },
  historyStats: { flexDirection: 'row', gap: 12 },
  historyStat: { fontSize: 12, color: Colors.textSecondary },
});
