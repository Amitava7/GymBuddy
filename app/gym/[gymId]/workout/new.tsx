import { useState } from 'react';
import { View, Text, TextInput, TouchableOpacity, StyleSheet, Pressable } from 'react-native';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { Colors } from '../../../../src/constants/colors';
import { startWorkout } from '../../../../src/db/database';

export default function NewWorkoutScreen() {
  const { gymId } = useLocalSearchParams<{ gymId: string }>();
  const router = useRouter();
  const [name, setName] = useState('');

  const handleStart = async () => {
    if (!name.trim()) return;
    const workoutId = await startWorkout(name.trim(), Number(gymId));
    router.replace(`/gym/${gymId}/workout/${workoutId}`);
  };

  return (
    <View style={styles.container}>
      <View style={styles.form}>
        <Text style={styles.label}>Workout Name</Text>
        <TextInput
          style={styles.input}
          placeholder="e.g. Push Day, Leg Day..."
          value={name}
          onChangeText={setName}
          autoFocus
        />
        <TouchableOpacity
          style={[styles.startBtn, !name.trim() && styles.startBtnDisabled]}
          onPress={handleStart}
          disabled={!name.trim()}
        >
          <Text style={styles.startBtnText}>Start Workout</Text>
        </TouchableOpacity>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: Colors.background, padding: 24 },
  form: { marginTop: 20 },
  label: { fontSize: 16, fontWeight: '600', color: Colors.text, marginBottom: 8 },
  input: {
    borderWidth: 1,
    borderColor: Colors.border,
    borderRadius: 12,
    padding: 16,
    fontSize: 18,
    backgroundColor: Colors.surface,
    marginBottom: 24,
  },
  startBtn: {
    backgroundColor: Colors.success,
    paddingVertical: 16,
    borderRadius: 12,
    alignItems: 'center',
  },
  startBtnDisabled: { opacity: 0.5 },
  startBtnText: { color: '#fff', fontSize: 18, fontWeight: '700' },
});
