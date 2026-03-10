import { useCallback, useState } from 'react';
import { View, Text, FlatList, TouchableOpacity, StyleSheet } from 'react-native';
import { useLocalSearchParams, useRouter, useFocusEffect } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { Colors } from '../../../../src/constants/colors';
import { getWorkoutHistory } from '../../../../src/db/database';

function formatDuration(seconds: number) {
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  if (h > 0) return `${h}h ${m}m`;
  return `${m}m`;
}

function formatDate(dateStr: string) {
  const d = new Date(dateStr + 'Z');
  return d.toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

export default function HistoryScreen() {
  const { gymId } = useLocalSearchParams<{ gymId: string }>();
  const router = useRouter();
  const [workouts, setWorkouts] = useState<any[]>([]);

  useFocusEffect(
    useCallback(() => {
      (async () => {
        const data = await getWorkoutHistory(Number(gymId));
        setWorkouts(data);
      })();
    }, [gymId])
  );

  return (
    <View style={styles.container}>
      {workouts.length === 0 ? (
        <View style={styles.empty}>
          <Ionicons name="calendar-outline" size={48} color={Colors.textLight} />
          <Text style={styles.emptyText}>No completed workouts yet</Text>
        </View>
      ) : (
        <FlatList
          data={workouts}
          keyExtractor={(item) => item.id.toString()}
          contentContainerStyle={styles.list}
          renderItem={({ item }) => (
            <TouchableOpacity
              style={styles.card}
              onPress={() =>
                router.navigate(`/gym/${gymId}/workout/summary/${item.id}`)
              }
              activeOpacity={0.7}
            >
              <View style={styles.cardHeader}>
                <Text style={styles.cardTitle}>{item.name}</Text>
                <Text style={styles.cardDate}>{formatDate(item.started_at)}</Text>
              </View>
              <View style={styles.cardStats}>
                <View style={styles.stat}>
                  <Ionicons name="time-outline" size={14} color={Colors.primary} />
                  <Text style={styles.statText}>{formatDuration(item.duration_seconds)}</Text>
                </View>
                <View style={styles.stat}>
                  <Ionicons name="barbell-outline" size={14} color={Colors.primary} />
                  <Text style={styles.statText}>{item.exercise_count} exercises</Text>
                </View>
                <View style={styles.stat}>
                  <Ionicons name="layers-outline" size={14} color={Colors.primary} />
                  <Text style={styles.statText}>{item.total_sets} sets</Text>
                </View>
              </View>
            </TouchableOpacity>
          )}
        />
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: Colors.background },
  list: { padding: 20 },
  card: {
    backgroundColor: Colors.surface,
    borderRadius: 14,
    padding: 16,
    marginBottom: 10,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  cardHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 10,
  },
  cardTitle: { fontSize: 16, fontWeight: '700', color: Colors.text },
  cardDate: { fontSize: 12, color: Colors.textSecondary },
  cardStats: { flexDirection: 'row', gap: 16 },
  stat: { flexDirection: 'row', alignItems: 'center', gap: 4 },
  statText: { fontSize: 13, color: Colors.textSecondary },
  empty: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  emptyText: { fontSize: 16, color: Colors.textSecondary, marginTop: 12 },
});
