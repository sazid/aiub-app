package com.mohammedsazid.android.aiub;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.File;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, AdvancedWebView.Listener {

    private Toolbar toolbar;
    private NavigationView navigationView;
    private DrawerLayout drawer;
    private ProgressBar progressBar;

    private AdvancedWebView webView;
    private CustomWebChromeClient webChromeClient;

    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

        webView.loadUrl("http://portal.aiub.edu/");
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
        webView.setWebViewClient(new WebViewClient());

        webView.addHttpHeader("X-Requested-With", getString(R.string.app_name));

        webView.getSettings().setAppCacheEnabled(true);
        webView.getSettings().setAppCachePath(getCacheDir().getAbsolutePath()
                + File.separator + "appCache" + File.separator);
        webView.setSaveEnabled(true);
    }

    @Override
    protected void onDestroy() {
        webView.onDestroy();
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
            case R.id.action_settings:
                return true;
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
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_portal) {
            webView.loadUrl("http://portal.aiub.edu/");
        } else if (id == R.id.nav_home) {
            webView.loadUrl("http://aiub.edu/");
        } else if (id == R.id.nav_notice) {
            webView.loadUrl("http://aiub.edu/category/notices");
        } else if (id == R.id.nav_clubs) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private class CustomWebChromeClient extends WebChromeClient {

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
