import {
  ActivityIndicator,
  Button,
  Modal,
  Pressable,
  StyleSheet,
  Switch,
  Text,
  TextInput,
  View,
} from 'react-native';
import type { SmartConfigResult } from 'react-native-esptouch';
import { BroadcastType, startSmartConfig } from 'react-native-esptouch';
import { useCallback, useState } from 'react';

export function SmartConfig() {
  const [password, setPassword] = useState<string>('');
  const [result, setResult] = useState<SmartConfigResult | null>(null);
  const [broadcast_type, setBroadcastType] = useState<BroadcastType>(
    BroadcastType.broadcast
  );
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);
  const [showResultDialog, setShowResultDialog] = useState(false);

  const requestSmartConfig = useCallback(() => {
    setLoading(true);
    setError(null);
    startSmartConfig(password, BroadcastType.broadcast)
      .then((res) => {
        setResult(res);
        setShowResultDialog(true);
        console.log('result', result);
      })
      .catch((err) => {
        console.error(err);
        setError(err[0]);
      })
      .finally(() => setLoading(false));
  }, [password]);

  return (
    <View style={styles.container}>
      <TextInput
        value={password}
        onChangeText={setPassword}
        placeholder="Введите пароль сети"
      />
      <View style={styles.container}>
        <Text>
          {broadcast_type == BroadcastType.broadcast
            ? 'Широковещательный (broadcast)'
            : 'Многоадерсная передача (multicast)'}
        </Text>
        <Switch
          trackColor={{ false: '#767577', true: '#81b0ff' }}
          thumbColor={
            broadcast_type == BroadcastType.broadcast ? '#f5dd4b' : '#f4f3f4'
          }
          ios_backgroundColor="#3e3e3e"
          onValueChange={() => {
            setBroadcastType(
              broadcast_type == BroadcastType.broadcast
                ? BroadcastType.multicast
                : BroadcastType.broadcast
            );
          }}
          value={broadcast_type == BroadcastType.broadcast}
        />
      </View>
      {error && <Text style={{ color: 'red' }}>{error}</Text>}
      <Button
        title="Подключить устройство"
        onPress={requestSmartConfig}
        color="#F9A825"
      />

      {loading && <ActivityIndicator size="large" color="#0000ff" />}
      <Modal
        animationType="slide"
        transparent={true}
        visible={showResultDialog}
        onRequestClose={() => {
          setShowResultDialog(!showResultDialog);
        }}
      >
        <View style={styles.centeredView}>
          <View style={styles.modalView}>
            <Text style={styles.modalText}>{result?.msg}</Text>
            <Pressable
              style={[
                styles.button,
                result?.code === 200 ? styles.buttonSuccess : styles.buttonFail,
              ]}
              onPress={() => setShowResultDialog(!showResultDialog)}
            >
              <Text style={styles.textStyle}>Принял</Text>
            </Pressable>
          </View>
        </View>
      </Modal>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    display: 'flex',
    gap: 8,
  },
  centeredView: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    marginTop: 22,
  },
  modalView: {
    margin: 20,
    backgroundColor: 'white',
    borderRadius: 20,
    padding: 35,
    alignItems: 'center',
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 2,
    },
    shadowOpacity: 0.25,
    shadowRadius: 4,
    elevation: 5,
  },
  button: {
    borderRadius: 20,
    padding: 10,
    elevation: 2,
  },
  buttonSuccess: {
    backgroundColor: '#43a138',
  },
  buttonFail: {
    backgroundColor: '#d52a2a',
  },
  textStyle: {
    color: 'white',
    fontWeight: 'bold',
    textAlign: 'center',
  },
  modalText: {
    marginBottom: 15,
    textAlign: 'center',
  },
});
