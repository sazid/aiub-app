package com.mohammedsazid.android.aiub.workers

import android.annotation.TargetApi
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.database.DatabaseUtils
import android.graphics.Color
import android.os.Build
import android.preference.PreferenceManager
import android.support.v4.app.NotificationCompat
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.CustomEvent
import com.mohammedsazid.android.aiub.MainActivity
import com.mohammedsazid.android.aiub.R
import org.jsoup.Jsoup

class NoticeWorker(context: Context, params: WorkerParameters)
    : Worker(context, params) {

    private val PREF_NOTICES_KEY = "PREF_NOTICES_KEY"

    override fun doWork(): Result {
        Log.d(javaClass.simpleName, "doWork for NoticeWorker")
        parseNoticeHTML("http://www.aiub.edu/category/notices")
        Log.d(javaClass.simpleName, "doWork for NoticeWorker")
        return Result.SUCCESS
    }

    @TargetApi(26)
    @Synchronized
    private fun createChannel(channelId: String, importance: Int): String {
        val manager = applicationContext
                .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?

        val mChannel = NotificationChannel(
                channelId,
                channelId,
                importance)

        mChannel.enableLights(true)
        mChannel.lightColor = Color.BLUE
        if (manager != null) {
            manager.createNotificationChannel(mChannel)
        } else {
        }
        return channelId
    }

    private fun parseNoticeHTML(url: String) {
        try {
            val doc = Jsoup
                    .connect(url)
                    .timeout(60 * 1000)
                    .get()
            val eventList = doc.getElementsByClass("event-list").first()

            val sb = StringBuilder()

            for (el in eventList.children()) {
                sb.append(DatabaseUtils.sqlEscapeString(
                        el.getElementsByTag("h2").first().text())
                )
            }

            val prefs = PreferenceManager
                    .getDefaultSharedPreferences(applicationContext)

            if (prefs.contains(PREF_NOTICES_KEY) &&
                    !prefs.getString(PREF_NOTICES_KEY, "")!!.contentEquals(sb)) {
                val i = Intent(applicationContext, MainActivity::class.java)
                i.putExtra(MainActivity.EXTRA_PRELOAD_URL, url)

                val pi = PendingIntent.getActivity(
                        applicationContext, 1, i, 0)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    createChannel("Notice", NotificationManager.IMPORTANCE_MAX)
                }

                val builder = NotificationCompat.Builder(applicationContext, "Notice")
                        .setSmallIcon(R.drawable.ic_notification_notice)
                        .setContentTitle("New notice")
                        .setContentText("Tap to view new notice")
                        .setAutoCancel(true)
                        .setDefaults(NotificationCompat.DEFAULT_ALL)
                        .setContentIntent(pi)

                val notifyMgr = applicationContext
                        .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?

                notifyMgr?.notify(120, builder.build())

                Answers.getInstance().logCustom(
                        CustomEvent("Notice notified"))
            }

            prefs.edit()
                    .putString(PREF_NOTICES_KEY, sb.toString())
                    .apply()
        } catch (e: Exception) {
            e.printStackTrace()
//            Crashlytics.logException(e)
        }

    }

}
