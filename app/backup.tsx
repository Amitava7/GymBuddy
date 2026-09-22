import { useState } from 'react';
import { View, Text, TouchableOpacity, StyleSheet, Alert, ActivityIndicator } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { Colors } from '../src/constants/colors';
import {
  exportBackup,
  pickBackupFile,
  importBackup,
  summarizeBackup,
  BackupData,
  ImportMode,
  ImportSummary,
} from '../src/db/backup';

function describe(s: ImportSummary) {
  return `${s.gyms} gyms, ${s.exercises} exercises, ${s.templates} templates, ${s.workouts} workouts, ${s.sets} sets`;
}

export default function BackupScreen() {
  const [busy, setBusy] = useState(false);

  const handleExport = async () => {
    setBusy(true);
    try {
      await exportBackup();
    } catch (e) {
      Alert.alert('Export failed', e instanceof Error ? e.message : String(e));
    } finally {
      setBusy(false);
    }
  };

  const runImport = (backup: BackupData, mode: ImportMode) => {
    try {
      const result = importBackup(backup, mode);
      Alert.alert('Import complete', `Added ${describe(result)}.`);
    } catch (e) {
      Alert.alert('Import failed', e instanceof Error ? e.message : String(e));
    }
  };

  const handleImport = async () => {
    let backup: BackupData | null;
    setBusy(true);
    try {
      backup = await pickBackupFile();
    } catch (e) {
      Alert.alert('Import failed', e instanceof Error ? e.message : String(e));
      return;
    } finally {
      setBusy(false);
    }
    if (!backup) return;

    const picked = backup;
    Alert.alert(
      'Import Data',
      `Backup from ${new Date(picked.exported_at).toLocaleString()}\n\n${describe(summarizeBackup(picked))}\n\nMerge keeps your current data and adds anything new. Replace deletes everything first.`,
      [
        { text: 'Cancel', style: 'cancel' },
        { text: 'Merge', onPress: () => runImport(picked, 'merge') },
        {
          text: 'Replace',
          style: 'destructive',
          onPress: () =>
            Alert.alert('Replace all data?', 'All current gyms, exercises and workouts will be deleted.', [
              { text: 'Cancel', style: 'cancel' },
              { text: 'Replace', style: 'destructive', onPress: () => runImport(picked, 'replace') },
            ]),
        },
      ]
    );
  };

  return (
    <View style={styles.container}>
      <TouchableOpacity style={styles.card} onPress={handleExport} disabled={busy} activeOpacity={0.7}>
        <View style={styles.cardIcon}>
          <Ionicons name="share-outline" size={24} color={Colors.primary} />
        </View>
        <View style={styles.cardContent}>
          <Text style={styles.cardTitle}>Export Data</Text>
          <Text style={styles.cardSubtitle}>
            Save gyms, exercises, templates and workout history to a JSON file
          </Text>
        </View>
      </TouchableOpacity>

      <TouchableOpacity style={styles.card} onPress={handleImport} disabled={busy} activeOpacity={0.7}>
        <View style={styles.cardIcon}>
          <Ionicons name="download-outline" size={24} color={Colors.primary} />
        </View>
        <View style={styles.cardContent}>
          <Text style={styles.cardTitle}>Import Data</Text>
          <Text style={styles.cardSubtitle}>Restore from a GymBuddy backup file</Text>
        </View>
      </TouchableOpacity>

      {busy && <ActivityIndicator style={styles.spinner} size="large" color={Colors.primary} />}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: Colors.background, padding: 20 },
  card: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: Colors.surface,
    borderRadius: 14,
    padding: 16,
    marginBottom: 10,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  cardIcon: {
    width: 44,
    height: 44,
    borderRadius: 12,
    backgroundColor: Colors.primary + '15',
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 14,
  },
  cardContent: { flex: 1 },
  cardTitle: { fontSize: 16, fontWeight: '600', color: Colors.text },
  cardSubtitle: { fontSize: 13, color: Colors.textSecondary, marginTop: 2 },
  spinner: { marginTop: 24 },
});
