package com.mohammedsazid.android.aiub

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.*
import android.net.Uri
import android.os.*
import android.preference.PreferenceManager
import android.provider.Settings
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.text.TextUtils
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.*
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.crashlytics.android.Crashlytics
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.mohammedsazid.android.aiub.widgets.AdvancedWebView
import com.mohammedsazid.android.aiub.widgets.CustomWebView
import com.mohammedsazid.android.aiub.workers.NoticeWorker
import com.mohammedsazid.android.aiub.workers.PortalNotificationWorker
import io.fabric.sdk.android.Fabric
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private var toolbar: Toolbar? = null
    private var navigationView: NavigationView? = null
    private var drawer: DrawerLayout? = null
    private var progressBar: ProgressBar? = null
    private var mAdView: AdView? = null

    private var webView: CustomWebView? = null
    private var webChromeClient: CustomWebChromeClient? = null

    private var prefs: SharedPreferences? = null

    private var isLoading = false

    private val password: String?
        get() = prefs?.getString(getString(R.string.pref_password_key), null)

    private val username: String?
        get() = prefs?.getString(getString(R.string.pref_username_key), null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Fabric.with(this, Crashlytics())
        setContentView(R.layout.activity_main)

        prefs = PreferenceManager.getDefaultSharedPreferences(this)

        // check if username or password is stored
        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        checkPermission()

        bindViews()
        setupWebView()

        setSupportActionBar(toolbar)
        toolbar?.setOnLongClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(webView?.url, webView?.url)
            clipboard.primaryClip = clip

            toast("URL copied into clipboard")
            true
        }

        val toggle = ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer?.addDrawerListener(toggle)
        toggle.syncState()

        navigationView?.setNavigationItemSelectedListener(this)

        if (intent.getStringExtra(EXTRA_PRELOAD_URL) == null) {
            webView?.loadUrl("https://portal.aiub.edu/")
        } else {
            webView?.loadUrl(intent.getStringExtra(EXTRA_PRELOAD_URL))
        }

        val navUsername = navigationView?.getHeaderView(0)
                ?.findViewById<TextView>(R.id.nav_username)
        navUsername?.text = username

        initAd()

        scheduleWork()
    }

    private fun initAd() {
        mAdView = findViewById(R.id.adView)
        val adRequest = AdRequest.Builder()
                .addTestDevice("2A7E924579ED23D7D38EC8451A9568B0")
                .build()
        mAdView?.loadAd(adRequest)
    }

    private fun scheduleWork() {
        val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

        val noticeWork = PeriodicWorkRequest.Builder(NoticeWorker::class.java, 1, TimeUnit.HOURS, 30, TimeUnit.MINUTES)
                .addTag("NoticeWorker")
                .setConstraints(constraints)
                .build()

        val portalNotificationWorker = PeriodicWorkRequest.Builder(PortalNotificationWorker::class.java, 1, TimeUnit.HOURS, 30, TimeUnit.MINUTES)
                .addTag("PortalNotificationWorker")
                .setConstraints(constraints)
                .build()

        // cancel any pending work
        WorkManager.getInstance().cancelAllWorkByTag("NoticeWorker")
        WorkManager.getInstance().cancelAllWorkByTag("PortalNotificationWorker")

        // enqueue new work
        WorkManager.getInstance().enqueue(noticeWork)
        WorkManager.getInstance().enqueue(portalNotificationWorker)
    }

    private fun postDelayed(delay: Long = 0L, looper: Looper = Looper.myLooper(), cb: () -> Unit) {
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

    private fun bindViews() {
        toolbar = findViewById(R.id.toolbar)
        drawer = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        webView = findViewById(R.id.web_view)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun login(view: WebView, url: String) {
        Log.d(javaClass.simpleName, "Logging in")
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
                    "$('button').first().click();" +
                    "}"


            postDelayed(delay = 500) {
                view.loadUrl("javascript: {$jsScript};")
            }
        }
    }

    private fun setupWebView() {
        webChromeClient = CustomWebChromeClient()
        webView?.webChromeClient = webChromeClient
        webView?.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                login(view, url)
            }
        }

        webView?.setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
            val request = DownloadManager.Request(Uri.parse(url))

            request.setMimeType(mimetype)
            //------------------------COOKIE!!------------------------
            val cookies = CookieManager.getInstance().getCookie(url)
            request.addRequestHeader("cookie", cookies)
            //------------------------COOKIE!!------------------------
            request.addRequestHeader("User-Agent", userAgent)
            request.setDescription("Downloading file...")
            request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimetype))
            request.allowScanningByMediaScanner()
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, contentDisposition, mimetype))
            val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

            try {
                try {
                    dm.enqueue(request)
                } catch (e: SecurityException) {
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                    dm.enqueue(request)
                }
            } catch (e: SecurityException) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    toast("Please enable STORAGE permission!")
                    openAppSettings(this, packageName)
                }

            } catch (e: IllegalArgumentException) {
                // show the settings screen where the user can enable the download manager app again
                openAppSettings(this, AdvancedWebView.PACKAGE_NAME_DOWNLOAD_MANAGER)
            }
        }
    }

    override fun onDestroy() {
        webView?.onDestroy()
        super.onDestroy()
    }

    override fun onBackPressed() {
        try {
            if (drawer!!.isDrawerOpen(GravityCompat.START)) {
                drawer!!.closeDrawer(GravityCompat.START)
            } else if (webView!!.onBackPressed()) {
                super.onBackPressed()
            }
        } catch (ignored: Exception) {
            super.onBackPressed()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (requestCode == ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE) {
                if (!Settings.canDrawOverlays(this)) {
                    // You don't have permission
                    toast("Please enable overlay/draw on top permission")
                    checkPermission()
                }
            }
        } else {
            webView?.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        webView?.onResume()

        if (!TextUtils.isEmpty(intent.getStringExtra(EXTRA_PRELOAD_URL))) {
            webView?.loadUrl(intent.getStringExtra(EXTRA_PRELOAD_URL))
            intent.putExtra(EXTRA_PRELOAD_URL, "")
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent != null && !TextUtils.isEmpty(
                        intent.getStringExtra(EXTRA_PRELOAD_URL))) {
            webView?.loadUrl(intent.getStringExtra(EXTRA_PRELOAD_URL))
            intent.putExtra(EXTRA_PRELOAD_URL, "")
        }
    }

    override fun onPause() {
        webView?.onPause()
        super.onPause()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val refreshMenuItem = menu.findItem(R.id.action_refresh)

        if (isLoading) {
            refreshMenuItem.title = "Stop"
            refreshMenuItem.setIcon(R.drawable.ic_stop)
        } else {
            refreshMenuItem.title = "Refresh"
            refreshMenuItem.setIcon(R.drawable.ic_refresh)
        }

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId

        when (id) {
            R.id.action_refresh -> {
                if (!isLoading) {
                    webView?.stopLoading()
                    webView?.reload()
                } else {
                    webView?.stopLoading()
                }
                return true
            }
            R.id.action_forward -> {
                webView?.stopLoading()
                webView?.goForward()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        val id = item.itemId

        when (id) {
            R.id.nav_portal -> webView?.loadUrl("https://portal.aiub.edu/")
            R.id.nav_home -> webView?.loadUrl("http://www.aiub.edu/")
            R.id.nav_notice -> webView?.loadUrl("http://www.aiub.edu/category/notices")
//            R.id.nav_clubs -> toast("Not available yet")
            R.id.nav_academic_calendar -> webView?.loadUrl("http://www.aiub.edu/academics/calendar/")
            R.id.nav_news -> webView?.loadUrl("http://www.aiub.edu/category/news-events")
            R.id.nav_logout -> logout()
            R.id.nav_about -> showHelp()
            R.id.nav_other_apps -> startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://search?q=pub:Sazid")))
            R.id.nav_rate -> {
                val appPackageName = packageName
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appPackageName")))
                } catch (ignored: ActivityNotFoundException) {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")))
                }
            }
        }

        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        drawer.closeDrawer(GravityCompat.START)
        drawer.isSelected = false
        return true
    }

    private fun checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName"))
                startActivityForResult(intent, ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE)
            }
        }
    }

    private fun logout() {
        prefs?.edit()
                ?.remove(getString(R.string.pref_username_key))
                ?.remove(getString(R.string.pref_password_key))
                ?.apply()

        val jsScript = "javascript: {" +
                "document.location.href = '/Login/Logout';" +
                "}"
        webView?.loadUrl(jsScript)

        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun showHelp() {
        AlertDialog.Builder(this)
                .setTitle("About")
                .setMessage(getString(R.string.about))
                .setPositiveButton("CONTACT ME") { dialog, which ->
                    composeEmail(
                            arrayOf("sazidozon@gmail.com"),
                            "[AIUB app]: {your subject here}",
                            ""
                    )
                }
                .create()
                .show()
    }

    private fun composeEmail(addresses: Array<String>, subject: String, body: String) {
        val intent = Intent(Intent.ACTION_SENDTO)
        intent.data = Uri.parse("mailto:")
        intent.putExtra(Intent.EXTRA_EMAIL, addresses)
        intent.putExtra(Intent.EXTRA_SUBJECT, subject)
        intent.putExtra(Intent.EXTRA_TEXT, body)
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        }
    }

    private inner class CustomWebChromeClient : WebChromeClient() {

        override fun onJsAlert(view: WebView, url: String, message: String, result: JsResult): Boolean {
            if (message.contentEquals(WRONG_DETAILS_MSG)) {
                toast(WRONG_DETAILS_MSG)
                startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                finish()
                return true
            }

            return super.onJsAlert(view, url, message, result)
        }

        override fun onProgressChanged(view: WebView, newProgress: Int) {
            if (newProgress == 100) {
                progressBar?.visibility = View.INVISIBLE
                isLoading = false
            } else {
                progressBar?.visibility = View.VISIBLE
                isLoading = true
            }

            invalidateOptionsMenu()
            super.onProgressChanged(view, newProgress)
        }

        override fun onReceivedTitle(view: WebView, title: String) {
            super.onReceivedTitle(view, title)
            setTitle(title)

            val uri = Uri.parse(view.url)
            supportActionBar?.subtitle = uri.authority
        }
    }

    @Suppress("MayBeConstant")
    companion object {
        val EXTRA_PRELOAD_URL = "EXTRA_PRELOAD_URL"
        val WRONG_DETAILS_MSG = "Wrong username/password!"
        var ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE = 5469

        @SuppressLint("NewApi")
        private fun openAppSettings(context: Context, packageName: String) {
            try {
                val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:$packageName")
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

                context.startActivity(intent)
            } catch (ignored: Exception) {
            }

        }
    }
}
