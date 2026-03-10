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
import { useLocalSearchParams, useRouter, useFocusEffect, Stack } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { Colors } from '../../../src/constants/colors';
import * as db from '../../../src/db/database';

export default function GymScreen() {
  const { gymId } = useLocalSearchParams<{ gymId: string }>();
  const router = useRouter();
  const id = Number(gymId);

  const [gymName, setGymName] = useState('');
  const [templates, setTemplates] = useState<any[]>([]);
  const [showForm, setShowForm] = useState(false);
  const [templateName, setTemplateName] = useState('');

  const load = useCallback(async () => {
    const gyms = await db.getGyms();
    const gym = gyms.find((g) => g.id === id);
    if (gym) setGymName(gym.name);
    const t = await db.getWorkoutTemplates(id);
    setTemplates(t);
  }, [id]);

  useFocusEffect(useCallback(() => { load(); }, [load]));

  const handleCreateTemplate = async () => {
    if (!templateName.trim()) return;
    await db.createWorkoutTemplate(templateName.trim(), id);
    setTemplateName('');
    setShowForm(false);
    load();
  };

  const handleStartFromTemplate = async (template: any) => {
    const workoutId = await db.startWorkout(template.name, id, template.id);
    const exercises = await db.getTemplateExercises(template.id);
    for (const ex of exercises) {
      const weId = await db.addWorkoutExercise(workoutId, ex.exercise_id, ex.sort_order);
      const lastData = await db.getLastWorkoutDataForExercise(ex.exercise_id);
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
    }
    router.push(`/gym/${id}/workout/${workoutId}`);
  };

  const handleDeleteTemplate = (template: any) => {
    Alert.alert('Delete Template', `Delete "${template.name}"?`, [
      { text: 'Cancel', style: 'cancel' },
      {
        text: 'Delete',
        style: 'destructive',
        onPress: async () => {
          await db.deleteWorkoutTemplate(template.id);
          load();
        },
      },
    ]);
  };

  return (
    <View style={styles.container}>
      <Stack.Screen options={{ title: gymName }} />

      <View style={styles.actions}>
        <TouchableOpacity
          style={styles.actionBtn}
          onPress={() => router.navigate(`/gym/${id}/workout/new`)}
        >
          <Ionicons name="add-circle" size={22} color="#fff" />
          <Text style={styles.actionBtnText}>New Workout</Text>
        </TouchableOpacity>
        <TouchableOpacity
          style={[styles.actionBtn, { backgroundColor: Colors.primaryDark }]}
          onPress={() => router.navigate(`/gym/${id}/workout/history`)}
        >
          <Ionicons name="time" size={22} color="#fff" />
          <Text style={styles.actionBtnText}>History</Text>
        </TouchableOpacity>
      </View>

      <View style={styles.section}>
        <View style={styles.sectionHeader}>
          <Text style={styles.sectionTitle}>Saved Workouts</Text>
          <TouchableOpacity onPress={() => setShowForm(true)}>
            <Ionicons name="add-circle-outline" size={24} color={Colors.primary} />
          </TouchableOpacity>
        </View>

        {templates.length === 0 ? (
          <View style={styles.empty}>
            <Text style={styles.emptyText}>No saved workouts yet</Text>
            <Text style={styles.emptySubtext}>
              Finish a workout or create a template to save it here
            </Text>
          </View>
        ) : (
          <FlatList
            data={templates}
            keyExtractor={(item) => item.id.toString()}
            renderItem={({ item }) => (
              <TouchableOpacity
                style={styles.card}
                onPress={() => handleStartFromTemplate(item)}
                onLongPress={() => handleDeleteTemplate(item)}
              >
                <Ionicons name="document-text" size={24} color={Colors.primary} />
                <View style={styles.cardContent}>
                  <Text style={styles.cardTitle}>{item.name}</Text>
                  <Text style={styles.cardSubtitle}>Tap to start this workout</Text>
                </View>
                <Ionicons name="play-circle" size={28} color={Colors.success} />
              </TouchableOpacity>
            )}
          />
        )}
      </View>

      {showForm && (
        <View style={styles.formOverlay}>
          <View style={styles.form}>
            <Text style={styles.formTitle}>New Workout Template</Text>
            <TextInput
              style={styles.input}
              placeholder="Workout name (e.g. Push Day)"
              value={templateName}
              onChangeText={setTemplateName}
              autoFocus
            />
            <View style={styles.formButtons}>
              <Pressable style={styles.cancelBtn} onPress={() => setShowForm(false)}>
                <Text style={styles.cancelBtnText}>Cancel</Text>
              </Pressable>
              <Pressable style={styles.saveBtn} onPress={handleCreateTemplate}>
                <Text style={styles.saveBtnText}>Create</Text>
              </Pressable>
            </View>
          </View>
        </View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: Colors.background },
  actions: {
    flexDirection: 'row',
    padding: 16,
    gap: 12,
  },
  actionBtn: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
    backgroundColor: Colors.primary,
    paddingVertical: 14,
    borderRadius: 12,
    elevation: 2,
  },
  actionBtnText: { color: '#fff', fontSize: 16, fontWeight: '600' },
  section: { flex: 1, padding: 16, paddingTop: 0 },
  sectionHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 12,
  },
  sectionTitle: { fontSize: 18, fontWeight: '700', color: Colors.text },
  card: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: Colors.surface,
    borderRadius: 12,
    padding: 16,
    marginBottom: 10,
    gap: 12,
    elevation: 1,
  },
  cardContent: { flex: 1 },
  cardTitle: { fontSize: 16, fontWeight: '600', color: Colors.text },
  cardSubtitle: { fontSize: 12, color: Colors.textSecondary, marginTop: 2 },
  empty: { alignItems: 'center', marginTop: 40 },
  emptyText: { fontSize: 16, fontWeight: '600', color: Colors.textSecondary },
  emptySubtext: { fontSize: 13, color: Colors.textLight, marginTop: 4, textAlign: 'center' },
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
