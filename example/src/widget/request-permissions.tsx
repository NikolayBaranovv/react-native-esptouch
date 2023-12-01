import { Button, StyleSheet, Text, View } from 'react-native';
import type { Dispatch, SetStateAction } from 'react';
import { useEffect, useReducer, useState } from 'react';
import { checkAndRequestPermissions } from '../feature/permissions';
import { initESPTouch } from 'react-native-esptouch';

interface RequestPermissionsProps {
  setReady: Dispatch<SetStateAction<boolean>>;
}

export function RequestPermissions(props: RequestPermissionsProps) {
  const { setReady } = props;
  const [have_permission, setPermission] = useState<boolean>(false);
  const [x, forceUpdate] = useReducer((x) => x + 1, 0);

  useEffect(() => {
    checkAndRequestPermissions().then(setPermission);
  }, [x]);

  useEffect(() => {
    if (have_permission) {
      initESPTouch().then((res) => {
        console.log(res);
        setReady(true);
      });
    }
  }, [have_permission]);

  if (have_permission) {
    return null;
  }

  return (
    <View style={styles.container}>
      {!have_permission && (
        <Text style={{ color: 'red' }}>Нет разрешения для работы с Wi-Fi </Text>
      )}
      {!have_permission && (
        <Button title="Запросить разрешение" onPress={forceUpdate} />
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    display: 'flex',
    gap: 16,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
