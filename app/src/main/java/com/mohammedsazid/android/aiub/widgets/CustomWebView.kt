package com.mohammedsazid.android.aiub.widgets

import android.content.Context
import android.util.AttributeSet

import com.mohammedsazid.android.aiub.R

import java.io.File

class CustomWebView : AdvancedWebView {
    constructor(context: Context) : super(context) {
        setupWebView()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        setupWebView()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        setupWebView()
    }

    private fun setupWebView() {
        setCookiesEnabled(true)
        setThirdPartyCookiesEnabled(true)
        setDesktopMode(false)
        settings.setSupportZoom(true)
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        settings.loadsImagesAutomatically = true

        addHttpHeader("X-Requested-With", context.getString(R.string.app_name))
        settings.setAppCacheEnabled(true)
        settings.setAppCachePath(context.cacheDir.absolutePath
                + File.separator + "appCache" + File.separator)
        isSaveEnabled = true
    }
}
