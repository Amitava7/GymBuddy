import { Stack } from 'expo-router';
import { Colors } from '../src/constants/colors';

export default function RootLayout() {
  return (
    <Stack
      screenOptions={{
        headerStyle: { backgroundColor: Colors.primary },
        headerTintColor: '#fff',
        headerTitleStyle: { fontWeight: 'bold' },
        contentStyle: { backgroundColor: Colors.background },
      }}
    >
      <Stack.Screen name="index" options={{ title: 'GymBuddy' }} />
      <Stack.Screen name="gym/[gymId]/index" options={{ title: 'Gym' }} />
      <Stack.Screen name="gym/[gymId]/workout/new" options={{ title: 'New Workout' }} />
      <Stack.Screen name="gym/[gymId]/workout/[workoutId]" options={{ title: 'Workout' }} />
      <Stack.Screen name="gym/[gymId]/workout/history" options={{ title: 'Workout History' }} />
      <Stack.Screen name="gym/[gymId]/workout/summary/[workoutId]" options={{ title: 'Workout Summary' }} />
      <Stack.Screen name="exercises/index" options={{ title: 'Exercises' }} />
      <Stack.Screen name="exercises/[exerciseId]" options={{ title: 'Exercise Details' }} />
      <Stack.Screen name="exercises/form" options={{ title: 'Exercise' }} />
      <Stack.Screen name="exercises/pick" options={{ title: 'Pick Exercise', presentation: 'modal' }} />
    </Stack>
  );
}
