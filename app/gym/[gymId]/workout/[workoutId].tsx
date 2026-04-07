import { View, ScrollView, TouchableOpacity, Text, StyleSheet } from 'react-native';
import { useLocalSearchParams, Stack } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { Colors } from '../../../../src/constants/colors';
import { useActiveWorkout } from '../../../../src/hooks/useActiveWorkout';
import { useWorkoutTimer } from '../../../../src/hooks/useWorkoutTimer';
import { WorkoutTimer } from '../../../../src/components/workout/WorkoutTimer';
import { ExerciseCard } from '../../../../src/components/workout/ExerciseCard';

export default function ActiveWorkoutScreen() {
  const { gymId, workoutId } = useLocalSearchParams<{ gymId: string; workoutId: string }>();
  const wId = Number(workoutId);
  const gId = Number(gymId);

  const {
    workoutName,
    exercises,
    startTime,
    handleAddExercise,
    handleAddSet,
    handleUpdateSet,
    handleDeleteSet,
    handleDeleteExercise,
    handleToggleComplete,
    handleUpdateNote,
    handleFinish,
  } = useActiveWorkout(wId, gId);

  const elapsed = useWorkoutTimer(startTime);

  return (
    <View style={styles.container}>
      <Stack.Screen options={{ title: workoutName, headerBackVisible: false }} />

      <WorkoutTimer elapsed={elapsed} />

      <ScrollView style={styles.scroll} contentContainerStyle={styles.scrollContent}>
        {exercises.map((ex) => (
          <ExerciseCard
            key={ex.id}
            exercise={ex}
            onToggleComplete={handleToggleComplete}
            onDeleteExercise={handleDeleteExercise}
            onUpdateNote={handleUpdateNote}
            onAddSet={handleAddSet}
            onUpdateSet={handleUpdateSet}
            onDeleteSet={handleDeleteSet}
          />
        ))}

        <TouchableOpacity style={styles.addExerciseBtn} onPress={handleAddExercise} activeOpacity={0.7}>
          <Ionicons name="add-circle-outline" size={20} color={Colors.primary} />
          <Text style={styles.addExerciseText}>Add Exercise</Text>
        </TouchableOpacity>
      </ScrollView>

      <TouchableOpacity style={styles.finishBtn} onPress={handleFinish} activeOpacity={0.8}>
        <Ionicons name="checkmark-done" size={20} color={Colors.background} />
        <Text style={styles.finishBtnText}>Finish Workout</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: Colors.background },
  scroll: { flex: 1 },
  scrollContent: { padding: 20, paddingBottom: 100 },
  addExerciseBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 6,
    paddingVertical: 18,
    backgroundColor: Colors.surface,
    borderRadius: 14,
    borderWidth: 1,
    borderColor: Colors.primary + '30',
    borderStyle: 'dashed',
  },
  addExerciseText: { color: Colors.primary, fontSize: 16, fontWeight: '600' },
  finishBtn: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 8,
    backgroundColor: Colors.primary,
    paddingVertical: 18,
    paddingBottom: 34,
  },
  finishBtnText: { color: Colors.background, fontSize: 18, fontWeight: '700' },
});
