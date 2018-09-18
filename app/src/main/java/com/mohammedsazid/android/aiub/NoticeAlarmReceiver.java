package com.mohammedsazid.android.aiub;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;

import java.util.concurrent.TimeUnit;

import static android.content.Context.ALARM_SERVICE;

public class NoticeAlarmReceiver extends BroadcastReceiver {

    // reschedule every one hour
    private static final long REPEAT_INTERVAL = 1;
    public static final String CHECK_NOTICE_ACTION = "com.mohammedsazid.intent.action.CHECK_NOTICE";

    private static PendingIntent getPendingIntent(Context context) {
        Intent intent = new Intent();
        intent.setClass(context, NoticeAlarmReceiver.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(CHECK_NOTICE_ACTION);

        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    public static void scheduleNewCheck(Context context) {
        long deferTime = TimeUnit.HOURS.toMillis(REPEAT_INTERVAL);
        long when = SystemClock.elapsedRealtime() + deferTime;
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);

        PendingIntent pi = getPendingIntent(context);

        if (alarmManager != null) {
            // cancel any previous alarm
            alarmManager.cancel(pi);

            int SDK_INT = Build.VERSION.SDK_INT;
            if (SDK_INT < Build.VERSION_CODES.KITKAT)
                alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, when, pi);
            else if (SDK_INT < Build.VERSION_CODES.M)
                alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, when, pi);
            else
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, when, pi);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        NoticeCheckJobIntentService.enqueue(context);
        NotificationService.enqueue(context);
    }
}
