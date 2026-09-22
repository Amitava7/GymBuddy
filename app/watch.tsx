import { useState } from 'react';
import { View, Text, FlatList, TouchableOpacity, TextInput, StyleSheet } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { Colors } from '../src/constants/colors';
import { useWatchConnection, type WatchLogEntry } from '../src/hooks/useWatchConnection';

function describe(entry: WatchLogEntry) {
  const { payload } = entry;
  switch (payload.type) {
    case 'ping':
      return 'Ping';
    case 'pong':
      return 'Pong';
    case 'set_done':
      return `Set done${payload.text ? `: ${payload.text}` : ''}`;
    default:
      return payload.text ?? '';
  }
}

export default function WatchScreen() {
  const { isSupported, connectedNodes, watchNodes, log, error, refresh, send } =
    useWatchConnection();
  const [text, setText] = useState('');

  if (!isSupported) {
    return (
      <View style={styles.center}>
        <Ionicons name="watch-outline" size={48} color={Colors.textLight} />
        <Text style={styles.emptyText}>Watch sync needs the Android dev/native build.</Text>
      </View>
    );
  }

  const watch = watchNodes[0];
  const status = watch
    ? `Connected to ${watch.displayName}`
    : connectedNodes.length > 0
      ? `${connectedNodes[0].displayName} connected, but GymBuddy isn't running/installed on it`
      : 'No watch connected over Bluetooth';

  const handleSendText = () => {
    if (!text.trim()) return;
    send({ type: 'text', text: text.trim() });
    setText('');
  };

  return (
    <View style={styles.container}>
      <View style={styles.statusCard}>
        <Ionicons
          name={watch ? 'watch' : 'watch-outline'}
          size={28}
          color={watch ? Colors.primary : Colors.textSecondary}
        />
        <Text style={styles.statusText}>{status}</Text>
        <TouchableOpacity onPress={refresh}>
          <Ionicons name="refresh" size={22} color={Colors.primary} />
        </TouchableOpacity>
      </View>

      {error && <Text style={styles.errorText}>{error}</Text>}

      <View style={styles.row}>
        <TouchableOpacity style={styles.btn} onPress={() => send({ type: 'ping' })}>
          <Text style={styles.btnText}>Ping watch</Text>
        </TouchableOpacity>
      </View>

      <View style={styles.row}>
        <TextInput
          style={styles.input}
          value={text}
          onChangeText={setText}
          placeholder="Message to watch"
          placeholderTextColor={Colors.textLight}
          onSubmitEditing={handleSendText}
        />
        <TouchableOpacity style={styles.btn} onPress={handleSendText}>
          <Text style={styles.btnText}>Send</Text>
        </TouchableOpacity>
      </View>

      <Text style={styles.sectionTitle}>Messages</Text>
      <FlatList
        data={log}
        keyExtractor={(item) => item.id}
        contentContainerStyle={{ paddingBottom: 20 }}
        ListEmptyComponent={<Text style={styles.emptyText}>No messages yet</Text>}
        renderItem={({ item }) => (
          <View style={styles.logRow}>
            <Ionicons
              name={item.direction === 'in' ? 'arrow-down' : 'arrow-up'}
              size={14}
              color={item.direction === 'in' ? Colors.accent : Colors.primary}
            />
            <Text style={styles.logText}>{describe(item)}</Text>
            <Text style={styles.logTime}>{new Date(item.payload.ts).toLocaleTimeString()}</Text>
          </View>
        )}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: Colors.background, padding: 20, gap: 12 },
  center: {
    flex: 1,
    backgroundColor: Colors.background,
    justifyContent: 'center',
    alignItems: 'center',
    gap: 12,
    padding: 20,
  },
  statusCard: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
    padding: 16,
    borderRadius: 12,
    backgroundColor: Colors.surface,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  statusText: { flex: 1, color: Colors.text, fontSize: 15, fontWeight: '600' },
  errorText: { color: Colors.danger, fontSize: 13 },
  row: { flexDirection: 'row', gap: 8 },
  btn: {
    paddingHorizontal: 16,
    paddingVertical: 12,
    borderRadius: 10,
    backgroundColor: Colors.primary,
    justifyContent: 'center',
  },
  btnText: { color: Colors.background, fontWeight: 'bold' },
  input: {
    flex: 1,
    backgroundColor: Colors.surface,
    color: Colors.text,
    borderRadius: 10,
    borderWidth: 1,
    borderColor: Colors.border,
    paddingHorizontal: 12,
  },
  sectionTitle: { color: Colors.text, fontSize: 18, fontWeight: 'bold', marginTop: 8 },
  emptyText: { color: Colors.textSecondary, textAlign: 'center' },
  logRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    paddingVertical: 8,
    borderBottomWidth: 1,
    borderBottomColor: Colors.border,
  },
  logText: { flex: 1, color: Colors.text },
  logTime: { color: Colors.textSecondary, fontSize: 12 },
});
