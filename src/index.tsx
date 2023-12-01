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
export function getNetInfo(): Promise<object> {
  return EspTouch.getNetInfo();
}

export enum BroadcastType {
  multicast = 0, // последовательно идём по группам получаетелей (x.12.12.12 -> x.13.13.13 -> x.14.14.14 -> ...)
  broadcast = 1, //одновременно всем получателям посылаем (255.255.255.255 for android, x.x.x.0 for ios)
}
export function startSmartConfig(password: string, broadcastType: BroadcastType): Promise<object> {
  return EspTouch.startSmartConfig(password, broadcastType);
}
