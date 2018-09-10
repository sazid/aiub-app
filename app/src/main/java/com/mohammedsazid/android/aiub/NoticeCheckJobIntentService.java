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
    private static final String ACTION_CHECK_FOR_NEW_NOTICE =
            "com.mohammedsazid.android.aiub.action.CHECK_FOR_NEW_NOTICE";
    private static final long REPEAT_INTERVAL = TimeUnit.HOURS.toMinutes(1);
    //    private static final long REPEAT_INTERVAL = 1;
    private static final String PREF_NOTICES_KEY = "PREF_NOTICES_KEY";


    public static void startActionCheckNotice(Context context) {
        Intent intent = new Intent(context, NoticeCheckJobIntentService.class);
        intent.setAction(ACTION_CHECK_FOR_NEW_NOTICE);
//        context.startService(intent);
        ContextCompat.startForegroundService(context, intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stopForeground(true); //true will remove notification
        }
    }

    @NonNull
    @TargetApi(26)
    private synchronized String createChannel(String channelId) {
        NotificationManager mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

        int importance = NotificationManager.IMPORTANCE_LOW;

        NotificationChannel mChannel = new NotificationChannel("Notice Service", channelId, importance);

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
            createChannel("Notice Service");

            NotificationCompat.Builder builder = new NotificationCompat
                    .Builder(this, "Notice Service")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setWhen(System.currentTimeMillis())
                    .setPriority(NotificationManager.IMPORTANCE_NONE)
                    .setContentTitle("AIUB App")
                    .setTicker("AIUB App: Checking new notices")
                    .setContentText("AIUB App: Checking new notices")
                    .setContentInfo("Info");

            Log.d(NoticeCheckJobIntentService.class.getSimpleName(), "Starting foreground service...");
            startForeground(1000, builder.build());
        }
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        final String action = intent.getAction();
        if (ACTION_CHECK_FOR_NEW_NOTICE.equals(action)) {
            handleActionCheckNotice();
        }
    }

    private void parseNoticeHTML(String url) {
        try {
            Document doc = Jsoup
                    .connect(url)
                    .timeout(300 * 1000)
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

    private void scheduleNewCheck(long minutes) {
        long deferTime = TimeUnit.MINUTES.toMillis(minutes);
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        Intent intent = new Intent();
        intent.setClass(this, NoticeCheckJobIntentService.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(ACTION_CHECK_FOR_NEW_NOTICE);

        PendingIntent pi = PendingIntent.getService(this, 0, intent, 0);

        // cancel any previous alarm
        if (alarmManager != null) {
            alarmManager.cancel(pi);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (alarmManager != null) {
                alarmManager.setExact(AlarmManager.ELAPSED_REALTIME,
                        SystemClock.elapsedRealtime() + deferTime,
                        pi);
            }
        } else {
            if (alarmManager != null) {
                alarmManager.set(AlarmManager.ELAPSED_REALTIME,
                        SystemClock.elapsedRealtime() + deferTime,
                        pi);
            }
        }
    }

    private void handleActionCheckNotice() {
//        Log.d(NoticeCheckJobIntentService.class.getSimpleName(), "Checking for new notice!");
        parseNoticeHTML("http://www.aiub.edu/category/notices");
        scheduleNewCheck(REPEAT_INTERVAL);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stopForeground(true); //true will remove notification
        }
    }
}