import { NativeModules, Platform } from 'react-native';

const LINKING_ERROR =
  `The package 'react-native-esptouch' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

const EspTouch = NativeModules.EspTouch
  ? NativeModules.EspTouch
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

export function initESPTouch(): Promise<string> {
  return EspTouch.initESPTouch();
}

export interface NetInfoResult {
  enable: boolean; //включен модуль GPS?
  permissionGranted: boolean;
  wifiConnected: boolean; //подключен к сети Wi-Fi?
  is5G: boolean;
  ssid: string | null;
  bssid: string | null;
  ip: string | null;
  message: string;
}
export function getNetInfo(): Promise<NetInfoResult> {
  return EspTouch.getNetInfo();
}

export enum BroadcastType {
  multicast = 0, // последовательно идём по группам получаетелей (x.12.12.12 -> x.13.13.13 -> x.14.14.14 -> ...)
  broadcast = 1, //одновременно всем получателям посылаем (255.255.255.255 for android, x.x.x.0 for ios)
}

export interface SmartConfigResult {
  code: 200 | 0; //success or fail
  msg: string;
  bssid?: string;
  ip?: string;
}
export function startSmartConfig(
  password: string,
  broadcastType: BroadcastType
): Promise<SmartConfigResult> {
  return EspTouch.startSmartConfig(password, broadcastType);
}
