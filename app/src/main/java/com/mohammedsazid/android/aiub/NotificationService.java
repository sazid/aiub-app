package com.mohammedsazid.android.aiub;

import android.annotation.TargetApi;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;


public class NotificationService extends JobIntentService {

    public static final String WRONG_DETAILS_MSG = "Wrong username/password!";
    private CustomWebView webView;
    private SharedPreferences prefs;

    private boolean SHOULD_STOP = false;

    public static void enqueue(Context context) {
        Intent intent = new Intent(context, NotificationService.class);
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

        Log.d("NotificationService", "Starting notification service...");
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
                    SHOULD_STOP = true;
                    return true;
                }

                return super.onJsAlert(view, url, message, result);
            }
        });
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                switch (webView.getUrl()) {
                    case "https://portal.aiub.edu/Student":
                    case "https://portal.aiub.edu/Student/":
                        Log.d("NotificationService", "Logged in");
                        toast("Logged in");
                        webView.loadUrl("https://portal.aiub.edu/Student/Notification");
                        break;
                    case "https://portal.aiub.edu/Student/Notification":
                    case "https://portal.aiub.edu/Student/Notification/":
                        toast("Loaded notifications");
                        SHOULD_STOP = true;
                        break;
                    case "https://portal.aiub.edu/":
                        Log.d("NotificationService", "Logging in...");
                        login(view, url);
                        break;
                    default:
                        // not supported
                        Crashlytics.log(1, "Unsupported URL", webView.getUrl());
                        break;
                }
            }
        });

        try {
            if (wm != null) {
                wm.addView(webView, params);
            }
        } catch (Exception e) {
            e.printStackTrace();
            SHOULD_STOP = true;
            return;
        }

        webView.loadUrl("https://portal.aiub.edu/");
    }

    private void toast(String msg) {
        Toast.makeText(NotificationService.this, msg, Toast.LENGTH_SHORT).show();
    }

    private void login(WebView view, String url) {
        String username = getUsername();
        String password = getPassword();

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password))
            return;

        if (url.contentEquals("https://portal.aiub.edu/") || url.startsWith("https://portal.aiub.edu/Login")) {
            String FIELD_USERNAME_ID = "username";
            String FIELD_PASSWORD_ID = "password";

            String jsScript =
                    "if ($('.text-danger').length !== 0) {" +
                            "window.alert('" + WRONG_DETAILS_MSG + "');" +
                            "} else {" +
                            "$('#" + FIELD_USERNAME_ID + "').val('" + username + "');" +
                            "$('#" + FIELD_PASSWORD_ID + "').val('" + password + "');" +
                            "$('button').first().click();" +
                            "}";


            new Handler(getMainLooper()).postDelayed(() ->
                    view.loadUrl("javascript: {" + jsScript + "};"), 500);
        }
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
            SHOULD_STOP = true;
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

        int tries = 0;
        try {
            while (!SHOULD_STOP && tries++ < 90) {
                Thread.sleep(1000);
            }
        } catch (Exception ignored) {
        }
    }
}
