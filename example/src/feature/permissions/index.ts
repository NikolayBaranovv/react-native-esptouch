import { PermissionsAndroid } from 'react-native';
import { checkPermission } from './check';
import { requestAndroidPermission } from './request';

export const checkAndRequestPermissions = async () => {
  const have_coarse_location = await checkPermission(
    PermissionsAndroid.PERMISSIONS.ACCESS_COARSE_LOCATION!
  );
  const have_fine_location = await checkPermission(
    PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION!
  );

  if (have_fine_location && have_coarse_location) {
    console.log('Есть все разрешения');
    return true;
  }

  if (!have_fine_location) {
    return await requestAndroidPermission(
      PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION!
    );
  }

  if (!have_coarse_location) {
    return await requestAndroidPermission(
      PermissionsAndroid.PERMISSIONS.ACCESS_COARSE_LOCATION!
    );
  }
  console.log('strange if we are here!');
  return false;
};
