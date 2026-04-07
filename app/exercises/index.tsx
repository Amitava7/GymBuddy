import { useCallback, useState } from 'react';
import {
  View,
  Text,
  FlatList,
  TouchableOpacity,
  TextInput,
  StyleSheet,
  Alert,
  ScrollView,
} from 'react-native';
import { useRouter, useFocusEffect } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { Colors } from '../../src/constants/colors';
import { getExercises, deleteExercise, MUSCLE_TAGS, EQUIPMENT_TAGS } from '../../src/db/database';

const ALL_FILTER_TAGS = [...MUSCLE_TAGS, ...EQUIPMENT_TAGS];

export default function ExercisesScreen() {
  const router = useRouter();
  const [exercises, setExercises] = useState<any[]>([]);
  const [search, setSearch] = useState('');
  const [activeTags, setActiveTags] = useState<string[]>([]);

  const load = useCallback(async () => {
    const data = await getExercises(search || undefined, activeTags.length > 0 ? activeTags : undefined);
    setExercises(data);
  }, [search, activeTags]);

  useFocusEffect(useCallback(() => { load(); }, [load]));

  const toggleTag = (tag: string) => {
    setActiveTags(prev =>
      prev.includes(tag) ? prev.filter(t => t !== tag) : [...prev, tag]
    );
  };

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
        <Ionicons name="search" size={18} color={Colors.textLight} />
        <TextInput
          style={styles.searchInput}
          placeholder="Search exercises..."
          placeholderTextColor={Colors.textLight}
          value={search}
          onChangeText={setSearch}
        />
        {search ? (
          <TouchableOpacity onPress={() => setSearch('')}>
            <Ionicons name="close-circle" size={18} color={Colors.textLight} />
          </TouchableOpacity>
        ) : null}
      </View>

      <ScrollView
        horizontal
        showsHorizontalScrollIndicator={false}
        contentContainerStyle={styles.tagScroll}
        style={{ flexGrow: 0 }}
      >
        {ALL_FILTER_TAGS.map(tag => (
          <TouchableOpacity
            key={tag}
            style={[styles.filterTag, activeTags.includes(tag) && styles.filterTagActive]}
            onPress={() => toggleTag(tag)}
            activeOpacity={0.7}
          >
            <Text style={[styles.filterTagText, activeTags.includes(tag) && styles.filterTagTextActive]}>
              {tag.charAt(0).toUpperCase() + tag.slice(1)}
            </Text>
          </TouchableOpacity>
        ))}
      </ScrollView>

      {exercises.length === 0 ? (
        <View style={styles.empty}>
          <Ionicons name="barbell-outline" size={48} color={Colors.textLight} />
          <Text style={styles.emptyText}>
            {search || activeTags.length > 0 ? 'No exercises found' : 'No exercises yet'}
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
              activeOpacity={0.7}
            >
              <View style={styles.cardIcon}>
                <Ionicons name="barbell" size={20} color={Colors.primary} />
              </View>
              <View style={styles.cardContent}>
                <Text style={styles.cardTitle}>{item.name}</Text>
                {item.tags ? (
                  <View style={styles.tagPills}>
                    {item.tags.split(',').map((t: string) => (
                      <View key={t} style={styles.pill}>
                        <Text style={styles.pillText}>{t}</Text>
                      </View>
                    ))}
                  </View>
                ) : item.details ? (
                  <Text style={styles.cardSubtitle} numberOfLines={1}>
                    {item.details}
                  </Text>
                ) : null}
              </View>
              <Ionicons name="chevron-forward" size={18} color={Colors.textLight} />
            </TouchableOpacity>
          )}
        />
      )}

      <TouchableOpacity
        style={styles.fab}
        onPress={() => router.navigate('/exercises/form')}
        activeOpacity={0.8}
      >
        <Ionicons name="add" size={28} color={Colors.background} />
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
    margin: 20,
    marginBottom: 8,
    borderRadius: 12,
    paddingHorizontal: 14,
    gap: 10,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  searchInput: { flex: 1, paddingVertical: 12, fontSize: 16, color: Colors.text },
  tagScroll: { paddingHorizontal: 20, paddingBottom: 0, gap: 8, flexDirection: 'row', alignItems: 'center' },
  filterTag: {
    paddingHorizontal: 14,
    paddingVertical: 6,
    borderRadius: 20,
    height: 30,
    borderWidth: 1,
    marginVertical: 5,
    borderColor: Colors.border,
    backgroundColor: Colors.surface,
    alignItems: 'center',
    justifyContent: 'center',
  },
  filterTagActive: {
    backgroundColor: Colors.primary + '20',
    borderColor: Colors.primary,
  },
  filterTagText: { fontSize: 13, color: Colors.textSecondary },
  filterTagTextActive: { color: Colors.primary, fontWeight: '600' },
  list: { paddingHorizontal: 20, paddingTop: 4, paddingBottom: 100 },
  card: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: Colors.surface,
    borderRadius: 14,
    padding: 14,
    marginBottom: 8,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  cardIcon: {
    width: 40,
    height: 40,
    borderRadius: 10,
    backgroundColor: Colors.primary + '15',
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 12,
  },
  cardContent: { flex: 1 },
  cardTitle: { fontSize: 16, fontWeight: '600', color: Colors.text },
  cardSubtitle: { fontSize: 13, color: Colors.textSecondary, marginTop: 2 },
  tagPills: { flexDirection: 'row', flexWrap: 'wrap', gap: 4, marginTop: 4 },
  pill: {
    paddingHorizontal: 8,
    paddingVertical: 2,
    borderRadius: 10,
    backgroundColor: Colors.primary + '15',
  },
  pillText: { fontSize: 11, color: Colors.primary, textTransform: 'capitalize' },
  empty: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  emptyText: { fontSize: 16, fontWeight: '600', color: Colors.textSecondary, marginTop: 12 },
  emptySubtext: { fontSize: 13, color: Colors.textLight, marginTop: 4 },
  fab: {
    position: 'absolute',
    bottom: 28,
    right: 24,
    width: 56,
    height: 56,
    borderRadius: 16,
    backgroundColor: Colors.primary,
    justifyContent: 'center',
    alignItems: 'center',
    elevation: 4,
    shadowColor: Colors.primary,
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.3,
    shadowRadius: 8,
  },
});
