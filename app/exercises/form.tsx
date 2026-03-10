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
        placeholderTextColor={Colors.textLight}
        value={name}
        onChangeText={setName}
        autoFocus
      />

      <Text style={styles.label}>Details / Instructions</Text>
      <TextInput
        style={[styles.input, styles.textArea]}
        placeholder="e.g. Lie flat on bench, grip slightly wider than shoulders..."
        placeholderTextColor={Colors.textLight}
        value={details}
        onChangeText={setDetails}
        multiline
        numberOfLines={4}
      />

      <TouchableOpacity
        style={[styles.saveBtn, !name.trim() && styles.saveBtnDisabled]}
        onPress={handleSave}
        disabled={!name.trim()}
        activeOpacity={0.8}
      >
        <Text style={styles.saveBtnText}>{isEdit ? 'Update' : 'Create'} Exercise</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: Colors.background, padding: 24 },
  label: { fontSize: 14, fontWeight: '600', color: Colors.textSecondary, marginBottom: 8, marginTop: 16, textTransform: 'uppercase', letterSpacing: 1 },
  input: {
    borderWidth: 1,
    borderColor: Colors.border,
    borderRadius: 14,
    padding: 16,
    fontSize: 16,
    backgroundColor: Colors.surface,
    color: Colors.text,
  },
  textArea: { minHeight: 100, textAlignVertical: 'top' },
  saveBtn: {
    backgroundColor: Colors.primary,
    paddingVertical: 16,
    borderRadius: 14,
    alignItems: 'center',
    marginTop: 32,
  },
  saveBtnDisabled: { opacity: 0.3 },
  saveBtnText: { color: Colors.background, fontSize: 17, fontWeight: '700' },
});
