import { Platform } from 'react-native';
import { checkPermission } from './check';
import { requestPermission } from './request';
import { PERMISSIONS } from 'react-native-permissions';

export const checkAndRequestPermissions = async () => {
  if (Platform.OS === 'android') {
    const have_coarse_location = await checkPermission(
      PERMISSIONS.ANDROID.ACCESS_COARSE_LOCATION
    );
    const have_fine_location = await checkPermission(
      PERMISSIONS.ANDROID.ACCESS_FINE_LOCATION
    );

    if (have_fine_location && have_coarse_location) {
      console.log('Have all permissions for Wi-Fi');
      return true;
    }

    if (!have_fine_location) {
      return await requestPermission(
        PERMISSIONS.ANDROID.ACCESS_COARSE_LOCATION
      );
    }

    if (!have_coarse_location) {
      return await requestPermission(PERMISSIONS.ANDROID.ACCESS_FINE_LOCATION);
    }
  } else {
    const have_location = await checkPermission(
      PERMISSIONS.IOS.LOCATION_WHEN_IN_USE
    );
    if (!have_location) {
      return await requestPermission(PERMISSIONS.IOS.LOCATION_WHEN_IN_USE);
    }
    return have_location;
  }

  console.warn('strange if we are here!');
  return false;
};
