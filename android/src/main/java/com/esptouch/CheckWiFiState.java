package com.esptouch;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.wifi.WifiInfo;
import android.os.Build;
import androidx.core.location.LocationManagerCompat;
import com.espressif.iot.esptouch.util.TouchNetUtil;

import com.facebook.react.bridge.ReactApplicationContext;
public class CheckWiFiState {
    final private ReactApplicationContext reactContext;

    /**
     * @param reactContext React-контекст, для доступа к системе
     */
    public CheckWiFiState(ReactApplicationContext reactContext) {
        this.reactContext = reactContext;
    }

    /**
     * @param info данные о состоянии сети от системы
     * @return WiFiStateResult результат исследования состояния сети
     */
    public WiFiStateResult checkState(WifiInfo info) {
        WiFiStateResult result = new WiFiStateResult();

        checkPermission(result);
        if (!result.enable) {
            return result;
        }

        checkLocation(result);
        if (!result.enable) {
            return result;
        }

        checkWifi(info, result);

        return result;
    }

    /**
     * У приложения есть доступ к точному местоположению?
     *
     * @param result результат исследования сети
     */
    private void checkPermission(WiFiStateResult result) {
        result.permissionGranted = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean locationGranted = reactContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
            if (!locationGranted) {

                result.message = reactContext.getResources().getString(R.string.esptouch_permission_error);
                result.permissionGranted = false;
                result.enable = false;
            }
        }
    }

    /**
     * Wi-Fi модуль включён?
     *
     * @param result результат исследования сети
     */
    private void checkLocation(WiFiStateResult result) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            LocationManager manager = reactContext.getSystemService(LocationManager.class);
            boolean enable = manager != null && LocationManagerCompat.isLocationEnabled(manager);
            if (!enable) {
                result.message =  reactContext.getResources().getString(R.string.esptouch_no_gps_connection_error);
                result.enable = false;
            }
        }

    }

    private void checkWifi(WifiInfo wifiInfo, WiFiStateResult result) {
        result.wifiConnected = false;
        boolean connected = TouchNetUtil.isWifiConnected(wifiInfo);
        if (!connected) {
            result.message = reactContext.getResources().getString(R.string.esptouch_no_wifi_connection_error);
            return;
        }

        String ssid = TouchNetUtil.getSsidString(wifiInfo);

        int ipValue = wifiInfo.getIpAddress();

        if (ipValue != 0) {
            result.address = TouchNetUtil.getAddress(wifiInfo.getIpAddress());
        } else {
            result.address = TouchNetUtil.getIPv4Address();
            if (result.address == null) {
                result.address = TouchNetUtil.getIPv6Address();
            }
        }

        result.wifiConnected = true;
        result.message = "";

        result.is5G = TouchNetUtil.is5G(wifiInfo.getFrequency());

        if (result.is5G) {
            result.message = reactContext.getResources().getString(R.string.esptouch_wifi_5g_warning);
        }

        result.ssid = ssid;
        result.ssidBytes = TouchNetUtil.getRawSsidBytesOrElse(wifiInfo, ssid.getBytes());
        result.bssid = wifiInfo.getBSSID();

        result.enable = result.wifiConnected;
    }
}
