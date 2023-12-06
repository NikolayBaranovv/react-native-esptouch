import { RESULTS, request } from 'react-native-permissions';
import type {
  Permission,
  PermissionStatus,
  Rationale,
} from 'react-native-permissions';
import { Platform } from 'react-native';
export const requestPermission = async (permission: Permission) => {
  let rationale: Rationale | undefined = undefined;
  if (Platform.OS === 'android') {
    rationale = {
      title: 'Приложению для использования SmartConfig требуется разрешение',
      message:
        'Чтобы подключить прибор, требуется доступ к геолокации (модулю Wi-Fi телефона)',
      buttonNegative: 'Нет',
      buttonPositive: 'Да',
    };
  }

  try {
    const result: PermissionStatus = await request(permission, rationale);
    if (result === RESULTS.GRANTED) {
      console.log('You can use the wifi');
      return true;
    } else {
      console.warn('wifi permission denied');
      return false;
    }
  } catch (err) {
    console.error(`Can't request ${permission}: ${err}`);
  }
  return false;
};
