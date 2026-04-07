import { useState, useEffect } from 'react';
import { View, Text, TextInput, TouchableOpacity, StyleSheet, ScrollView } from 'react-native';
import { useLocalSearchParams, useRouter, Stack } from 'expo-router';
import { Colors } from '../../src/constants/colors';
import { createExercise, updateExercise, getExercise, MUSCLE_TAGS, EQUIPMENT_TAGS } from '../../src/db/database';

export default function ExerciseFormScreen() {
  const { exerciseId } = useLocalSearchParams<{ exerciseId?: string }>();
  const router = useRouter();
  const isEdit = !!exerciseId;

  const [name, setName] = useState('');
  const [details, setDetails] = useState('');
  const [selectedTags, setSelectedTags] = useState<string[]>([]);

  useEffect(() => {
    if (isEdit) {
      (async () => {
        const ex = await getExercise(Number(exerciseId));
        if (ex) {
          setName(ex.name);
          setDetails(ex.details || '');
          setSelectedTags(ex.tags ? ex.tags.split(',').filter(Boolean) : []);
        }
      })();
    }
  }, [exerciseId, isEdit]);

  const toggleTag = (tag: string) => {
    setSelectedTags(prev =>
      prev.includes(tag) ? prev.filter(t => t !== tag) : [...prev, tag]
    );
  };

  const handleSave = async () => {
    if (!name.trim()) return;
    const tagsStr = selectedTags.length > 0 ? selectedTags.join(',') : undefined;
    if (isEdit) {
      await updateExercise(Number(exerciseId), name.trim(), details.trim() || undefined, tagsStr);
    } else {
      await createExercise(name.trim(), details.trim() || undefined, tagsStr);
    }
    router.back();
  };

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
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

      <Text style={styles.label}>Muscle Groups</Text>
      <View style={styles.tagRow}>
        {MUSCLE_TAGS.map(tag => (
          <TouchableOpacity
            key={tag}
            style={[styles.tag, selectedTags.includes(tag) && styles.tagSelected]}
            onPress={() => toggleTag(tag)}
            activeOpacity={0.7}
          >
            <Text style={[styles.tagText, selectedTags.includes(tag) && styles.tagTextSelected]}>
              {tag}
            </Text>
          </TouchableOpacity>
        ))}
      </View>

      <Text style={styles.label}>Equipment</Text>
      <View style={styles.tagRow}>
        {EQUIPMENT_TAGS.map(tag => (
          <TouchableOpacity
            key={tag}
            style={[styles.tag, selectedTags.includes(tag) && styles.tagSelected]}
            onPress={() => toggleTag(tag)}
            activeOpacity={0.7}
          >
            <Text style={[styles.tagText, selectedTags.includes(tag) && styles.tagTextSelected]}>
              {tag}
            </Text>
          </TouchableOpacity>
        ))}
      </View>

      <TouchableOpacity
        style={[styles.saveBtn, !name.trim() && styles.saveBtnDisabled]}
        onPress={handleSave}
        disabled={!name.trim()}
        activeOpacity={0.8}
      >
        <Text style={styles.saveBtnText}>{isEdit ? 'Update' : 'Create'} Exercise</Text>
      </TouchableOpacity>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: Colors.background },
  content: { padding: 24, paddingBottom: 48 },
  label: {
    fontSize: 14,
    fontWeight: '600',
    color: Colors.textSecondary,
    marginBottom: 8,
    marginTop: 16,
    textTransform: 'uppercase',
    letterSpacing: 1,
  },
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
  tagRow: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  tag: {
    paddingHorizontal: 14,
    paddingVertical: 8,
    borderRadius: 20,
    borderWidth: 1,
    borderColor: Colors.border,
    backgroundColor: Colors.surface,
  },
  tagSelected: {
    backgroundColor: Colors.primary + '20',
    borderColor: Colors.primary,
  },
  tagText: { fontSize: 14, color: Colors.textSecondary, textTransform: 'capitalize' },
  tagTextSelected: { color: Colors.primary, fontWeight: '600' },
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
