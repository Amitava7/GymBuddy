import { View, Text, TextInput, TouchableOpacity, StyleSheet } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { Colors } from '../../constants/colors';
import { SetData } from '../../types/workout';

type Props = {
  set: SetData;
  onUpdate: (setId: number, field: 'kg' | 'reps', value: string) => void;
  onDelete: (setId: number) => void;
};

export function SetRow({ set, onUpdate, onDelete }: Props) {
  return (
    <View style={styles.setRow}>
      <Text style={styles.setNumber}>{set.set_number}</Text>
      <TextInput
        style={styles.setInput}
        placeholder="0"
        placeholderTextColor={Colors.textLight}
        value={set.kg != null ? String(set.kg) : ''}
        onChangeText={(v) => onUpdate(set.id, 'kg', v)}
        keyboardType="numeric"
      />
      <TextInput
        style={styles.setInput}
        placeholder="0"
        placeholderTextColor={Colors.textLight}
        value={set.reps != null ? String(set.reps) : ''}
        onChangeText={(v) => onUpdate(set.id, 'reps', v)}
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
