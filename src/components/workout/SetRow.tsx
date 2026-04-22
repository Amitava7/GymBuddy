import { View, Text, TextInput, TouchableOpacity, StyleSheet } from 'react-native';
import { useState, useEffect } from 'react';
import { Ionicons } from '@expo/vector-icons';
import { Colors } from '../../constants/colors';
import { SetData } from '../../types/workout';

type Props = {
  set: SetData;
  onUpdate: (setId: number, field: 'kg' | 'reps', value: string) => void;
  onDelete: (setId: number) => void;
};

export function SetRow({ set, onUpdate, onDelete }: Props) {
  const [kgText, setKgText] = useState(set.kg != null ? String(set.kg) : '');
  const [repsText, setRepsText] = useState(set.reps != null ? String(set.reps) : '');

  useEffect(() => {
    if (set.kg == null) setKgText('');
  }, [set.kg]);
  useEffect(() => {
    if (set.reps == null) setRepsText('');
  }, [set.reps]);

  return (
    <View style={styles.setRow}>
      <Text style={styles.setNumber}>{set.set_number}</Text>
      <TextInput
        style={styles.setInput}
        placeholder="0"
        placeholderTextColor={Colors.textLight}
        value={kgText}
        onChangeText={(v) => { setKgText(v); onUpdate(set.id, 'kg', v); }}
        keyboardType="decimal-pad"
      />
      <TextInput
        style={styles.setInput}
        placeholder="0"
        placeholderTextColor={Colors.textLight}
        value={repsText}
        onChangeText={(v) => { setRepsText(v); onUpdate(set.id, 'reps', v); }}
        keyboardType="numeric"
      />
      <TouchableOpacity onPress={() => onDelete(set.id)}>
        <Ionicons name="close-circle-outline" size={20} color={Colors.danger} />
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  setRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 6,
    gap: 4,
  },
  setNumber: {
    flex: 1,
    textAlign: 'center',
    fontSize: 15,
    fontWeight: '600',
    color: Colors.textSecondary,
  },
  setInput: {
    flex: 1,
    borderWidth: 1,
    borderColor: Colors.border,
    borderRadius: 8,
    padding: 10,
    textAlign: 'center',
    fontSize: 16,
    backgroundColor: Colors.background,
    color: Colors.text,
  },
});
