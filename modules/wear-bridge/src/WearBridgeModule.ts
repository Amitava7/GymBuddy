import { NativeModule, requireOptionalNativeModule } from 'expo';

export type WearNode = {
  id: string;
  displayName: string;
  isNearby: boolean;
};

export type WearMessage = {
  path: string;
  data: string;
  sourceNodeId: string;
};

type WearBridgeEvents = {
  onMessage(message: WearMessage): void;
  onWatchesChanged(event: { count: number }): void;
};

declare class WearBridgeNativeModule extends NativeModule<WearBridgeEvents> {
  getConnectedNodes(): Promise<WearNode[]>;
  getWatchNodes(): Promise<WearNode[]>;
  sendMessage(path: string, data: string): Promise<number>;
}

// Null on platforms without the native module (iOS, web, Expo Go).
export default requireOptionalNativeModule<WearBridgeNativeModule>('WearBridge');
