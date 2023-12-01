package com.esptouch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.esptouch.R;


import com.facebook.react.bridge.ReactApplicationContext;

public class EspTouchBroadcastReceiver extends BroadcastReceiver {
    final private ReactApplicationContext reactContext;
    private final String NAME;
    private WiFiStateResult thisStateResult;
    protected EspTouchAsyncTask mTask;

    public EspTouchBroadcastReceiver(ReactApplicationContext reactContext, String name) {
        this.reactContext = reactContext;
        NAME = name;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action == null) {
            return;
        }
        Log.d(NAME, "BroadcastReceiver on Recieve");
        getWifiInfo(intent);
    }

    protected void getWifiInfo(Intent intent) {
        Log.d(NAME, "getWifiInfo");
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
                Log.d(NAME, "NETWORK_STATE_CHANGED_ACTION");
                onWifiChanged(wifiInfo);
                break;
            case LocationManager.PROVIDERS_CHANGED_ACTION:
                Log.d(NAME, "PROVIDERS_CHANGED_ACTION");
                onWifiChanged(wifiManager.getConnectionInfo());
                break;
        }
//    }
    }
    //  Обработка изменений Wi-Fi
    private void onWifiChanged(WifiInfo info) {
        Log.d(NAME, "onWifiChanged");

        WiFiStateResult stateResult = new CheckWiFiState(reactContext).checkState(info);
        thisStateResult = stateResult;
        if (thisStateResult != null) {
            Log.d(NAME, "message " + thisStateResult.message);
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
        if (stateResult.wifiConnected) {
            if (stateResult.is5G) {
                if (thisStateResult != null) {
                    thisStateResult.message = reactContext.getResources().getString(R.string.esptouch_wifi_5g_warning);
                }
            }
        } else {
            if (mTask != null) {
                mTask.cancelEsptouch();
                mTask = null;
            }
        }
    }

  public WiFiStateResult getWiFiState() {
    return thisStateResult;
  }

  public void setEspTouchTask(EspTouchAsyncTask mTask) {
    this.mTask = mTask;
  }
}
