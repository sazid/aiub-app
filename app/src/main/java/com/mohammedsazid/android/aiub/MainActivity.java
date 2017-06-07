package com.mohammedsazid.android.aiub;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, AdvancedWebView.Listener {

    public static final String EXTRA_PRELOAD_URL = "EXTRA_PRELOAD_URL";
    public static final String WRONG_DETAILS_MSG = "Wrong username/password!";

    private Toolbar toolbar;
    private NavigationView navigationView;
    private DrawerLayout drawer;
    private ProgressBar progressBar;

    private AdvancedWebView webView;
    @SuppressWarnings("FieldCanBeLocal")
    private CustomWebChromeClient webChromeClient;

    private SharedPreferences prefs;

    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // check if username or password is stored
        if (TextUtils.isEmpty(getUsername()) || TextUtils.isEmpty(getPassword())) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        bindViews();
        setupWebView();

        setSupportActionBar(toolbar);
        toolbar.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText(webView.getUrl(), webView.getUrl());
                clipboard.setPrimaryClip(clip);

                Snackbar.make(v,
                        "URL copied into clipboard",
                        Snackbar.LENGTH_SHORT).show();
                return true;
            }
        });

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(this);

        // check for any new notice
//        NoticeCheckIntentService.startActionCheckNotice(this);
        startService(new Intent(this, NetworkChangeService.class));



        if (getIntent().getStringExtra(EXTRA_PRELOAD_URL) == null) {
            webView.loadUrl("http://www.portal.aiub.edu/");
        } else {
            webView.loadUrl(getIntent().getStringExtra(EXTRA_PRELOAD_URL));
        }

        TextView navUsername = (TextView) navigationView.getHeaderView(0)
                .findViewById(R.id.nav_username);
        navUsername.setText(getUsername());
    }

    private void bindViews() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        navigationView = (NavigationView) findViewById(R.id.nav_view);
        webView = (AdvancedWebView) findViewById(R.id.web_view);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
    }

    private void setupWebView() {
        webView.setCookiesEnabled(true);
        webView.setThirdPartyCookiesEnabled(true);
        webView.setDesktopMode(false);
        webView.getSettings().setSupportZoom(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);
        webView.getSettings().setLoadsImagesAutomatically(true);

        webView.setListener(this, this);

        webChromeClient = new CustomWebChromeClient();
        webView.setWebChromeClient(webChromeClient);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                String username = getUsername();
                String password = getPassword();

                if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password))
                    return;

                if (url.startsWith("https://www.portal.aiub.edu/")) {
                    String FIELD_USERNAME_ID = "username";
                    String FIELD_PASSWORD_ID = "password";
                    String FIELD_LOGIN_BUTTON_ID = "login";

                    String jsScript =
                            "if (document.getElementsByClassName('text-danger').length !== 0) { window.alert('" + WRONG_DETAILS_MSG + "'); } else {" +
                                    "document.getElementById('" + FIELD_USERNAME_ID + "').value = '" + username + "';" +
                                    "document.getElementById('" + FIELD_PASSWORD_ID + "').value = '" + password + "';" +
                                    "document.getElementById('" + FIELD_LOGIN_BUTTON_ID + "').disabled = false;" +
                                    "document.getElementById('" + FIELD_LOGIN_BUTTON_ID + "').click(); }";

                    // execute the script (click the login button automatically);
                    view.loadUrl("javascript: {" + jsScript + "};");
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//                        view.evaluateJavascript(jsScript, null);
//                    } else {
//                    }
                }
            }
        });

//        webView.addHttpHeader("X-Requested-With", getString(R.string.app_name));

        webView.getSettings().setAppCacheEnabled(true);
        webView.getSettings().setAppCachePath(getCacheDir().getAbsolutePath()
                + File.separator + "appCache" + File.separator);
        webView.setSaveEnabled(true);
    }

    private String getPassword() {
        return prefs.getString(getString(R.string.pref_password_key), null);
    }

    private String getUsername() {
        return prefs.getString(getString(R.string.pref_username_key), null);
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.onDestroy();
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (webView.onBackPressed()) {
            super.onBackPressed();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        webView.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();

        if (!TextUtils.isEmpty(getIntent().getStringExtra(EXTRA_PRELOAD_URL))) {
            webView.loadUrl(getIntent().getStringExtra(EXTRA_PRELOAD_URL));
            getIntent().putExtra(EXTRA_PRELOAD_URL, "");
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent != null && !TextUtils.isEmpty(
                intent.getStringExtra(EXTRA_PRELOAD_URL))) {
            webView.loadUrl(intent.getStringExtra(EXTRA_PRELOAD_URL));
            intent.putExtra(EXTRA_PRELOAD_URL, "");
        }
    }

    @Override
    protected void onPause() {
        webView.onPause();
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem refreshMenuItem = menu.findItem(R.id.action_refresh);

        if (isLoading) {
            refreshMenuItem.setTitle("Stop");
            refreshMenuItem.setIcon(R.drawable.ic_stop);
        } else {
            refreshMenuItem.setTitle("Refresh");
            refreshMenuItem.setIcon(R.drawable.ic_refresh);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id) {
            case R.id.action_refresh:
                if (!isLoading) {
                    webView.stopLoading();
                    webView.reload();
                } else {
                    webView.stopLoading();
                }
                return true;
            case R.id.action_forward:
                webView.stopLoading();
                webView.goForward();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        switch (id) {
            case R.id.nav_portal:
                webView.loadUrl("http://www.portal.aiub.edu/");
                break;
            case R.id.nav_home:
                webView.loadUrl("http://www.aiub.edu/");
                break;
            case R.id.nav_notice:
                webView.loadUrl("http://www.aiub.edu/category/notices");
                break;
            case R.id.nav_clubs:
                Snackbar.make(navigationView,
                        "Not available yet",
                        Snackbar.LENGTH_SHORT).show();
                break;
            case R.id.nav_academic_calendar:
                webView.loadUrl("http://www.aiub.edu/academics/calendar/");
                break;
            case R.id.nav_news:
                webView.loadUrl("http://www.aiub.edu/category/news-events");
                break;
            case R.id.nav_logout:
                logout();
                break;
            case R.id.nav_about:
                showHelp();
                break;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        drawer.setSelected(false);
        return true;
    }

    private void logout() {
        prefs.edit()
                .remove(getString(R.string.pref_username_key))
                .remove(getString(R.string.pref_password_key))
                .apply();

        String jsScript = "javascript: {" +
                "document.location.href = '/Login/Logout';" +
                "}";
        webView.loadUrl(jsScript);

        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    public void showHelp() {
        new AlertDialog.Builder(this)
                .setTitle("About")
                .setMessage(getString(R.string.about))
                .setPositiveButton("CONTACT ME", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        composeEmail(
                                new String[]{"sazidozon@gmail.com"},
                                "[AIUB app]: {your subject here}",
                                ""
                        );
                    }
                })
                .create()
                .show();
    }

    public void composeEmail(String[] addresses, String subject, String body) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_EMAIL, addresses);
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, body);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }

/*
    private void clearCookies() {
        webView.clearCache(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().removeAllCookies(null);
            CookieManager.getInstance().flush();
        } else {
            CookieSyncManager cookieSyncMngr = CookieSyncManager.createInstance(MainActivity.this);
            cookieSyncMngr.startSync();
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.removeAllCookie();
            cookieManager.removeSessionCookie();
            cookieSyncMngr.stopSync();
            cookieSyncMngr.sync();
        }
    }
*/

    private class CustomWebChromeClient extends WebChromeClient {

        @Override
        public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
            if (message.contentEquals(WRONG_DETAILS_MSG)) {
                Toast.makeText(MainActivity.this,
                        WRONG_DETAILS_MSG, Toast.LENGTH_SHORT).show();
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                finish();
                return true;
            }

            return super.onJsAlert(view, url, message, result);
        }

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            if (newProgress == 100) {
                progressBar.setVisibility(View.INVISIBLE);
                isLoading = false;
            } else {
                progressBar.setVisibility(View.VISIBLE);
                isLoading = true;
            }

            supportInvalidateOptionsMenu();
            super.onProgressChanged(view, newProgress);
        }

        @Override
        public void onReceivedTitle(WebView view, String title) {
            super.onReceivedTitle(view, title);
            setTitle(title);

            Uri uri = Uri.parse(view.getUrl());
            try {
                //noinspection ConstantConditions
                getSupportActionBar().setSubtitle(uri.getAuthority());
            } catch (Exception ignored) {
            }
        }

        @Override
        public void onReceivedIcon(WebView view, Bitmap icon) {
            super.onReceivedIcon(view, icon);
        }

        @Override
        public void onShowCustomView(View view, CustomViewCallback callback) {
            super.onShowCustomView(view, callback);
//            showVideoInFullscreen(view);
        }

        @Override
        public void onHideCustomView() {
            super.onHideCustomView();
//            revertFullscreenVideo();
        }
    }

    @Override
    public void onPageStarted(String url, Bitmap favicon) {

    }

    @Override
    public void onPageFinished(String url) {

    }

    @Override
    public void onPageError(int errorCode, String description, String failingUrl) {

    }

    @Override
    public void onDownloadRequested(String url, String suggestedFilename, String mimeType, long contentLength, String contentDisposition, String userAgent) {
        if (AdvancedWebView.handleDownload(this, url, suggestedFilename)) {
            Toast.makeText(this, "Downloading file…", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to download file", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onExternalPageRequest(String url) {

    }
}
