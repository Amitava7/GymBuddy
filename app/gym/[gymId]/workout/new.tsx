import { useState } from 'react';
import { View, Text, TextInput, TouchableOpacity, StyleSheet } from 'react-native';
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
        <Text style={styles.label}>WORKOUT NAME</Text>
        <TextInput
          style={styles.input}
          placeholder="e.g. Push Day, Leg Day..."
          placeholderTextColor={Colors.textLight}
          value={name}
          onChangeText={setName}
          autoFocus
        />
        <TouchableOpacity
          style={[styles.startBtn, !name.trim() && styles.startBtnDisabled]}
          onPress={handleStart}
          disabled={!name.trim()}
          activeOpacity={0.8}
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
  label: { fontSize: 13, fontWeight: '700', color: Colors.textSecondary, marginBottom: 10, letterSpacing: 1 },
  input: {
    borderWidth: 1,
    borderColor: Colors.border,
    borderRadius: 14,
    padding: 18,
    fontSize: 18,
    backgroundColor: Colors.surface,
    color: Colors.text,
    marginBottom: 28,
  },
  startBtn: {
    backgroundColor: Colors.primary,
    paddingVertical: 18,
    borderRadius: 14,
    alignItems: 'center',
  },
  startBtnDisabled: { opacity: 0.3 },
  startBtnText: { color: Colors.background, fontSize: 18, fontWeight: '700' },
});
