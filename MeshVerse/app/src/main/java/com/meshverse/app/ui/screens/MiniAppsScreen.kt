package com.meshverse.app.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.meshverse.app.miniapp.MiniAppWebViewFactory

@Composable
fun MiniAppsScreen() {
    AndroidView(
        factory = { context -> MiniAppWebViewFactory.create(context).apply { loadUrl("file:///android_asset/miniapps/index.html") } },
        modifier = Modifier.fillMaxSize()
    )
}
