package com.meshverse.app.miniapp

import android.content.Context
import android.webkit.JavascriptInterface
import android.widget.Toast

class MiniAppBridge(private val context: Context) {

    @JavascriptInterface
    fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    @JavascriptInterface
    fun getPaymentDisclaimer(): String = "Prototype only. Not real banking infrastructure."

    @JavascriptInterface
    fun getDeviceMode(): String = "offline-mesh"
}
