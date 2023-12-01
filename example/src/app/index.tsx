import { BroadcastType, startSmartConfig } from 'react-native-esptouch';
import { MainPage } from '../pages';

const requestStartSmartConfig = async () => {
  try {
    const result = await startSmartConfig('123456789', BroadcastType.broadcast);
    console.log('requestStartSmartConfig', result);
  } catch (err) {
    console.warn('requestStartSmartConfig', err);
  }
};
export default function App() {
  return <MainPage />;
}
