import type { Permission } from 'react-native';
import { PermissionsAndroid } from 'react-native';

export const checkPermission = async (
  permission: Permission
): Promise<boolean> => {
  let have_permission = false;
  try {
    have_permission = await PermissionsAndroid.check(permission);
  } catch (err) {
    console.warn(`Can't check ${permission}: ${err}`);
  }
  return have_permission;
};
