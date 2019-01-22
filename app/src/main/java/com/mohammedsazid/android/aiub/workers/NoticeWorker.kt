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
import com.mohammedsazid.android.aiub.DiffViewer
import com.mohammedsazid.android.aiub.MainActivity
import com.mohammedsazid.android.aiub.R
import com.mohammedsazid.android.aiub.computeHash
import org.jsoup.Jsoup

class NoticeWorker(context: Context, params: WorkerParameters)
    : Worker(context, params) {

    private val PREF_NOTICES_KEY = "PREF_NOTICES_HASH_KEY"

    private var workerResult = Result.SUCCESS

    override fun doWork(): Result {
        Log.d(javaClass.simpleName, "doWork for NoticeWorker")
        parseNoticeHTML("http://www.aiub.edu/category/notices")
        Log.d(javaClass.simpleName, "NoticeWorker done")
        return workerResult
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

            val sb = doc.select(".event-list > li > time > .day").text().trim()
            val newNoticeHash = computeHash(sb)

            Log.d("HASH", newNoticeHash.toString())

            val prefs = PreferenceManager
                    .getDefaultSharedPreferences(applicationContext)

//            val intent = Intent(applicationContext, DiffViewer::class.java)
//            intent.putExtra("PREF_STRING", prefs.getLong(PREF_NOTICES_KEY, 0L).toString())
//            intent.putExtra("NOTI_STRING", newNoticeHash.toString())
//            intent.putExtra("TITLE", "Notice worker")
//            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
//            applicationContext.startActivity(intent)

            if (
                    prefs.contains(PREF_NOTICES_KEY) &&
                    newNoticeHash != 0L &&
                    prefs.getLong(PREF_NOTICES_KEY, 0L) != newNoticeHash
            ) {
                val i = Intent(applicationContext, MainActivity::class.java)
                i.putExtra(MainActivity.EXTRA_PRELOAD_URL, url)
//
//                Log.d("NOTICE", prefs.getLong(PREF_NOTICES_KEY, 0L).toString())
//                Log.d("NOTICE", sb)

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

                prefs.edit()
                        .putLong(PREF_NOTICES_KEY, newNoticeHash)
                        .apply()
            } else if (!prefs.contains(PREF_NOTICES_KEY)) {
                prefs.edit()
                        .putLong(PREF_NOTICES_KEY, newNoticeHash)
                        .apply()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            workerResult = Result.FAILURE
//            Crashlytics.logException(e)
        }

    }

}
