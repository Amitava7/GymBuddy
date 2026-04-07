import { View, Text, TextInput, TouchableOpacity, StyleSheet } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { Colors } from '../../constants/colors';
import { WorkoutExercise } from '../../types/workout';
import { SetRow } from './SetRow';

type Props = {
  exercise: WorkoutExercise;
  onToggleComplete: (weId: number, current: number) => void;
  onDeleteExercise: (weId: number, name: string) => void;
  onUpdateNote: (weId: number, note: string) => void;
  onAddSet: (weId: number) => void;
  onUpdateSet: (setId: number, field: 'kg' | 'reps', value: string) => void;
  onDeleteSet: (setId: number) => void;
};

export function ExerciseCard({
  exercise: ex,
  onToggleComplete,
  onDeleteExercise,
  onUpdateNote,
  onAddSet,
  onUpdateSet,
  onDeleteSet,
}: Props) {
  return (
    <View style={[styles.exerciseCard, ex.is_completed === 1 && styles.exerciseCompleted]}>
      <View style={styles.exerciseHeader}>
        <TouchableOpacity onPress={() => onToggleComplete(ex.id, ex.is_completed)}>
          <Ionicons
            name={ex.is_completed ? 'checkmark-circle' : 'ellipse-outline'}
            size={26}
            color={ex.is_completed ? Colors.success : Colors.textLight}
          />
        </TouchableOpacity>
        <Text style={[styles.exerciseName, ex.is_completed === 1 && styles.exerciseNameDone]}>
          {ex.name}
        </Text>
        <TouchableOpacity onPress={() => onDeleteExercise(ex.id, ex.name)}>
          <Ionicons name="trash-outline" size={20} color={Colors.danger} />
        </TouchableOpacity>
      </View>

      <TextInput
        style={styles.noteInput}
        placeholder="Add note..."
        placeholderTextColor={Colors.textLight}
        value={ex.note || ''}
        onChangeText={(text) => onUpdateNote(ex.id, text)}
        multiline
      />

      <View style={styles.setsHeader}>
        <Text style={styles.setHeaderText}>SET</Text>
        <Text style={styles.setHeaderText}>KG</Text>
        <Text style={styles.setHeaderText}>REPS</Text>
        <Text style={{ width: 30 }} />
      </View>

      {ex.sets.map((set) => (
        <SetRow
          key={set.id}
          set={set}
          onUpdate={onUpdateSet}
          onDelete={onDeleteSet}
        />
      ))}

      <TouchableOpacity style={styles.addSetBtn} onPress={() => onAddSet(ex.id)}>
        <Ionicons name="add" size={16} color={Colors.primary} />
        <Text style={styles.addSetText}>Add Set</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  exerciseCard: {
    backgroundColor: Colors.surface,
    borderRadius: 14,
    padding: 16,
    marginBottom: 12,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  exerciseCompleted: {
    borderLeftWidth: 3,
    borderLeftColor: Colors.success,
  },
  exerciseHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    marginBottom: 8,
  },
  exerciseName: {
    fontSize: 17,
    fontWeight: '700',
    color: Colors.text,
    flex: 1,
  },
  exerciseNameDone: {
    textDecorationLine: 'line-through',
    color: Colors.textSecondary,
  },
  noteInput: {
    borderWidth: 1,
    borderColor: Colors.border,
    borderRadius: 10,
    padding: 10,
    fontSize: 14,
    color: Colors.text,
    marginBottom: 12,
    minHeight: 36,
    backgroundColor: Colors.background,
  },
  setsHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 4,
    marginBottom: 6,
  },
  setHeaderText: {
    flex: 1,
    fontSize: 11,
    fontWeight: '700',
    color: Colors.textLight,
    textAlign: 'center',
    letterSpacing: 1,
  },
  addSetBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 4,
    paddingVertical: 8,
    marginTop: 4,
  },
  addSetText: {
    color: Colors.primary,
    fontSize: 14,
    fontWeight: '600',
  },
});
