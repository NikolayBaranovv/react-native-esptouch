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

@ReactModule(name = EsptouchModule.NAME)
public class EsptouchModule extends ReactContextBaseJavaModule implements LifecycleEventListener {
  public static final String NAME = "Esptouch";
  private final ReactApplicationContext reactContext;
  private Activity thisActivity;
  private boolean mReceiverRegistered = false; // есть слушатель состояния сети?
  protected EspTouchAsyncTask mTask;  //  Задача настройки Wi-Fi
  private Promise mConfigPromise;
  private final EspTouchBroadcastReceiver mReceiver; //  Мониторинг состояния сети и трансляция изменений GPS-переключателя

  public EsptouchModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
    reactContext.addLifecycleEventListener(this);
    mReceiver = new EspTouchBroadcastReceiver(reactContext, NAME);
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
    Log.d(NAME, "destroy module");
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
    promise.resolve("Успешно подписались на изменения в системе.");
  }

  protected String getEspTouchVersion() {
    return "ESP touch version:" + IEsptouchTask.ESPTOUCH_VERSION;
  }

  @ReactMethod()
  public void initESPTouch(Promise promise) {
    Log.i(NAME, "initESPTouch " + getEspTouchVersion());
    thisActivity = getCurrentActivity();
    if (thisActivity == null) {
      promise.reject("Не смог обратиться к системе");
      return;
    }
    // Android 9 и выше обязан предоставить разрешение на определение местоположения, а затем включить GPS для получения информации о Wi-Fi.
    if (isSDKAtLeastP()) {
      //Если разрешение на определение местоположения не предоставлено
      if (ContextCompat.checkSelfPermission(thisActivity, Manifest.permission.ACCESS_COARSE_LOCATION)
        != PackageManager.PERMISSION_GRANTED) {
        promise.reject("Нужно разрешение на определение местоположения");
      } else {
        registerBroadcastReceiver(promise);
      }
      return;
    } else {
      registerBroadcastReceiver(promise);
      return;
    }
    promise.reject("Не смог инициализировать модуль");
  }

  @ReactMethod
  public void getNetInfo(Promise promise) {
    Log.d(NAME, "try getNetInfo");
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
        promise.reject("Нет данных о состоянии сети");
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
      respondErrorToRTN(-1, "Модуль ESPTouch не готов");
      return;
    }
    if (mReceiver == null) {
      respondErrorToRTN(-1, "Модуль ESPTouch не готов");
      return;
    }

    WiFiStateResult thisStateResult = mReceiver.getWiFiState();

    if (thisStateResult == null) {
      respondErrorToRTN(-2, "Не удалось собрать статистику о сети Wi-Fi");
      return;
    }

    if (thisStateResult.is5G) {
      respondErrorToRTN(-3, "Устройство ZONT не поддерживает 5G Wi-Fi, убедитесь, что подключены к сети 2.4G");
      return;
    }

    if (!thisStateResult.wifiConnected) {
      respondErrorToRTN(-4, "Телефон не подключен к сети Wi-Fi");
      return;
    }

    if (!thisStateResult.permissionGranted) {
      respondErrorToRTN(-5, "Не выдано разрешение на доступ к GPS (нужно для взаимодействия с Wi-Fi)");
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
    mTask = new EspTouchAsyncTask(reactContext, thisActivity, NAME, mConfigPromise);
    mTask.execute(ssid, bssid, password, deviceCount, broadcast);
    mReceiver.setEspTouchTask(mTask);
  }

  @ReactMethod
  public void finish() {
    Log.d(NAME, "finish ESPTouch task");
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
    return NAME;
  }

}
