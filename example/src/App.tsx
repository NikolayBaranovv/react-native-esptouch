import * as React from 'react';

import {
  StyleSheet,
  View, Text, Button,
  PermissionsAndroid,
  StatusBar,
} from 'react-native';
import { getNetInfo, initESPTouch, startSmartConfig , BroadcastType} from 'react-native-esptouch';

const requestNetInformation = async () => {
  try {
    const result = await getNetInfo();
    console.log('requestNetInformation', result);
  } catch (err) {
    console.warn(err);
  }
}
const requestStartSmartConfig = async () => {
  try {
    const result = await startSmartConfig("123456789", BroadcastType.broadcast);
    console.log('requestStartSmartConfig', result);
  } catch (err) {
    console.warn('requestStartSmartConfig', err);
  }
}
const requestCameraPermission = (setPermission: any) => async () => {
  let have_permission = false;
  try {
    have_permission = await PermissionsAndroid.check(PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION);
  } catch (err) {
    console.warn(err);
  }
  let have_permission2 = false;
  try {
    have_permission2 = await PermissionsAndroid.check(PermissionsAndroid.PERMISSIONS.ACCESS_COARSE_LOCATION);
  } catch (err) {
    console.warn(err);
  }

  console.log('request', have_permission, PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION);
  console.log('request', have_permission2, PermissionsAndroid.PERMISSIONS.ACCESS_COARSE_LOCATION);


  if (have_permission) {
    console.log('Already have fine location');
    setPermission(have_permission);
    return;
  }

  try {
    const granted = await PermissionsAndroid.request(
      PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION,
      {
        title: 'Приложению ZONT для использования smartconfig требуется разрешение',
        message:
          'Чтобы подключить прибор ZONT, требуется доступ к геолокации (модулю Wi-Fi телефона)',
        buttonNeutral: 'Ask Me Later',
        buttonNegative: 'Cancel',
        buttonPositive: 'OK',
      }
    );
    if (granted === PermissionsAndroid.RESULTS.GRANTED) {
      console.log('You can use the wifi');
      setPermission(true);
    } else {
      console.log('wifi permission denied');
    }
  } catch (err) {
    console.warn(err);
  }
};

export default function App() {
  const [result, setResult] = React.useState<number | undefined>(70);
  const [permission, setPermission] = React.useState<boolean>(false);

  React.useEffect(() => {
    if (permission) {
      initESPTouch().then();
    }
  }, [permission]);

  return (
    <View style={styles.container}>
      <Text>Result: {result}</Text>
      <View style={styles.container_button}>
      <Button title='request permissions' onPress={requestCameraPermission(setPermission)} />
      <Button title='request NET INFO' onPress={requestNetInformation} />
      <Button title='start SMART CONFIG' onPress={requestStartSmartConfig} />
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  container_button: {
    display: 'flex',
    gap: 16,
    alignItems: 'center',
    justifyContent: 'center',
  },
  box: {
    width: 60,
    height: 60,
    marginVertical: 20,
  },
});
