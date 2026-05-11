package com.meshverse.app.miniapp

import android.content.Context
import android.webkit.WebSettings
import android.webkit.WebView

object MiniAppWebViewFactory {

    fun create(context: Context): WebView = WebView(context).apply {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
        settings.allowContentAccess = false
        settings.allowFileAccess = false
        settings.allowFileAccessFromFileURLs = false
        settings.allowUniversalAccessFromFileURLs = false
        addJavascriptInterface(MiniAppBridge(context), "MeshVerseBridge")
    }
}
