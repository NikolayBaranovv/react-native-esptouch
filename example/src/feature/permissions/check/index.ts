import { RESULTS, check } from 'react-native-permissions';
import type { Permission, PermissionStatus } from 'react-native-permissions';

export const checkPermission = async (
  permission: Permission
): Promise<boolean> => {
  let have_permission = false;
  try {
    const result: PermissionStatus = await check(permission);
    switch (result) {
      case RESULTS.UNAVAILABLE:
        console.error(
          'This feature is not available (on this device / in this context)'
        );
        break;
      case RESULTS.DENIED:
        console.warn(
          'The permission has not been requested / is denied but requestable'
        );
        break;
      case RESULTS.LIMITED:
        console.log('The permission is limited: some actions are possible');
        break;
      case RESULTS.GRANTED:
        console.log('The permission is granted');
        break;
      case RESULTS.BLOCKED:
        console.error('The permission is denied and not requestable anymore');
        break;
    }
    have_permission = result == RESULTS.GRANTED;
  } catch (err) {
    console.error(`Can't check ${permission}: ${err}`);
  }
  return have_permission;
};
