package com.mohammedsazid.android.aiub;

import android.annotation.TargetApi;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.JobIntentService;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebView;


public class NotificationService extends JobIntentService {

    public static final String WRONG_DETAILS_MSG = "Wrong username/password!";
    private CustomWebView webView;
    private SharedPreferences prefs;

    public static void enqueue(Context context) {
        Intent intent = new Intent(context, NotificationService.class);
//        ContextCompat.startForegroundService(context, intent);
        enqueueWork(context, NotificationService.class, 23, intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel("Notification Service", NotificationManager.IMPORTANCE_LOW);
        }

        NotificationCompat.Builder builder = new NotificationCompat
                .Builder(this, "Notification Service")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setWhen(System.currentTimeMillis())
                .setContentTitle("Checking for new notifications");

        Log.d("NotificationServce", "Starting notification service...");
        startForeground(2, builder.build());

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
    }

    private void createAndAttachView(Context context) {
        final WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        WindowManager.LayoutParams params;

        int flag;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            flag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            flag = WindowManager.LayoutParams.TYPE_PHONE;
        }

        params = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                flag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                ,
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = 0;
        params.width = 0;
        params.height = 0;

        webView = new CustomWebView(this);
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                if (message.contentEquals(WRONG_DETAILS_MSG)) {
                    stopSelf();
                    return true;
                }

                return super.onJsAlert(view, url, message, result);
            }
        });

        try {
            if (wm != null) {
                wm.addView(webView, params);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        stopSelf();
    }

    @NonNull
    @TargetApi(26)
    private synchronized String createChannel(String channelId, int importance) {
        NotificationManager mNotificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationChannel mChannel = new NotificationChannel(
                channelId,
                channelId,
                importance);

        mChannel.enableLights(true);
        mChannel.setLightColor(Color.BLUE);
        if (mNotificationManager != null) {
            mNotificationManager.createNotificationChannel(mChannel);
        } else {
            stopSelf();
        }
        return channelId;
    }

    @Override
    public void onDestroy() {
        if (webView != null) {
            WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
            if (wm != null) wm.removeView(webView);
            webView.onDestroy();
            webView = null;
        }
        super.onDestroy();
    }

    private String getPassword() {
        return prefs.getString(getString(R.string.pref_password_key), null);
    }

    private String getUsername() {
        return prefs.getString(getString(R.string.pref_username_key), null);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        new Handler(getMainLooper())
                .post(() -> createAndAttachView(this));
    }
}
