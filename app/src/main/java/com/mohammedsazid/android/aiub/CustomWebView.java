package com.mohammedsazid.android.aiub;

import android.content.Context;
import android.util.AttributeSet;

import java.io.File;

public class CustomWebView extends AdvancedWebView {
    public CustomWebView(Context context) {
        super(context);
        setupWebView();
    }

    public CustomWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setupWebView();
    }

    public CustomWebView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setupWebView();
    }

    private void setupWebView() {
        setCookiesEnabled(true);
        setThirdPartyCookiesEnabled(true);
        setDesktopMode(false);
        getSettings().setSupportZoom(true);
        getSettings().setBuiltInZoomControls(true);
        getSettings().setDisplayZoomControls(false);
        getSettings().setLoadsImagesAutomatically(true);

        addHttpHeader("X-Requested-With", getContext().getString(R.string.app_name));
        getSettings().setAppCacheEnabled(true);
        getSettings().setAppCachePath(getContext().getCacheDir().getAbsolutePath()
                + File.separator + "appCache" + File.separator);
        setSaveEnabled(true);
    }
}
