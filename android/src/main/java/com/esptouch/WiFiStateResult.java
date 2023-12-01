package com.esptouch;

import java.net.InetAddress;

public class WiFiStateResult {
    public CharSequence message = null;
    public boolean enable = true;
    public boolean permissionGranted = false;
    public boolean wifiConnected = false;
    public boolean is5G = false;
    public InetAddress address = null;
    public String ssid = null;
    public byte[] ssidBytes = null;
    public String bssid = null;
}
