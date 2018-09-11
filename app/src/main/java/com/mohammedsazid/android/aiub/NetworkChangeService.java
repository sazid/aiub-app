package com.mohammedsazid.android.aiub;

import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.IBinder;
import android.util.Log;

import io.fabric.sdk.android.Fabric;

public class NetworkChangeService extends Service {
    NetworkChangeReceiver receiver = new NetworkChangeReceiver();

    public NetworkChangeService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(receiver, filter);
        NoticeCheckJobIntentService.startActionCheckNotice(this);
        Log.d("NetworkChangeService", "Starting service");

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        try {
            if (receiver != null) {
                unregisterReceiver(receiver);
            }
        } catch (Exception e) {
            Log.e(NetworkChangeService.class.getSimpleName(), e.getMessage());
            Fabric.getLogger().e(NetworkChangeService.class.getSimpleName(), e.getMessage());
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
