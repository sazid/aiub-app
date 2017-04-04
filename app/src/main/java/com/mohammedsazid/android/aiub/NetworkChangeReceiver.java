package com.mohammedsazid.android.aiub;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v4.content.WakefulBroadcastReceiver;

public class NetworkChangeReceiver extends WakefulBroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
//        Toast.makeText(context, "Network state changed! Receiver called", Toast.LENGTH_SHORT).show();
        int status = NetworkUtil.getConnectivityStatusString(context);
        if (!ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
            if (status != NetworkUtil.NETWORK_STATUS_NOT_CONNECTED) {
                CheckNoticeService.startActionCheckNotice(context);
            }
        }
    }
}

class NetworkUtil {
    private static int TYPE_WIFI = 1;
    private static int TYPE_MOBILE = 2;
    private static int TYPE_NOT_CONNECTED = 0;
    @SuppressWarnings("WeakerAccess")
    public static final int NETWORK_STATUS_NOT_CONNECTED = 0,
            NETWORK_STAUS_WIFI = 1,
            NETWORK_STATUS_MOBILE = 2;

    private static int getConnectivityStatus(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (null != activeNetwork) {
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI)
                return TYPE_WIFI;

            if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE)
                return TYPE_MOBILE;
        }
        return TYPE_NOT_CONNECTED;
    }

    static int getConnectivityStatusString(Context context) {
        int conn = NetworkUtil.getConnectivityStatus(context);
        int status = 0;
        if (conn == NetworkUtil.TYPE_WIFI) {
            status = NETWORK_STAUS_WIFI;
        } else if (conn == NetworkUtil.TYPE_MOBILE) {
            status = NETWORK_STATUS_MOBILE;
        } else if (conn == NetworkUtil.TYPE_NOT_CONNECTED) {
            status = NETWORK_STATUS_NOT_CONNECTED;
        }
        return status;
    }
}