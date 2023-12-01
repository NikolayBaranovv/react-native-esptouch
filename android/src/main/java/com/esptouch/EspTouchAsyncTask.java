package com.esptouch;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;
import com.esptouch.R;
import com.espressif.iot.esptouch.EsptouchTask;
import com.espressif.iot.esptouch.IEsptouchListener;
import com.espressif.iot.esptouch.IEsptouchResult;
import com.espressif.iot.esptouch.IEsptouchTask;

import java.lang.ref.WeakReference;
import java.util.List;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.WritableMap;

public class EspTouchAsyncTask extends AsyncTask<byte[], Void, List<IEsptouchResult>> {
    private WeakReference<Activity> mActivity;
    private final Object mLock = new Object();
    private IEsptouchTask mEsptouchTask;
    private final String NAME;
    final private ReactApplicationContext reactContext;
    private final Promise mConfigPromise;

    public EspTouchAsyncTask(ReactApplicationContext reactContext, Activity activity, String name, Promise mConfigPromise) {
        this.reactContext = reactContext;
        mActivity = new WeakReference<>(activity);
        NAME = name;
        this.mConfigPromise = mConfigPromise;
    }
    private final IEsptouchListener myListener = new IEsptouchListener() {
        @Override
        public void onEsptouchResultAdded(final IEsptouchResult result) {
            Log.w(NAME, result.getBssid() + " is connected to the wifi");
        }
    };
    public void cancelEsptouch() {
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
            Log.e(NAME, "Create Esptouch task failed, the EspTouch port could be used by other thread");
            mConfigPromise.reject(Integer.toString(-6), reactContext.getResources().getString(R.string.esptouch_port_busy_error));
            return;
        }

        // check whether the task is cancelled and no results received
        IEsptouchResult firstResult = result.get(0);
        if (!firstResult.isCancelled()) {
            // the task received some results including cancelled while
            // executing before receiving enough results
            if (firstResult.isSuc()) {
                Log.w(NAME,"EspTouch success " + firstResult.getBssid() + " " + firstResult.getInetAddress());
                WritableMap map = Arguments.createMap();
                map.putInt("code", 200);
                map.putString("msg", reactContext.getResources().getString(R.string.esptouch_success_execute_task));
                map.putString("bssid", firstResult.getBssid());
                map.putString("ip", firstResult.getInetAddress().getHostAddress());
                mConfigPromise.resolve(map);

            } else {
                Log.i(NAME, "EspTouch fail");
                WritableMap map = Arguments.createMap();
                map.putInt("code", 0);
                map.putString("msg", reactContext.getResources().getString(R.string.esptouch_failed));
                mConfigPromise.resolve(map);
            }
        }
    }
}
