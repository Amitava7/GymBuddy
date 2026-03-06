import { useState, useEffect } from 'react';
import { View, Text, TextInput, TouchableOpacity, StyleSheet } from 'react-native';
import { useLocalSearchParams, useRouter, Stack } from 'expo-router';
import { Colors } from '../../src/constants/colors';
import { createExercise, updateExercise, getExercise } from '../../src/db/database';

export default function ExerciseFormScreen() {
  const { exerciseId } = useLocalSearchParams<{ exerciseId?: string }>();
  const router = useRouter();
  const isEdit = !!exerciseId;

  const [name, setName] = useState('');
  const [details, setDetails] = useState('');

  useEffect(() => {
    if (isEdit) {
      (async () => {
        const ex = await getExercise(Number(exerciseId));
        if (ex) {
          setName(ex.name);
          setDetails(ex.details || '');
        }
      })();
    }
  }, [exerciseId, isEdit]);

  const handleSave = async () => {
    if (!name.trim()) return;
    if (isEdit) {
      await updateExercise(Number(exerciseId), name.trim(), details.trim() || undefined);
    } else {
      await createExercise(name.trim(), details.trim() || undefined);
    }
    router.back();
  };

  return (
    <View style={styles.container}>
      <Stack.Screen options={{ title: isEdit ? 'Edit Exercise' : 'New Exercise' }} />

      <Text style={styles.label}>Exercise Name</Text>
      <TextInput
        style={styles.input}
        placeholder="e.g. Bench Press"
        value={name}
        onChangeText={setName}
        autoFocus
      />

      <Text style={styles.label}>Details / Instructions</Text>
      <TextInput
        style={[styles.input, styles.textArea]}
        placeholder="e.g. Lie flat on bench, grip slightly wider than shoulders..."
        value={details}
        onChangeText={setDetails}
        multiline
        numberOfLines={4}
      />

      <TouchableOpacity
        style={[styles.saveBtn, !name.trim() && styles.saveBtnDisabled]}
        onPress={handleSave}
        disabled={!name.trim()}
      >
        <Text style={styles.saveBtnText}>{isEdit ? 'Update' : 'Create'} Exercise</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: Colors.background, padding: 24 },
  label: { fontSize: 16, fontWeight: '600', color: Colors.text, marginBottom: 8, marginTop: 16 },
  input: {
    borderWidth: 1,
    borderColor: Colors.border,
    borderRadius: 12,
    padding: 14,
    fontSize: 16,
    backgroundColor: Colors.surface,
  },
  textArea: { minHeight: 100, textAlignVertical: 'top' },
  saveBtn: {
    backgroundColor: Colors.primary,
    paddingVertical: 16,
    borderRadius: 12,
    alignItems: 'center',
    marginTop: 32,
  },
  saveBtnDisabled: { opacity: 0.5 },
  saveBtnText: { color: '#fff', fontSize: 18, fontWeight: '700' },
});
