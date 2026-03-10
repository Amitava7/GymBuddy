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
import { Logo } from '../src/components/Logo';
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
      <View style={styles.safeTop} />

      <View style={styles.hero}>
        <Logo size="large" />
        <Text style={styles.tagline}>Track. Lift. Repeat.</Text>
      </View>

      <View style={styles.header}>
        <Text style={styles.sectionTitle}>Your Gyms</Text>
        <TouchableOpacity
          style={styles.headerBtn}
          onPress={() => router.navigate('/exercises')}
        >
          <Ionicons name="barbell-outline" size={18} color={Colors.primary} />
          <Text style={styles.headerBtnText}>Exercises</Text>
        </TouchableOpacity>
      </View>

      {gyms.length === 0 && !showForm && (
        <View style={styles.empty}>
          <Ionicons name="fitness-outline" size={48} color={Colors.textLight} />
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
            onPress={() => router.navigate(`/gym/${item.id}`)}
            onLongPress={() => handleDelete(item)}
            activeOpacity={0.7}
          >
            <View style={styles.cardIcon}>
              <Ionicons name="business" size={24} color={Colors.primary} />
            </View>
            <View style={styles.cardContent}>
              <Text style={styles.cardTitle}>{item.name}</Text>
              {item.location ? (
                <Text style={styles.cardSubtitle}>{item.location}</Text>
              ) : null}
            </View>
            <Ionicons name="chevron-forward" size={20} color={Colors.textLight} />
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
              placeholderTextColor={Colors.textLight}
              value={name}
              onChangeText={setName}
              autoFocus
            />
            <TextInput
              style={styles.input}
              placeholder="Location (optional)"
              placeholderTextColor={Colors.textLight}
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

      <TouchableOpacity style={styles.fab} onPress={() => setShowForm(true)} activeOpacity={0.8}>
        <Ionicons name="add" size={28} color={Colors.background} />
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: Colors.background },
  safeTop: { height: 50 },
  hero: {
    alignItems: 'center',
    paddingVertical: 28,
    paddingBottom: 20,
  },
  tagline: {
    fontSize: 13,
    color: Colors.textSecondary,
    letterSpacing: 3,
    marginTop: 10,
    textTransform: 'uppercase',
  },
  header: {
    paddingHorizontal: 20,
    paddingBottom: 8,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: '700',
    color: Colors.text,
  },
  headerBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    paddingHorizontal: 14,
    paddingVertical: 8,
    borderRadius: 20,
    backgroundColor: Colors.surface,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  headerBtnText: { fontSize: 13, color: Colors.primary, fontWeight: '600' },
  list: { paddingHorizontal: 20, paddingTop: 8 },
  card: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: Colors.surface,
    borderRadius: 14,
    padding: 16,
    marginBottom: 10,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  cardIcon: {
    width: 44,
    height: 44,
    borderRadius: 12,
    backgroundColor: Colors.primary + '15',
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 14,
  },
  cardContent: { flex: 1 },
  cardTitle: { fontSize: 16, fontWeight: '600', color: Colors.text },
  cardSubtitle: { fontSize: 13, color: Colors.textSecondary, marginTop: 2 },
  empty: { alignItems: 'center', marginTop: 60 },
  emptyText: { fontSize: 16, fontWeight: '600', color: Colors.textSecondary, marginTop: 14 },
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
  formOverlay: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: 'rgba(0,0,0,0.7)',
    justifyContent: 'center',
    alignItems: 'center',
    padding: 24,
  },
  form: {
    backgroundColor: Colors.surface,
    borderRadius: 20,
    padding: 24,
    width: '100%',
    maxWidth: 400,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  formTitle: { fontSize: 20, fontWeight: '700', marginBottom: 16, color: Colors.text },
  input: {
    borderWidth: 1,
    borderColor: Colors.border,
    borderRadius: 12,
    padding: 14,
    fontSize: 16,
    marginBottom: 12,
    backgroundColor: Colors.background,
    color: Colors.text,
  },
  formButtons: { flexDirection: 'row', justifyContent: 'flex-end', gap: 12, marginTop: 8 },
  cancelBtn: { paddingHorizontal: 20, paddingVertical: 12, borderRadius: 10 },
  cancelBtnText: { color: Colors.textSecondary, fontSize: 16 },
  saveBtn: {
    paddingHorizontal: 24,
    paddingVertical: 12,
    borderRadius: 10,
    backgroundColor: Colors.primary,
  },
  saveBtnText: { color: Colors.background, fontSize: 16, fontWeight: '700' },
});
