import { useCallback, useState } from 'react';
import {
  View,
  Text,
  FlatList,
  TouchableOpacity,
  StyleSheet,
  TextInput,
  Alert,
  Pressable,
} from 'react-native';
import { useRouter, useFocusEffect } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { Colors } from '../src/constants/colors';
import { getGyms, createGym, deleteGym } from '../src/db/database';

type Gym = { id: number; name: string; location: string | null };

export default function HomeScreen() {
  const router = useRouter();
  const [gyms, setGyms] = useState<Gym[]>([]);
  const [showForm, setShowForm] = useState(false);
  const [name, setName] = useState('');
  const [location, setLocation] = useState('');

  const loadGyms = useCallback(async () => {
    const data = await getGyms();
    setGyms(data);
  }, []);

  useFocusEffect(
    useCallback(() => {
      loadGyms();
    }, [loadGyms])
  );

  const handleCreate = async () => {
    if (!name.trim()) return;
    await createGym(name.trim(), location.trim() || undefined);
    setName('');
    setLocation('');
    setShowForm(false);
    loadGyms();
  };

  const handleDelete = (gym: Gym) => {
    Alert.alert('Delete Gym', `Delete "${gym.name}"? This will remove all related workouts.`, [
      { text: 'Cancel', style: 'cancel' },
      {
        text: 'Delete',
        style: 'destructive',
        onPress: async () => {
          await deleteGym(gym.id);
          loadGyms();
        },
      },
    ]);
  };

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.subtitle}>Select your gym to get started</Text>
        <View style={styles.headerButtons}>
          <TouchableOpacity
            style={styles.headerBtn}
            onPress={() => router.push('/exercises')}
          >
            <Ionicons name="barbell-outline" size={20} color={Colors.primary} />
            <Text style={styles.headerBtnText}>Exercises</Text>
          </TouchableOpacity>
        </View>
      </View>

      {gyms.length === 0 && !showForm && (
        <View style={styles.empty}>
          <Ionicons name="fitness-outline" size={64} color={Colors.textLight} />
          <Text style={styles.emptyText}>No gyms yet</Text>
          <Text style={styles.emptySubtext}>Add your first gym to get started</Text>
        </View>
      )}

      <FlatList
        data={gyms}
        keyExtractor={(item) => item.id.toString()}
        contentContainerStyle={styles.list}
        renderItem={({ item }) => (
          <TouchableOpacity
            style={styles.card}
            onPress={() => router.push(`/gym/${item.id}`)}
            onLongPress={() => handleDelete(item)}
          >
            <View style={styles.cardIcon}>
              <Ionicons name="business" size={28} color={Colors.primary} />
            </View>
            <View style={styles.cardContent}>
              <Text style={styles.cardTitle}>{item.name}</Text>
              {item.location ? (
                <Text style={styles.cardSubtitle}>{item.location}</Text>
              ) : null}
            </View>
            <Ionicons name="chevron-forward" size={24} color={Colors.textLight} />
          </TouchableOpacity>
        )}
      />

      {showForm && (
        <View style={styles.formOverlay}>
          <View style={styles.form}>
            <Text style={styles.formTitle}>Add Gym</Text>
            <TextInput
              style={styles.input}
              placeholder="Gym name"
              value={name}
              onChangeText={setName}
              autoFocus
            />
            <TextInput
              style={styles.input}
              placeholder="Location (optional)"
              value={location}
              onChangeText={setLocation}
            />
            <View style={styles.formButtons}>
              <Pressable style={styles.cancelBtn} onPress={() => setShowForm(false)}>
                <Text style={styles.cancelBtnText}>Cancel</Text>
              </Pressable>
              <Pressable style={styles.saveBtn} onPress={handleCreate}>
                <Text style={styles.saveBtnText}>Add</Text>
              </Pressable>
            </View>
          </View>
        </View>
      )}

      <TouchableOpacity style={styles.fab} onPress={() => setShowForm(true)}>
        <Ionicons name="add" size={28} color="#fff" />
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: Colors.background },
  header: {
    padding: 16,
    paddingBottom: 8,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  subtitle: { fontSize: 15, color: Colors.textSecondary },
  headerButtons: { flexDirection: 'row', gap: 8 },
  headerBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 16,
    backgroundColor: Colors.surface,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  headerBtnText: { fontSize: 13, color: Colors.primary, fontWeight: '600' },
  list: { padding: 16, paddingTop: 8 },
  card: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: Colors.surface,
    borderRadius: 12,
    padding: 16,
    marginBottom: 12,
    elevation: 2,
    shadowColor: Colors.cardShadow,
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
  },
  cardIcon: {
    width: 48,
    height: 48,
    borderRadius: 24,
    backgroundColor: Colors.primary + '15',
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 12,
  },
  cardContent: { flex: 1 },
  cardTitle: { fontSize: 17, fontWeight: '600', color: Colors.text },
  cardSubtitle: { fontSize: 13, color: Colors.textSecondary, marginTop: 2 },
  empty: { alignItems: 'center', marginTop: 80 },
  emptyText: { fontSize: 18, fontWeight: '600', color: Colors.textSecondary, marginTop: 16 },
  emptySubtext: { fontSize: 14, color: Colors.textLight, marginTop: 4 },
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
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.25,
    shadowRadius: 4,
  },
  formOverlay: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: 'rgba(0,0,0,0.5)',
    justifyContent: 'center',
    alignItems: 'center',
    padding: 24,
  },
  form: {
    backgroundColor: Colors.surface,
    borderRadius: 16,
    padding: 24,
    width: '100%',
    maxWidth: 400,
  },
  formTitle: { fontSize: 20, fontWeight: '700', marginBottom: 16, color: Colors.text },
  input: {
    borderWidth: 1,
    borderColor: Colors.border,
    borderRadius: 8,
    padding: 12,
    fontSize: 16,
    marginBottom: 12,
  },
  formButtons: { flexDirection: 'row', justifyContent: 'flex-end', gap: 12, marginTop: 8 },
  cancelBtn: { paddingHorizontal: 20, paddingVertical: 10, borderRadius: 8 },
  cancelBtnText: { color: Colors.textSecondary, fontSize: 16 },
  saveBtn: {
    paddingHorizontal: 20,
    paddingVertical: 10,
    borderRadius: 8,
    backgroundColor: Colors.primary,
  },
  saveBtnText: { color: '#fff', fontSize: 16, fontWeight: '600' },
});
