package com.mohammedsazid.android.aiub;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.DatabaseUtils;
import android.os.Build;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class CheckNoticeService extends IntentService {
    private static final String ACTION_CHECK_FOR_NEW_NOTICE =
            "com.mohammedsazid.android.aiub.action.CHECK_FOR_NEW_NOTICE";
    private static final long REPEAT_INTERVAL = TimeUnit.HOURS.toMillis(6);
    private static final String PREF_NOTICES_KEY = "PREF_NOTICES_KEY";

    public CheckNoticeService() {
        super("CheckNoticeService");
    }

    public static void startActionCheckNotice(Context context) {
        Intent intent = new Intent(context, CheckNoticeService.class);
        intent.setAction(ACTION_CHECK_FOR_NEW_NOTICE);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_CHECK_FOR_NEW_NOTICE.equals(action)) {
                handleActionCheckNotice();
            }
        }
    }

    private void parseNoticeHTML(String url) {
        try {
            Document doc = Jsoup.connect(url).get();
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
                Log.d("NOTICE", "Change detected!");
                Intent i = new Intent(this, MainActivity.class);
                i.putExtra(MainActivity.EXTRA_PRELOAD_URL, "http://aiub.edu/category/notices");

                PendingIntent pi = PendingIntent.getActivity(
                        this, 1, i, PendingIntent.FLAG_UPDATE_CURRENT);

                NotificationCompat.Builder builder =
                        new NotificationCompat.Builder(this)
                                .setSmallIcon(R.drawable.ic_notice)
                                .setContentTitle("AIUB: New notice")
                                .setContentText("Tap to view new notice.")
                                .setAutoCancel(true)
                                .setDefaults(NotificationCompat.DEFAULT_ALL)
                                .setContentIntent(pi);

                NotificationManager notifyMgr =
                        (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                notifyMgr.notify(0, builder.build());
            }

            prefs.edit()
                    .putString(PREF_NOTICES_KEY, sb.toString())
                    .apply();

            Log.d("HTML", sb.toString());
        } catch (IOException e) {
            // try again after 10 minutes
//            scheduleNewCheck(10);
            e.printStackTrace();
        }
    }

    private void scheduleNewCheck(long minutes) {
        long deferTime = TimeUnit.MINUTES.toMillis(minutes);

        Intent intent = new Intent(this, CheckNoticeService.class);
        intent.setAction(ACTION_CHECK_FOR_NEW_NOTICE);

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        PendingIntent pi = PendingIntent.getService(this, 0, intent, 0);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarmManager.setExact(AlarmManager.ELAPSED_REALTIME,
                    SystemClock.elapsedRealtime() + 1000 * 60,
                    pi);
        } else {
            alarmManager.set(AlarmManager.ELAPSED_REALTIME,
                    SystemClock.elapsedRealtime() + TimeUnit.HOURS.toMillis(deferTime),
                    pi);
        }
    }

    private void handleActionCheckNotice() {
        parseNoticeHTML("http://aiub.edu/category/notices");
        scheduleNewCheck(REPEAT_INTERVAL);
    }
}
