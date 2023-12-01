import type { Permission } from 'react-native';
import { PermissionsAndroid } from 'react-native';

export const requestAndroidPermission = async (permission: Permission) => {
  try {
    const granted = await PermissionsAndroid.request(permission, {
      title: 'Приложению для использования SmartConfig требуется разрешение',
      message:
        'Чтобы подключить прибор, требуется доступ к геолокации (модулю Wi-Fi телефона)',
      buttonNegative: 'Нет',
      buttonPositive: 'Да',
    });
    if (granted === PermissionsAndroid.RESULTS.GRANTED) {
      console.log('You can use the wifi');
      return true;
    } else {
      console.log('wifi permission denied');
      return false;
    }
  } catch (err) {
    console.warn(`Can't request ${permission}: ${err}`);
  }
  return false;
};
