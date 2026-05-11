package com.meshverse.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meshverse.app.maps.OfflinePoiProvider
import com.meshverse.app.ui.viewmodel.MainViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun MapScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val peers by viewModel.peers.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Offline Map + Peer Overlay")
        AndroidView(
            factory = {
                MapView(context).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(13.0)
                    controller.setCenter(GeoPoint(28.6139, 77.2090))
                }
            },
            modifier = Modifier.fillMaxWidth().height(340.dp),
            update = { mapView ->
                mapView.overlays.removeAll { it is Marker }
                peers.forEachIndexed { index, peer ->
                    val lat = peer.latitude ?: (28.6139 + (index * 0.005))
                    val lon = peer.longitude ?: (77.2090 + (index * 0.005))
                    val marker = Marker(mapView).apply {
                        position = GeoPoint(lat, lon)
                        title = peer.deviceName
                        snippet = if (peer.isConnected) "Connected peer" else "Last seen node"
                    }
                    mapView.overlays.add(marker)
                }
                OfflinePoiProvider.defaultPois().forEach { poi ->
                    val marker = Marker(mapView).apply {
                        position = GeoPoint(poi.latitude, poi.longitude)
                        title = poi.name
                        snippet = poi.category
                    }
                    mapView.overlays.add(marker)
                }
                mapView.invalidate()
            }
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Emergency Overlay")
                Text("Hospitals, shelters and crowd reports are available offline through cached tiles + mesh updates.")
            }
        }

        DisposableEffect(Unit) {
            onDispose { Configuration.getInstance().save(context, context.getSharedPreferences("osmdroid", 0)) }
        }
    }
}
