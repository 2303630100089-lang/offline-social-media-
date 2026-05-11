package com.meshverse.app.miniapp

import android.content.Context
import android.webkit.WebSettings
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

object MiniAppWebViewFactory {

    fun create(context: Context): WebView = WebView(context).apply {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
        settings.allowContentAccess = false
        settings.allowFileAccess = true
        settings.allowFileAccessFromFileURLs = false
        settings.allowUniversalAccessFromFileURLs = false
        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString().orEmpty()
                return !url.startsWith("file:///android_asset/miniapps/")
            }
        }
        addJavascriptInterface(MiniAppBridge(context), "MeshVerseBridge")
    }
}
