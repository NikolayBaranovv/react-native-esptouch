package com.esptouch;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.net.wifi.WifiInfo;
import android.os.*;

import android.util.Log;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.location.LocationManagerCompat;

import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.esptouch.CheckWiFiState;
import com.esptouch.EspTouchAsyncTask;
import com.esptouch.WiFiStateResult;
import com.esptouch.EspTouchBroadcastReceiver;
import com.espressif.iot.esptouch.EsptouchTask;
import com.espressif.iot.esptouch.IEsptouchTask;
import com.espressif.iot.esptouch.IEsptouchListener;
import com.espressif.iot.esptouch.IEsptouchResult;
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
  private static final int REQUEST_PERMISSION = 0x01;
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
    Log.d(NAME, "-1. destroy module");
  }

  private boolean isSDKAtLeastP() {
    return Build.VERSION.SDK_INT >= 28;
  }

  /* Подписка изменений в системе */
  private void registerBroadcastReceiver() {
    if (mReceiverRegistered) {
      return;
    }
    mReceiverRegistered = true;
    Log.d(NAME, "3. registerBroadcastReceiver");
    IntentFilter filter = new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION);
    if (isSDKAtLeastP()) {
      filter.addAction(LocationManager.PROVIDERS_CHANGED_ACTION);
    }
    reactContext.registerReceiver(mReceiver, filter);
  }

  protected String getEspTouchVersion() {
    return "ESP touch version:" + IEsptouchTask.ESPTOUCH_VERSION;
  }

  @ReactMethod()
  public void initESPTouch(Promise promise) {
    Log.i(NAME, "initESPTouch");
    thisActivity = getCurrentActivity();
    PermissionAwareActivity activity = (PermissionAwareActivity) getCurrentActivity();
    if (thisActivity == null) {
      Log.i(NAME, "initESPTouch 2 activity is null!!!");
      return;
    }
    // FIXME: this logic to UI
    // Проблема №29 гласит, что Android 9 должен предоставить разрешение на определение местоположения, а затем включить GPS для получения информации о Wi-Fi.
    if (isSDKAtLeastP()) {
      Log.d(NAME, "isSDKAtLeastP");
      //Если разрешение на определение местоположения не предоставлено
      if (ContextCompat.checkSelfPermission(thisActivity, Manifest.permission.ACCESS_COARSE_LOCATION)
        != PackageManager.PERMISSION_GRANTED) {
        Log.d(NAME, "we no have permission, try request");
        // Определите, требуются ли инструкции по авторизации
        Log.d(NAME, "try request permission 2");
        // Инициировать запрос авторизации
//        String[] permissions = {Manifest.permission.ACCESS_COARSE_LOCATION};
//        activity.requestPermissions(permissions, REQUEST_PERMISSION, this);
      } else {
        Log.d(NAME, "registerBroadcastReceiver 1");
        promise.resolve("success registerBroadcastReceiver!");
        registerBroadcastReceiver();
      }
    } else {
      Log.d(NAME, "registerBroadcastReceiver 2");
      promise.resolve("success registerBroadcastReceiver! 2");
      registerBroadcastReceiver();
    }
    promise.reject("can't start broadcast receiver");
  }

  @ReactMethod
  public void getNetInfo(Promise promise) {
    Log.d(NAME, "try getNetInfo");


    if (mReceiver != null) {
      WiFiStateResult thisStateResult = mReceiver.getWiFiState();
      if (thisStateResult != null) {

        WritableMap map = Arguments.createMap();
        Log.d(NAME, "enable " + thisStateResult.enable);
        map.putBoolean("enable", thisStateResult.enable);

        Log.d(NAME, "permissionGranted " + thisStateResult.permissionGranted);
        map.putBoolean("permissionGranted", thisStateResult.permissionGranted);

        Log.d(NAME, "wifiConnected " + thisStateResult.wifiConnected);
        map.putBoolean("wifiConnected", thisStateResult.wifiConnected);

        Log.d(NAME, "is5G " + thisStateResult.is5G);
        map.putBoolean("is5G", thisStateResult.is5G);

        Log.d(NAME, "ssid " + thisStateResult.ssid);
        map.putString("ssid", thisStateResult.ssid); //Название сети
        if (thisStateResult.address != null) {
          Log.d(NAME, "ip " + thisStateResult.address.getHostAddress());
          map.putString("ip", thisStateResult.address.getHostAddress());
        }

        Log.d(NAME, "bssid " + thisStateResult.bssid);
        map.putString("bssid", thisStateResult.bssid); //мак адрес точки доступа

        if (thisStateResult.message != null) {
          Log.d(NAME, "message " + thisStateResult.message);
          map.putString("message", thisStateResult.message.toString());
        }
        promise.resolve(map);
      } else {
        Log.d(NAME, "thisStateResult is null!");
      }
    }
    Log.d(NAME, "finish getNetInfo");
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

    Log.w(NAME, "ssid " + thisStateResult.ssid + " pwd " + pwd);

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
    mConfigPromise = null;
    if (mTask != null) {
      mTask.cancelEsptouch();
    }
    if (mReceiverRegistered) {
      reactContext.unregisterReceiver(mReceiver);
      Log.i(NAME, "config finished and unregisterReceiver");
    }
    mReceiverRegistered = false;
  }

  @Override
  @NonNull
  public String getName() {
    return NAME;
  }

}
