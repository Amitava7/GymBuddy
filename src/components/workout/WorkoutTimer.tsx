import { View, Text, StyleSheet } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { Colors } from '../../constants/colors';
import { formatTime } from '../../hooks/useWorkoutTimer';

type Props = {
  elapsed: number;
};

export function WorkoutTimer({ elapsed }: Props) {
  return (
    <View style={styles.timer}>
      <Ionicons name="time-outline" size={18} color={Colors.primary} />
      <Text style={styles.timerText}>{formatTime(elapsed)}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  timer: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 6,
    paddingVertical: 12,
    backgroundColor: Colors.surface,
    borderBottomWidth: 1,
    borderBottomColor: Colors.border,
  },
  timerText: {
    fontSize: 20,
    fontWeight: '700',
    color: Colors.primary,
    fontVariant: ['tabular-nums'],
  },
});
