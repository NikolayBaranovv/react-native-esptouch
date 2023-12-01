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
public class EsptouchModule extends ReactContextBaseJavaModule implements LifecycleEventListener,  PermissionListener {
  public static final String NAME = "Esptouch";
  private static final int REQUEST_PERMISSION = 0x01;
  private final ReactApplicationContext reactContext;
  private Activity thisActivity;
  private boolean mDestroyed = false;
  private boolean mReceiverRegistered = false; // есть слушатель состояния сети?
  //  private EsptouchAsyncTask4 mTask;  //  Задача настройки Wi-Fi
  private EspTouchAsyncTask mTask;  //  Задача настройки Wi-Fi
  private Promise mConfigPromise;
  private WiFiStateResult thisStateResult;

  public EsptouchModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
    reactContext.addLifecycleEventListener(this);
    Log.d(NAME, "1. initialize module");
  }



  protected void getWifiInfo(Intent intent) {
    Log.i(NAME, "getWifiInfo");
//    TODO: change to new API
//     Попробовал использовать новый метод, ничего не получилось. Вместо wifiInfo приходит null

//    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//      Log.i(NAME, "go to new");
//      ConnectivityManager connectivityManager = reactContext.getSystemService(ConnectivityManager.class);
//
//      Network currentNetwork = connectivityManager.getActiveNetwork();
//      NetworkCapabilities netCaps = connectivityManager.getNetworkCapabilities(currentNetwork);
//      WifiInfo info = (WifiInfo) netCaps.getTransportInfo();
//      String ssid = info.getSSID();
//      Log.i(NAME, "currentNetwork " + ssid);
//
//
//      final NetworkRequest request =
//        new NetworkRequest.Builder()
//          .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
//          .build();
//      //      ConnectivityManager.NetworkCallback.FLAG_INCLUDE_LOCATION_INFO
//      final ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
//        @Override
//        public void onAvailable(Network network) {
//          Log.i(NAME, "ConnectivityManager onAvailable");
//        }
//        @Override
//        public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
//          Log.i(NAME, "onCapabilitiesChanged");
//          WifiInfo wifiInfo = (WifiInfo) networkCapabilities.getTransportInfo(); // ВОТ ТУТ null
//          Log.i(NAME, "connectivityManager");
////              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
////                wifiInfo.getWifiStandard();
////              }
////              wifiInfo.getBSSID();
////              wifiInfo.getRssi();
    //              //get ipaddr
//              LinkProperties link = connectivityManager.getLinkProperties(connectivityManager.getActiveNetwork());
//
////                        Log.e("IPAddress List", link.getLinkAddresses().toString());
//
//              // return only one IPv4Address
//              Optional<String> links = link.getLinkAddresses().stream()
//                      .filter(linkAddress -> linkAddress.getAddress().getAddress().length == 4)
//                      .findFirst()
//                      .map(LinkAddress::toString);
//          onWifiChanged(wifiInfo);
//          connectivityManager.unregisterNetworkCallback(this);
//        }
//      };
//      connectivityManager.registerNetworkCallback(request, networkCallback);
//      Log.i(NAME, "connectivityManager.requestNetwork");
//    } else {
    WifiManager wifiManager = (WifiManager) reactContext.getSystemService(Context.WIFI_SERVICE);

    String action = intent.getAction();
    assert wifiManager != null;

    switch (action) {
      case WifiManager.NETWORK_STATE_CHANGED_ACTION:
        WifiInfo wifiInfo;
        if (intent.hasExtra(WifiManager.EXTRA_WIFI_INFO)) {
          wifiInfo = intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);
        } else {
          wifiInfo = wifiManager.getConnectionInfo();
        }
        Log.i(NAME, "NETWORK_STATE_CHANGED_ACTION");
        onWifiChanged(wifiInfo);
        break;
      case LocationManager.PROVIDERS_CHANGED_ACTION:
        Log.i(NAME, "PROVIDERS_CHANGED_ACTION");
        onWifiChanged(wifiManager.getConnectionInfo());
        break;
    }
//    }
  }
  //  Мониторинг состояния сети и трансляция изменений GPS-переключателя
  private BroadcastReceiver mReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      if (action == null) {
        return;
      }
      Log.i(NAME, "BroadcastReceiver on Recieve");
      getWifiInfo(intent);
    }
  };

  private final IEsptouchListener myListener = new IEsptouchListener() {
    @Override
    public void onEsptouchResultAdded(final IEsptouchResult result) {
      Log.i(NAME,result.getBssid() + " is connected to the wifi");
    }
  };
  private class EsptouchAsyncTask4 extends AsyncTask<byte[], Void, List<IEsptouchResult>> {
    private WeakReference<Activity> mActivity;
    private final Object mLock = new Object();
    private IEsptouchTask mEsptouchTask;

    EsptouchAsyncTask4(Activity activity) {
      mActivity = new WeakReference<>(activity);
    }

    void cancelEsptouch() {
      if (mEsptouchTask != null) {
        mEsptouchTask.interrupt();
      }
    }

    @Override
    protected void onPreExecute() {
      //
    }

    @Override
    protected List<IEsptouchResult> doInBackground(byte[]... params) {
      int taskResultCount;
      synchronized (mLock) {
        byte[] apSsid = params[0];
        byte[] apBssid = params[1];
        byte[] apPassword = params[2];
        byte[] deviceCountData = params[3];
        byte[] broadcastData = params[4];
        taskResultCount = deviceCountData.length == 0 ? -1 : Integer.parseInt(new String(deviceCountData));
        mEsptouchTask = new EsptouchTask(apSsid, apBssid, apPassword, reactContext);
        mEsptouchTask.setPackageBroadcast(broadcastData[0] == 1);
        mEsptouchTask.setEsptouchListener(myListener);
      }
      return mEsptouchTask.executeForResults(taskResultCount);
    }

    @Override
    protected void onPostExecute(List<IEsptouchResult> result) {
      if (result == null) {
        Log.i(NAME,"Create Esptouch task failed, the EspTouch port could be used by other thread");
        respondErrorToRTN(-6, "Не смог запустить ESPTouch, порт занят");
        return;
      }

      // check whether the task is cancelled and no results received
      IEsptouchResult firstResult = result.get(0);
      if (!firstResult.isCancelled()) {
        // the task received some results including cancelled while
        // executing before receiving enough results
        if (firstResult.isSuc()) {
          Log.i(NAME,"EspTouch success " + firstResult.getBssid() + " " + firstResult.getInetAddress());
//          respondErrorToRTN(200, "EspTouch succcess");
          WritableMap map = Arguments.createMap();
          map.putInt("code", 200);
          map.putString("msg", "Устройство успешно настроено");
          map.putString("bssid", firstResult.getBssid());
          map.putString("ip", firstResult.getInetAddress().getHostAddress());
          mConfigPromise.resolve(map);

        } else {
          Log.i(NAME, "EspTouch fail");
          respondErrorToRTN(0, "Устройство не найдено");
        }
      }

      mTask = null;
    }

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
    mDestroyed = true;
    if (mReceiverRegistered) {
      reactContext.unregisterReceiver(mReceiver);
    }
    Log.d(NAME, "-1. destroy module");
  }

  /* Обратный вызов после авторизации запроса (после вызова метода requestPermissions, описанного выше, эта функция будет запущена после обратной связи с пользователем). */
  @Override
  public boolean onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    Log.d(NAME, "2. onRequestPermissionsResult");
    if (requestCode == REQUEST_PERMISSION) {
      Log.d(NAME, "2. onRequestPermissionsResult 1 " + requestCode);
      if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        Log.d(NAME, "2. onRequestPermissionsResult PERMISSION_GRANTED");
        if (!mDestroyed) {
          registerBroadcastReceiver();
        }
        return true;
      } else {
        //TODO: send error to UI
//        new AlertDialog.Builder(reactContext)
//          .setTitle("⚠️Warning")
//          .setMessage("On Android M or higher version, App can\'t get Wi-Fi connection information if you forbid Location permission.")
//          .setCancelable(false)
//          .setPositiveButton(android.R.string.ok, (dialog, which) -> finish())
//          .show();
        Log.d(NAME, "2. onRequestPermissionsResult show dialog");
      }
    }
    Log.d(NAME, "2. onRequestPermissionsResult false");
    return false;
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

  //  Обработка изменений Wi-Fi
  private void onWifiChanged(WifiInfo info) {
    Log.d(NAME, "4. onWifiChanged");

    WiFiStateResult stateResult = new CheckWiFiState(reactContext).checkState(info);
    thisStateResult = stateResult;
    if (thisStateResult != null) {
      Log.w(NAME, "message " + thisStateResult.message);
      Log.d(NAME, "enable " + thisStateResult.enable);
      Log.d(NAME, "permissionGranted " + thisStateResult.permissionGranted);
      Log.d(NAME, "wifiConnected " + thisStateResult.wifiConnected);
      Log.d(NAME, "is5G " + thisStateResult.is5G);
      Log.d(NAME, "ssid " + thisStateResult.ssid);
      if (thisStateResult.address != null) {
        Log.d(NAME, "ip " + thisStateResult.address.getHostAddress());
      }
      Log.d(NAME, "bssid " + thisStateResult.bssid);

    }
    CharSequence message = stateResult.message;
    boolean confirmEnable = false;
    if (stateResult.wifiConnected) {
      confirmEnable = true;
      if (stateResult.is5G) {
        message = "Current Wi-Fi connection is 5G, make sure your device supports it.";
      }
    } else {
      if (mTask != null) {
        mTask.cancelEsptouch();
        mTask = null;
      }
    }
    return;
  }
  protected String getEspTouchVersion() {
    return "ESP touch version:" + IEsptouchTask.ESPTOUCH_VERSION;
  }
  private void respondErrorToRTN(int code, String msg) {
    if (mConfigPromise != null) {
      mConfigPromise.reject(Integer.toString(code), msg);
    }
  }

  @ReactMethod
  public void startSmartConfig(String pwd, int broadcastType, Promise promise) {
    Log.d(NAME,"startSmartConfig");
    mConfigPromise = promise;
    if (!mReceiverRegistered) {
      respondErrorToRTN(-1, "Модуль ESPTouch не готов");
      return;
    }
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

    Log.w(NAME,"ssid " + thisStateResult.ssid + " pwd " + pwd);

    byte[] ssid = ByteUtil.getBytesByString(thisStateResult.ssid);
    byte[] password = ByteUtil.getBytesByString(pwd);
    byte[] bssid = TouchNetUtil.parseBssid2bytes(thisStateResult.bssid);
    byte[] deviceCount = ByteUtil.getBytesByString("1");
    byte[] broadcast = {(byte) broadcastType}; // 1 broadcast， 0 multicast

    if (mTask != null) {
      mTask.cancelEsptouch();
    }
//    mTask = new EsptouchAsyncTask4(thisActivity);
    mTask = new EspTouchAsyncTask(reactContext, thisActivity, NAME, mConfigPromise);
    mTask.execute(ssid, bssid, password, deviceCount, broadcast);
  }
  @ReactMethod()
  public void initESPTouch(Promise promise) {
    Log.i(NAME,"initESPTouch");
    thisActivity = getCurrentActivity();
    PermissionAwareActivity activity = (PermissionAwareActivity) getCurrentActivity();
    if (thisActivity == null) {
      Log.i(NAME,"initESPTouch 2 activity is null!!!");
      return;
    }
    // FIXME: this logic to UI
    // Проблема №29 гласит, что Android 9 должен предоставить разрешение на определение местоположения, а затем включить GPS для получения информации о Wi-Fi.
    if (isSDKAtLeastP()) {
      Log.d(NAME,"isSDKAtLeastP");
      //Если разрешение на определение местоположения не предоставлено
      if (ContextCompat.checkSelfPermission(thisActivity, Manifest.permission.ACCESS_COARSE_LOCATION)
        != PackageManager.PERMISSION_GRANTED) {
        Log.d(NAME,"we no have permission, try request");
        // Определите, требуются ли инструкции по авторизации
        Log.d(NAME,"try request permission 2");
        // Инициировать запрос авторизации
        String[] permissions = {Manifest.permission.ACCESS_COARSE_LOCATION};
        activity.requestPermissions(permissions, REQUEST_PERMISSION, this);
      } else {
        Log.d(NAME,"registerBroadcastReceiver 1");
        promise.resolve("success registerBroadcastReceiver!");
        registerBroadcastReceiver();
      }
    } else {
      Log.d(NAME,"registerBroadcastReceiver 2");
      promise.resolve("success registerBroadcastReceiver! 2");
      registerBroadcastReceiver();
    }
    promise.reject("can't start broadcast receiver");
  }
  @ReactMethod
  public void getNetInfo(Promise promise) {
    Log.d(NAME,"try getNetInfo");

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
    }
    Log.d(NAME,"finish getNetInfo");
  }

  @ReactMethod
  public void finish() {
    mConfigPromise = null;
    if (mTask != null) {
      mTask.cancelEsptouch();
    }
    if (mReceiverRegistered) {
      reactContext.unregisterReceiver(mReceiver);
      Log.i(NAME,"config finished and unregisterReceiver");
    }
    mReceiverRegistered = false;
  }

  @Override
  @NonNull
  public String getName() {
    return NAME;
  }

}
