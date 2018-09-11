package com.mohammedsazid.android.aiub;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.DatabaseUtils;
import android.graphics.Color;
import android.os.Build;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.concurrent.TimeUnit;

public class NoticeCheckJobIntentService extends JobIntentService {
    private static final String PREF_NOTICES_KEY = "PREF_NOTICES_KEY";


    public static void startActionCheckNotice(Context context) {
        Intent intent = new Intent(context, NoticeCheckJobIntentService.class);
        enqueueWork(context, NoticeCheckJobIntentService.class, 1, intent);
//        ContextCompat.startForegroundService(context, intent);
    }

    @NonNull
    @TargetApi(26)
    private synchronized String createChannel(String channelId) {
        NotificationManager mNotificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationChannel mChannel = new NotificationChannel(
                "Notice Service",
                channelId,
                NotificationManager.IMPORTANCE_LOW);

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
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationCompat.Builder builder = new NotificationCompat
                    .Builder(this, createChannel("Notice Service"))
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setWhen(System.currentTimeMillis())
                    .setPriority(NotificationManager.IMPORTANCE_LOW)
                    .setContentTitle("AIUB App")
                    .setTicker("AIUB App: Checking new notices")
                    .setContentText("AIUB App: Checking new notices")
                    .setContentInfo("Info");

            Log.d(NoticeCheckJobIntentService.class.getSimpleName(), "Starting foreground service...");
            startForeground(1, builder.build());
        }
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        Log.d("SERVICE", "Starting work");
        handleActionCheckNotice();
        Log.d("SERVICE", "Work done");
    }

    private void parseNoticeHTML(String url) {
        try {
            Document doc = Jsoup
                    .connect(url)
                    .timeout(60 * 1000)
                    .get();
            Element event_list = doc.getElementsByClass("event-list").first();

            StringBuilder sb = new StringBuilder();

            for (Element el : event_list.children()) {
                sb.append(DatabaseUtils.sqlEscapeString(
                        el.getElementsByTag("h2").first().text())
                );
            }

            SharedPreferences prefs = PreferenceManager
                    .getDefaultSharedPreferences(this);
            if (prefs.contains(PREF_NOTICES_KEY) &&
                    (!prefs.getString(PREF_NOTICES_KEY, "").contentEquals(sb))) {
//                Log.d("NOTICE", "Change detected!");
                Intent i = new Intent(this, MainActivity.class);
                i.putExtra(MainActivity.EXTRA_PRELOAD_URL, url);

                PendingIntent pi = PendingIntent.getActivity(
                        this, 1, i, PendingIntent.FLAG_UPDATE_CURRENT);

                createChannel("Notice");
                NotificationCompat.Builder builder =
                        new NotificationCompat.Builder(this, "Notice")
                                .setSmallIcon(R.drawable.ic_notification_notice)
                                .setContentTitle("AIUB: New notice")
                                .setContentText("Tap to view new notice.")
                                .setAutoCancel(true)
                                .setDefaults(NotificationCompat.DEFAULT_ALL)
                                .setContentIntent(pi);

                NotificationManager notifyMgr =
                        (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                if (notifyMgr != null) {
                    notifyMgr.notify(0, builder.build());
                }

                Answers.getInstance().logCustom(
                        new CustomEvent("Notice notified"));
            }

            prefs.edit()
                    .putString(PREF_NOTICES_KEY, sb.toString())
                    .apply();
        } catch (Exception e) {
            e.printStackTrace();
            Crashlytics.logException(e);
        }
    }

    private void handleActionCheckNotice() {
        parseNoticeHTML("http://www.aiub.edu/category/notices");
        NoticeAlarmReceiver.scheduleNewCheck(this);
        Log.d("SERVICE", "Check complete");

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                stopForeground(true); //true will remove notification
                Log.d("SERVICE", "Stopping foreground");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Crashlytics.logException(e);
        }

        stopSelf();
    }
}
