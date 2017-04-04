package com.mohammedsazid.android.aiub;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.IBinder;

public class NetworkChangeService extends Service {
    NetworkChangeReceiver receiver = new NetworkChangeReceiver();

    public NetworkChangeService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(receiver, filter);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (receiver != null) {
            unregisterReceiver(receiver);
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
//        Return the communication channel to the service.
//        throw new UnsupportedOperationException("Not yet implemented");
    }
}
