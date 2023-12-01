import { Button, StyleSheet, Text, View } from 'react-native';
import type { NetInfoResult } from 'react-native-esptouch';
import { getNetInfo } from 'react-native-esptouch';
import { useCallback, useEffect, useState } from 'react';

interface NetInfoViewProps {
  info: NetInfoResult | null;
}
const NetInfoView = ({ info }: NetInfoViewProps) => {
  if (info == null) {
    return <Text style={{ color: 'red' }}>Состояние сети не получено</Text>;
  }
  const {
    ip,
    is5G,
    bssid,
    message,
    ssid,
    permissionGranted,
    enable,
    wifiConnected,
  } = info;

  return (
    <View style={styles.container}>
      {permissionGranted === false && (
        <Text style={{ color: 'blue' }}>
          Требуется разрешение для использования Wi-Fi
        </Text>
      )}
      {enable === false && (
        <Text style={{ color: 'blue' }}>Включите GPS-модуль</Text>
      )}
      {wifiConnected === false && (
        <Text style={{ color: 'blue' }}>Подключитесь к сети Wi-Fi</Text>
      )}
      {is5G === true && (
        <Text style={{ color: 'blue' }}>
          Поддерживаются только Wi-Fi сети 2.4G
        </Text>
      )}
      {message != '' && message != null && (
        <Text style={{ color: 'red' }}>Сообщение: {message}</Text>
      )}
      {ssid != '' && <Text>Название сети (SSID): {ssid}</Text>}
      {bssid != '' && <Text>BSSID роутера: {bssid}</Text>}
      {ip != '' && <Text>IP-адрес телефона: {ip}</Text>}
    </View>
  );
};

interface NetInfoProps {
  ready: boolean;
}
export function NetInfo(props: NetInfoProps) {
  const { ready } = props;
  const [info, setInfo] = useState<NetInfoResult | null>(null);
  const [error, setError] = useState(null);

  const requestNetInfo = useCallback(() => {
    getNetInfo()
      .then(setInfo)
      .catch((err) => {
        console.error(err);
        setError(err[0]);
      });
  }, []);

  useEffect(() => {
    if (ready) {
      setTimeout(requestNetInfo, 500);
    }
  }, [ready]);
  return (
    <View style={styles.container}>
      <NetInfoView info={info} />
      {error && <Text style={{ color: 'red' }}>{error}</Text>}
      <Button
        title="Запросить состояние сети"
        onPress={requestNetInfo}
        color="#2ABABE"
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    display: 'flex',
    gap: 8,
  },
});
