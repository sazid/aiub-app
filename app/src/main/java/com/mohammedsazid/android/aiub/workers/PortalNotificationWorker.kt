package com.mohammedsazid.android.aiub.workers

import android.annotation.TargetApi
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.PixelFormat
import android.net.ConnectivityManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.support.v4.app.NotificationCompat
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.CustomEvent
import com.mohammedsazid.android.aiub.widgets.CustomWebView
import com.mohammedsazid.android.aiub.MainActivity
import com.mohammedsazid.android.aiub.R

class PortalNotificationWorker(context: Context, parameters: WorkerParameters)
    : Worker(context, parameters) {

    val WRONG_DETAILS_MSG = "Wrong username/password!"
    val NOTIFICATIONS_MSG = "NOTIFICATIONS:"
    private val PREF_NOTIFICATIONS_KEY = "PREF_NOTIFICATIONS_KEY"

    private var webView: CustomWebView? = null
    private var prefs: SharedPreferences? = PreferenceManager
            .getDefaultSharedPreferences(applicationContext)
    private var workResult = Result.SUCCESS
    private var shouldStop = false

    override fun doWork(): Result {
        var tries = 0

        try {
            Log.d(javaClass.simpleName, "doWork for PortalNotification")

            postDelayed(looper = Looper.getMainLooper()) {
                createAndAttachView(applicationContext)
            }

            while (!shouldStop && ++tries <= 90) {
                Log.d(javaClass.simpleName, "Trying $tries")
                Thread.sleep(1000)
            }

            if (tries > 90) {
                workResult = Result.FAILURE
            }

            Log.d(javaClass.simpleName, prefs?.getString(PREF_NOTIFICATIONS_KEY, "") ?: "")
            Log.d(javaClass.simpleName, "PortalNotificationWorker done")

            // remove the webview from window manager
            postDelayed(looper = Looper.getMainLooper()) {
                val wm = applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                wm.removeView(webView)
                webView = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            fail("Failed in doWork()")
        }

        return workResult
    }

    fun postDelayed(delay: Long = 0L, looper: Looper = Looper.myLooper(), cb: () -> Unit) {
        if (delay == 0L) {
            Handler(looper).post {
                cb()
            }
        } else {
            Handler(looper).postDelayed({
                cb()
            }, delay)
        }
    }

    private fun success() {
        workResult = Result.SUCCESS
        shouldStop = true
    }

    private fun fail(msg: String) {
        Log.d(javaClass.simpleName, "Failed with reason: $msg")
        workResult = Result.FAILURE
        shouldStop = true
    }

//    private fun retry() {
//        workResult = Result.RETRY
//        lock.notifyAll()
//    }

    private fun createAndAttachView(context: Context) {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val params: WindowManager.LayoutParams

        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                flag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 0
        params.width = 0
        params.height = 0

        webView = CustomWebView(applicationContext)
        webView?.webChromeClient = object : WebChromeClient() {
            override fun onJsAlert(view: WebView, url: String, message: String, result: JsResult): Boolean {
                Log.d(javaClass.simpleName, "Inside onJsAlert")

                if (message.startsWith(NOTIFICATIONS_MSG)) {
                    parseNotification(message)
                    success()
                    return super.onJsAlert(view, url, message, result)
                } else if (message.contentEquals(WRONG_DETAILS_MSG)) {
                }

                fail("Failed onJsAlert")
                return super.onJsAlert(view, url, message, result)
            }
        }
        webView?.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                Log.d(javaClass.simpleName, "onPageFinished")

                if (webView == null) {
                    fail("webView is null inside onPageFinished")
                    return
                }

                when (url) {
                    "https://portal.aiub.edu/Student", "https://portal.aiub.edu/Student/" -> {
                        webView?.loadUrl("https://portal.aiub.edu/Student/Notification")
                    }
                    "https://portal.aiub.edu/Student/Notification", "https://portal.aiub.edu/Student/Notification/" -> {
                        postDelayed(delay = 500) {
                            webView?.loadUrl(
                                    "javascript: {\n" +
                                            "var loadDetector = setInterval(function() {\n" +
                                            "   if ($('#dvLoading').css('display') === 'none') {\n" +
                                            "       alert('" + NOTIFICATIONS_MSG + "' + $('div.col-md-1 > small').text());\n" +
                                            "       clearInterval(loadDetector);\n" +
                                            "   }\n" +
                                            "}, 100);\n" +
                                            "}"
                            )
                        }
                    }
                    "https://portal.aiub.edu/" -> {
                        login(view, url)
                    }
                    else -> {
                        // not supported
                        Crashlytics.log(1, "Unsupported URL", webView?.url)
                        fail("Unknown url")
                    }
                }
            }
        }

        try {
            wm.addView(webView, params)
        } catch (e: Exception) {
            e.printStackTrace()
            fail("Failed to add view to WindowManager")
            return
        }

        webView?.loadUrl("https://portal.aiub.edu/")
    }

    private fun parseNotification(newMsg: String) {
        Log.d(javaClass.simpleName, "Parsing notification")
        try {
            val storedMsg = prefs?.getString(PREF_NOTIFICATIONS_KEY, "")
            if (prefs!!.contains(PREF_NOTIFICATIONS_KEY) &&
                    newMsg.length > storedMsg!!.length &&
                    !newMsg.contentEquals(NOTIFICATIONS_MSG) &&
                    !newMsg.contentEquals(storedMsg)) {
                postNewNoticeNotification()
            }

            prefs!!.edit()
                    ?.putString(PREF_NOTIFICATIONS_KEY, newMsg)
                    ?.apply()

            success()
        } catch (e: Exception) {
            e.printStackTrace()
            fail("Failed to parse notification")
        }
    }

    private fun postNewNoticeNotification() {
        Log.d(javaClass.simpleName, "Post new notification")
        val i = Intent(applicationContext, MainActivity::class.java)
        i.putExtra(MainActivity.EXTRA_PRELOAD_URL, "https://portal.aiub.edu/")

        val pi = PendingIntent.getActivity(
                applicationContext, 1, i, 0)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel("Portal Notification", NotificationManager.IMPORTANCE_MAX)
        }

        val builder = NotificationCompat.Builder(applicationContext, "Portal Notification")
                .setSmallIcon(R.drawable.ic_notification_notice)
                .setContentTitle("New portal notification")
                .setContentText("Tap to view new notification")
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pi)

        val notifyMgr = applicationContext
                .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?

        notifyMgr?.notify(121, builder.build())

        Answers.getInstance().logCustom(
                CustomEvent("Portal notification notified"))
    }

    @TargetApi(26)
    @Synchronized
    private fun createChannel(channelId: String, importance: Int) {
        Log.d(javaClass.simpleName, "Creating channel")
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?

        val mChannel = NotificationChannel(
                channelId,
                channelId,
                importance)

        mChannel.enableLights(true)
        mChannel.lightColor = Color.BLUE
        if (manager != null) {
            manager.createNotificationChannel(mChannel)
        } else {
            fail("Failed to create notification channel")
        }
    }

    private fun login(view: WebView, url: String) {
        Log.d(javaClass.simpleName, "Logging in")
        val username = getUsername()
        val password = getPassword()

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password))
            return

        if (url.contentEquals("https://portal.aiub.edu/") || url.startsWith("https://portal.aiub.edu/Login")) {
            val fieldUsernameId = "username"
            val fieldPasswordId = "password"

            val jsScript = "if ($('.text-danger').length !== 0) {" +
                    "window.alert('$WRONG_DETAILS_MSG');" +
                    "} else {" +
                    "$('#$fieldUsernameId').val('$username');" +
                    "$('#$fieldPasswordId').val('$password');" +
                    "$('button[type=submit]:contains(\"Log In\")').click();" +
                    "}"


            postDelayed(delay = 500) {
                view.loadUrl("javascript: {$jsScript};")
            }
        }
    }

    private fun getPassword(): String? {
        return prefs?.getString(applicationContext.getString(R.string.pref_password_key), null)
    }

    private fun getUsername(): String? {
        return prefs?.getString(applicationContext.getString(R.string.pref_username_key), null)
    }


}
