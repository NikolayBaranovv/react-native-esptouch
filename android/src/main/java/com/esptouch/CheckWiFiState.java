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
                result.message =  "Приложению необходим доступ к точному местоположению, чтобы получить доступ к модулю Wi-Fi";
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
                result.message = "Пожалуйста, включите GPS модуль, чтобы получить информацию о сети Wi-Fi.";
                result.enable = false;
            }
        }

    }

    private void checkWifi(WifiInfo wifiInfo, WiFiStateResult result) {
        result.wifiConnected = false;
        boolean connected = TouchNetUtil.isWifiConnected(wifiInfo);
        if (!connected) {
            result.message = "Пожалуйста, сначала подключитесь к точке доступа Wi-Fi. Please connect Wi-Fi first.";
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
            result.message = "Вы подключены к сети 5G, устройство ZONT поддерживает работу только в сети 2.4G.  Current Wi-Fi connection is 5G, make sure your device supports it.";
        }

        result.ssid = ssid;
        result.ssidBytes = TouchNetUtil.getRawSsidBytesOrElse(wifiInfo, ssid.getBytes());
        result.bssid = wifiInfo.getBSSID();

        result.enable = result.wifiConnected;
    }
}
