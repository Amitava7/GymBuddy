import { useEffect } from 'react';
import { View, Text, ActivityIndicator, StyleSheet } from 'react-native';
import { Stack } from 'expo-router';
import { StatusBar } from 'expo-status-bar';
import { useMigrations } from 'drizzle-orm/expo-sqlite/migrator';
import { Colors } from '../src/constants/colors';
import { db } from '../src/db/database';
import migrations from '../src/db/drizzle/migrations';
import { seedExercisesIfNeeded } from '../src/db/seed';

export default function RootLayout() {
  const { success, error } = useMigrations(db, migrations);

  useEffect(() => {
    if (success) {
      seedExercisesIfNeeded();
    }
  }, [success]);

  if (error) {
    return (
      <View style={migrationStyles.container}>
        <Text style={migrationStyles.errorText}>Migration error: {error.message}</Text>
      </View>
    );
  }

  if (!success) {
    return (
      <View style={migrationStyles.container}>
        <ActivityIndicator size="large" color={Colors.primary} />
      </View>
    );
  }

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
        <Stack.Screen name="watch" options={{ title: 'Watch' }} />
      </Stack>
    </>
  );
}

const migrationStyles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: Colors.background,
    justifyContent: 'center',
    alignItems: 'center',
  },
  errorText: {
    color: Colors.danger,
    fontSize: 16,
    textAlign: 'center',
    padding: 20,
  },
});
