import { useCallback, useEffect, useState } from 'react';
import WearBridge, { type WearNode } from '../../modules/wear-bridge';

// Shared with the watch app (wear/app/src/main/java/com/gymbuddy/wear/PhoneConnection.kt).
export const WEAR_MESSAGE_PATH = '/gymbuddy/msg';

export type WatchPayload = {
  type: 'ping' | 'pong' | 'text' | 'set_done';
  text?: string;
  ts: number;
};

export type WatchLogEntry = {
  id: string;
  direction: 'in' | 'out';
  payload: WatchPayload;
};

const MAX_LOG = 50;

export function useWatchConnection() {
  const [connectedNodes, setConnectedNodes] = useState<WearNode[]>([]);
  const [watchNodes, setWatchNodes] = useState<WearNode[]>([]);
  const [log, setLog] = useState<WatchLogEntry[]>([]);
  const [error, setError] = useState<string | null>(null);

  const addLog = useCallback((direction: WatchLogEntry['direction'], payload: WatchPayload) => {
    const entry = { id: `${direction}-${payload.ts}-${Math.random()}`, direction, payload };
    setLog((prev) => [entry, ...prev].slice(0, MAX_LOG));
  }, []);

  const refresh = useCallback(async () => {
    if (!WearBridge) return;
    try {
      const [connected, watches] = await Promise.all([
        WearBridge.getConnectedNodes(),
        WearBridge.getWatchNodes(),
      ]);
      setConnectedNodes(connected);
      setWatchNodes(watches);
      setError(null);
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    }
  }, []);

  const send = useCallback(
    async (payload: Omit<WatchPayload, 'ts'>) => {
      if (!WearBridge) return;
      const full: WatchPayload = { ...payload, ts: Date.now() };
      try {
        const reached = await WearBridge.sendMessage(WEAR_MESSAGE_PATH, JSON.stringify(full));
        if (reached === 0) {
          setError('No GymBuddy watch reachable. Is the watch app installed and connected?');
          return;
        }
        setError(null);
        addLog('out', full);
      } catch (e) {
        setError(e instanceof Error ? e.message : String(e));
      }
    },
    [addLog]
  );

  useEffect(() => {
    if (!WearBridge) return;
    refresh();
    const messageSub = WearBridge.addListener('onMessage', (message) => {
      if (message.path !== WEAR_MESSAGE_PATH) return;
      let payload: WatchPayload;
      try {
        payload = JSON.parse(message.data);
      } catch {
        payload = { type: 'text', text: message.data, ts: Date.now() };
      }
      addLog('in', payload);
      if (payload.type === 'ping') send({ type: 'pong' });
    });
    const watchesSub = WearBridge.addListener('onWatchesChanged', refresh);
    // Capability changes aren't always pushed promptly, so poll as a fallback.
    const interval = setInterval(refresh, 5000);
    return () => {
      messageSub.remove();
      watchesSub.remove();
      clearInterval(interval);
    };
  }, [refresh, send, addLog]);

  return {
    isSupported: WearBridge != null,
    connectedNodes,
    watchNodes,
    log,
    error,
    refresh,
    send,
  };
}
