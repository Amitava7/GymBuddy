import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { Colors } from '../src/constants/colors';

export default function RootLayout() {
  return (
    <>
      <StatusBar style="light" />
      <Stack
        screenOptions={{
          headerStyle: { backgroundColor: Colors.background },
          headerTintColor: Colors.text,
          headerTitleStyle: { fontWeight: 'bold', color: Colors.text },
          contentStyle: { backgroundColor: Colors.background },
          headerShadowVisible: false,
        }}
      >
        <Stack.Screen name="index" options={{ headerShown: false }} />
        <Stack.Screen name="gym/[gymId]/index" options={{ title: 'Gym' }} />
        <Stack.Screen name="gym/[gymId]/workout/new" options={{ title: 'New Workout' }} />
        <Stack.Screen name="gym/[gymId]/workout/[workoutId]" options={{ title: 'Workout' }} />
        <Stack.Screen name="gym/[gymId]/workout/history" options={{ title: 'History' }} />
        <Stack.Screen name="gym/[gymId]/workout/summary/[workoutId]" options={{ title: 'Workout Summary' }} />
        <Stack.Screen name="exercises/index" options={{ title: 'Exercises' }} />
        <Stack.Screen name="exercises/[exerciseId]" options={{ title: 'Exercise Details' }} />
        <Stack.Screen name="exercises/form" options={{ title: 'Exercise' }} />
        <Stack.Screen name="exercises/pick" options={{ title: 'Pick Exercise', presentation: 'modal' }} />
      </Stack>
    </>
  );
}
