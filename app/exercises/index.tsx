import { useCallback, useState } from 'react';
import {
  View,
  Text,
  FlatList,
  TouchableOpacity,
  TextInput,
  StyleSheet,
  Alert,
} from 'react-native';
import { useRouter, useFocusEffect } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { Colors } from '../../src/constants/colors';
import { getExercises, deleteExercise } from '../../src/db/database';

export default function ExercisesScreen() {
  const router = useRouter();
  const [exercises, setExercises] = useState<any[]>([]);
  const [search, setSearch] = useState('');

  const load = useCallback(async () => {
    const data = await getExercises(search || undefined);
    setExercises(data);
  }, [search]);

  useFocusEffect(useCallback(() => { load(); }, [load]));

  const handleDelete = (ex: any) => {
    Alert.alert('Delete Exercise', `Delete "${ex.name}"?`, [
      { text: 'Cancel', style: 'cancel' },
      {
        text: 'Delete',
        style: 'destructive',
        onPress: async () => {
          await deleteExercise(ex.id);
          load();
        },
      },
    ]);
  };

  return (
    <View style={styles.container}>
      <View style={styles.searchBar}>
        <Ionicons name="search" size={20} color={Colors.textLight} />
        <TextInput
          style={styles.searchInput}
          placeholder="Search exercises..."
          value={search}
          onChangeText={setSearch}
        />
        {search ? (
          <TouchableOpacity onPress={() => setSearch('')}>
            <Ionicons name="close-circle" size={20} color={Colors.textLight} />
          </TouchableOpacity>
        ) : null}
      </View>

      {exercises.length === 0 ? (
        <View style={styles.empty}>
          <Ionicons name="barbell-outline" size={64} color={Colors.textLight} />
          <Text style={styles.emptyText}>
            {search ? 'No exercises found' : 'No exercises yet'}
          </Text>
          <Text style={styles.emptySubtext}>Tap + to create one</Text>
        </View>
      ) : (
        <FlatList
          data={exercises}
          keyExtractor={(item) => item.id.toString()}
          contentContainerStyle={styles.list}
          renderItem={({ item }) => (
            <TouchableOpacity
              style={styles.card}
              onPress={() => router.navigate(`/exercises/${item.id}`)}
              onLongPress={() => handleDelete(item)}
            >
              <View style={styles.cardIcon}>
                <Ionicons name="barbell" size={22} color={Colors.primary} />
              </View>
              <View style={styles.cardContent}>
                <Text style={styles.cardTitle}>{item.name}</Text>
                {item.details ? (
                  <Text style={styles.cardSubtitle} numberOfLines={1}>
                    {item.details}
                  </Text>
                ) : null}
              </View>
              <Ionicons name="chevron-forward" size={20} color={Colors.textLight} />
            </TouchableOpacity>
          )}
        />
      )}

      <TouchableOpacity
        style={styles.fab}
        onPress={() => router.navigate('/exercises/form')}
      >
        <Ionicons name="add" size={28} color="#fff" />
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: Colors.background },
  searchBar: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: Colors.surface,
    margin: 16,
    marginBottom: 8,
    borderRadius: 10,
    paddingHorizontal: 12,
    gap: 8,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  searchInput: { flex: 1, paddingVertical: 10, fontSize: 16 },
  list: { padding: 16, paddingTop: 8 },
  card: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: Colors.surface,
    borderRadius: 10,
    padding: 14,
    marginBottom: 8,
    elevation: 1,
  },
  cardIcon: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: Colors.primary + '15',
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 12,
  },
  cardContent: { flex: 1 },
  cardTitle: { fontSize: 16, fontWeight: '600', color: Colors.text },
  cardSubtitle: { fontSize: 13, color: Colors.textSecondary, marginTop: 2 },
  empty: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  emptyText: { fontSize: 16, fontWeight: '600', color: Colors.textSecondary, marginTop: 12 },
  emptySubtext: { fontSize: 13, color: Colors.textLight, marginTop: 4 },
  fab: {
    position: 'absolute',
    bottom: 24,
    right: 24,
    width: 56,
    height: 56,
    borderRadius: 28,
    backgroundColor: Colors.accent,
    justifyContent: 'center',
    alignItems: 'center',
    elevation: 4,
  },
});
