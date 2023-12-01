import { StyleSheet, View } from 'react-native';
import { RequestPermissions } from '../widget/request-permissions';
import { NetInfo } from '../widget/net-info';
import { useState } from 'react';
import { SmartConfig } from '../widget/run-smartconfig';

export function MainPage() {
  const [espTouch_ready, setReady] = useState<boolean>(false);
  return (
    <View style={styles.container}>
      <RequestPermissions setReady={setReady} />
      <NetInfo ready={espTouch_ready} />
      <SmartConfig />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    display: 'flex',
    flex: 1,
    gap: 16,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
