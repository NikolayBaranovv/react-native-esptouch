package com.esptouch;

import android.Manifest;
import android.app.Activity;
import android.content.IntentFilter;
import android.location.LocationManager;

import android.util.Log;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.esptouch.EspTouchAsyncTask;
import com.esptouch.EspTouchBroadcastReceiver;
import com.esptouch.WiFiStateResult;
import com.esptouch.R;
import com.espressif.iot.esptouch.IEsptouchTask;
import com.espressif.iot.esptouch.util.TouchNetUtil;
import com.espressif.iot.esptouch.util.ByteUtil;

import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.modules.core.PermissionListener;
import com.facebook.react.modules.core.PermissionAwareActivity;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.WritableMap;

@ReactModule(name = EspTouchModule.TAG)
public class EspTouchModule extends ReactContextBaseJavaModule implements LifecycleEventListener {
  public static final String TAG = "EspTouch";
  private final ReactApplicationContext reactContext;
  private Activity thisActivity;
  private boolean mReceiverRegistered = false; // есть слушатель состояния сети?
  protected EspTouchAsyncTask mTask;  //  Задача настройки Wi-Fi
  private Promise mConfigPromise;
  private final EspTouchBroadcastReceiver mReceiver; //  Мониторинг состояния сети и трансляция изменений GPS-переключателя

  public EspTouchModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
    reactContext.addLifecycleEventListener(this);
    mReceiver = new EspTouchBroadcastReceiver(reactContext, TAG);
  }

  @Override
  public void onHostResume() {
    // Activity `onResume`
  }

  @Override
  public void onHostPause() {
    // Activity `onPause`
  }

  @Override
  public void onHostDestroy() {
    if (mReceiverRegistered) {
      reactContext.unregisterReceiver(mReceiver);
    }
    Log.d(TAG, "destroy module");
  }

  private String getLocalizedString(int str_from_resource) {
    return reactContext.getResources().getString(str_from_resource);
  }

  private boolean isSDKAtLeastP() {
    return Build.VERSION.SDK_INT >= 28;
  }

  /* Подписка изменений в системе */
  private void registerBroadcastReceiver(Promise promise) {
    if (mReceiverRegistered) {
      return;
    }
    mReceiverRegistered = true;
    IntentFilter filter = new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION);
    if (isSDKAtLeastP()) {
      filter.addAction(LocationManager.PROVIDERS_CHANGED_ACTION);
    }
    reactContext.registerReceiver(mReceiver, filter);

    promise.resolve(getLocalizedString(R.string.esptouch_create_broadcast_receiver));
  }

  protected String getEspTouchVersion() {
    return reactContext.getResources().getString(R.string.esptouch_about_version, IEsptouchTask.ESPTOUCH_VERSION);
  }

  @ReactMethod()
  public void initESPTouch(Promise promise) {
    Log.i(TAG, "init " + getEspTouchVersion());
    thisActivity = getCurrentActivity();
    if (thisActivity == null) {
      promise.reject(getLocalizedString(R.string.esptouch_activity_error));
      return;
    }
    // Android 9 и выше обязан предоставить разрешение на определение местоположения, а затем включить GPS для получения информации о Wi-Fi.
    if (isSDKAtLeastP()) {
      //Если разрешение на определение местоположения не предоставлено
      if (ContextCompat.checkSelfPermission(thisActivity, Manifest.permission.ACCESS_COARSE_LOCATION)
        != PackageManager.PERMISSION_GRANTED) {
        promise.reject(getLocalizedString(R.string.esptouch_permission_error));
      } else {
        registerBroadcastReceiver(promise);
      }
      return;
    } else {
      registerBroadcastReceiver(promise);
      return;
    }
  }

  @ReactMethod
  public void getNetInfo(Promise promise) {
    Log.d(TAG, "try getNetInfo");
    if (mReceiver != null) {
      WiFiStateResult thisStateResult = mReceiver.getWiFiState();
      if (thisStateResult != null) {
        WritableMap map = Arguments.createMap();
        map.putBoolean("enable", thisStateResult.enable);
        map.putBoolean("permissionGranted", thisStateResult.permissionGranted);
        map.putBoolean("wifiConnected", thisStateResult.wifiConnected);
        map.putBoolean("is5G", thisStateResult.is5G);
        map.putString("ssid", thisStateResult.ssid); //Название сети
        map.putString("bssid", thisStateResult.bssid); //мак адрес точки доступа
        if (thisStateResult.address != null) {
          map.putString("ip", thisStateResult.address.getHostAddress());
        }
        if (thisStateResult.message != null) {
          map.putString("message", thisStateResult.message.toString());
        }
        promise.resolve(map);
      } else {
        promise.reject(getLocalizedString(R.string.esptouch_get_wifi_state_error));
      }
    }
  }

  private void respondErrorToRTN(int code, String msg) {
    if (mConfigPromise != null) {
      mConfigPromise.reject(Integer.toString(code), msg);
    }
  }


  @ReactMethod
  public void startSmartConfig(String pwd, int broadcastType, Promise promise) {
    mConfigPromise = promise;
    if (!mReceiverRegistered) {
      respondErrorToRTN(-1, getLocalizedString(R.string.esptouch_not_ready_error));
      return;
    }
    if (mReceiver == null) {
      respondErrorToRTN(-1, getLocalizedString(R.string.esptouch_not_ready_error));
      return;
    }

    WiFiStateResult thisStateResult = mReceiver.getWiFiState();

    if (thisStateResult == null) {
      respondErrorToRTN(-2, getLocalizedString(R.string.esptouch_get_wifi_state_error));
      return;
    }

    if (thisStateResult.is5G) {
      respondErrorToRTN(-3, getLocalizedString(R.string.esptouch_wifi_5g_warning));
      return;
    }

    if (!thisStateResult.wifiConnected) {
      respondErrorToRTN(-4, getLocalizedString(R.string.esptouch_no_wifi_connection_error));
      return;
    }

    if (!thisStateResult.permissionGranted) {
      respondErrorToRTN(-5, getLocalizedString(R.string.esptouch_permission_error));
      return;
    }

    byte[] ssid = ByteUtil.getBytesByString(thisStateResult.ssid);
    byte[] password = ByteUtil.getBytesByString(pwd);
    byte[] bssid = TouchNetUtil.parseBssid2bytes(thisStateResult.bssid);
    byte[] deviceCount = ByteUtil.getBytesByString("1");
    byte[] broadcast = {(byte) broadcastType}; // 1 broadcast， 0 multicast

    if (mTask != null) {
      mTask.cancelEsptouch();
    }
    mTask = new EspTouchAsyncTask(reactContext, thisActivity, TAG, mConfigPromise);
    mTask.execute(ssid, bssid, password, deviceCount, broadcast);
    mReceiver.setEspTouchTask(mTask);
  }

  @ReactMethod
  public void finish() {
    Log.d(TAG, "finish ESPTouch task");
    mConfigPromise = null;
    if (mTask != null) {
      mTask.cancelEsptouch();
    }
    if (mReceiverRegistered) {
      reactContext.unregisterReceiver(mReceiver);
    }
    mReceiverRegistered = false;
  }

  @Override
  @NonNull
  public String getName() {
    return TAG;
  }

}
