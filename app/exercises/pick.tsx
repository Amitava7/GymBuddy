import { useCallback, useState } from 'react';
import {
  View,
  Text,
  FlatList,
  TouchableOpacity,
  TextInput,
  StyleSheet,
} from 'react-native';
import { useLocalSearchParams, useRouter, useFocusEffect } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { Colors } from '../../src/constants/colors';
import * as db from '../../src/db/database';

export default function PickExerciseScreen() {
  const { workoutId, gymId } = useLocalSearchParams<{ workoutId: string; gymId: string }>();
  const router = useRouter();
  const [exercises, setExercises] = useState<any[]>([]);
  const [search, setSearch] = useState('');
  const [showCreate, setShowCreate] = useState(false);
  const [newName, setNewName] = useState('');

  const load = useCallback(async () => {
    const data = await db.getExercises(search || undefined);
    setExercises(data);
  }, [search]);

  useFocusEffect(useCallback(() => { load(); }, [load]));

  const handlePick = async (exerciseId: number) => {
    const existing = await db.getWorkoutExercises(Number(workoutId));
    const weId = await db.addWorkoutExercise(Number(workoutId), exerciseId, existing.length);
    const lastData = await db.getLastWorkoutDataForExercise(exerciseId);
    if (lastData) {
      if (lastData.note) {
        await db.updateWorkoutExerciseNote(weId, lastData.note);
      }
      if (lastData.sets.length > 0) {
        for (const set of lastData.sets) {
          await db.addWorkoutSet(weId, set.set_number, set.kg ?? undefined, set.reps ?? undefined);
        }
      } else {
        await db.addWorkoutSet(weId, 1);
      }
    } else {
      await db.addWorkoutSet(weId, 1);
    }
    router.back();
  };

  const handleCreateAndPick = async () => {
    if (!newName.trim()) return;
    const id = await db.createExercise(newName.trim());
    await handlePick(id);
  };

  return (
    <View style={styles.container}>
      <View style={styles.searchBar}>
        <Ionicons name="search" size={18} color={Colors.textLight} />
        <TextInput
          style={styles.searchInput}
          placeholder="Search exercises..."
          placeholderTextColor={Colors.textLight}
          value={search}
          onChangeText={setSearch}
          autoFocus
        />
      </View>

      <FlatList
        data={exercises}
        keyExtractor={(item) => item.id.toString()}
        contentContainerStyle={styles.list}
        ListEmptyComponent={
          <View style={styles.empty}>
            <Text style={styles.emptyText}>
              {search ? 'No exercises found' : 'No exercises yet'}
            </Text>
          </View>
        }
        renderItem={({ item }) => (
          <TouchableOpacity style={styles.card} onPress={() => handlePick(item.id)} activeOpacity={0.7}>
            <Ionicons name="add-circle" size={22} color={Colors.success} />
            <View style={styles.cardContent}>
              <Text style={styles.cardTitle}>{item.name}</Text>
              {item.details ? (
                <Text style={styles.cardSubtitle} numberOfLines={1}>{item.details}</Text>
              ) : null}
            </View>
          </TouchableOpacity>
        )}
      />

      {showCreate ? (
        <View style={styles.createBar}>
          <TextInput
            style={styles.createInput}
            placeholder="New exercise name"
            placeholderTextColor={Colors.textLight}
            value={newName}
            onChangeText={setNewName}
            autoFocus
          />
          <TouchableOpacity style={styles.createBtn} onPress={handleCreateAndPick}>
            <Text style={styles.createBtnText}>Add</Text>
          </TouchableOpacity>
          <TouchableOpacity onPress={() => setShowCreate(false)}>
            <Ionicons name="close" size={24} color={Colors.textSecondary} />
          </TouchableOpacity>
        </View>
      ) : (
        <TouchableOpacity style={styles.createToggle} onPress={() => setShowCreate(true)}>
          <Ionicons name="add" size={20} color={Colors.primary} />
          <Text style={styles.createToggleText}>Create New Exercise</Text>
        </TouchableOpacity>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: Colors.background },
  searchBar: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: Colors.surface,
    margin: 20,
    marginBottom: 8,
    borderRadius: 12,
    paddingHorizontal: 14,
    gap: 10,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  searchInput: { flex: 1, paddingVertical: 12, fontSize: 16, color: Colors.text },
  list: { paddingHorizontal: 20, paddingTop: 8 },
  card: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: Colors.surface,
    borderRadius: 14,
    padding: 14,
    marginBottom: 8,
    gap: 12,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  cardContent: { flex: 1 },
  cardTitle: { fontSize: 16, fontWeight: '600', color: Colors.text },
  cardSubtitle: { fontSize: 13, color: Colors.textSecondary, marginTop: 2 },
  empty: { alignItems: 'center', marginTop: 40 },
  emptyText: { fontSize: 15, color: Colors.textSecondary },
  createBar: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 14,
    paddingBottom: 30,
    backgroundColor: Colors.surface,
    borderTopWidth: 1,
    borderTopColor: Colors.border,
    gap: 10,
  },
  createInput: {
    flex: 1,
    borderWidth: 1,
    borderColor: Colors.border,
    borderRadius: 10,
    padding: 12,
    fontSize: 16,
    backgroundColor: Colors.background,
    color: Colors.text,
  },
  createBtn: {
    backgroundColor: Colors.primary,
    paddingHorizontal: 18,
    paddingVertical: 12,
    borderRadius: 10,
  },
  createBtnText: { color: Colors.background, fontWeight: '700' },
  createToggle: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 6,
    padding: 16,
    paddingBottom: 30,
    backgroundColor: Colors.surface,
    borderTopWidth: 1,
    borderTopColor: Colors.border,
  },
  createToggleText: { color: Colors.primary, fontSize: 15, fontWeight: '600' },
});
