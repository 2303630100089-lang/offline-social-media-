package com.meshverse.app.ui.screens

import android.Manifest
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.meshverse.app.maps.OfflinePoiProvider
import com.meshverse.app.ui.viewmodel.MainViewModel
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val peers by viewModel.peers.collectAsStateWithLifecycle()

    // Track device GPS location in Compose state
    var deviceLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var locationStatus by remember { mutableStateOf("Acquiring GPS…") }

    // Request location permissions
    val locationPermissions = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    LaunchedEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
        if (!locationPermissions.allPermissionsGranted) {
            locationPermissions.launchMultiplePermissionRequest()
        }
    }

    // Start listening for GPS updates using Android LocationManager
    DisposableEffect(locationPermissions.allPermissionsGranted) {
        if (!locationPermissions.allPermissionsGranted) return@DisposableEffect onDispose {}

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val listener = object : LocationListener {
            override fun onLocationChanged(loc: Location) {
                deviceLocation = GeoPoint(loc.latitude, loc.longitude)
                locationStatus = "GPS: %.4f, %.4f".format(loc.latitude, loc.longitude)
            }
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
        }

        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER
        )
        providers.forEach { provider ->
            runCatching {
                if (locationManager.isProviderEnabled(provider)) {
                    locationManager.requestLocationUpdates(provider, 5_000L, 10f, listener)
                    // Use last known location immediately if available
                    locationManager.getLastKnownLocation(provider)?.let { last ->
                        if (deviceLocation == null) {
                            deviceLocation = GeoPoint(last.latitude, last.longitude)
                            locationStatus = "Last GPS: %.4f, %.4f".format(last.latitude, last.longitude)
                        }
                    }
                }
            }
        }

        onDispose {
            runCatching { locationManager.removeUpdates(listener) }
            Configuration.getInstance().save(context, context.getSharedPreferences("osmdroid", 0))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Offline Map + Peer Overlay", style = MaterialTheme.typography.headlineSmall)
        Text(locationStatus, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary)

        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(15.0)
                    controller.setCenter(GeoPoint(28.6139, 77.2090))
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp),
            update = { mapView ->
                // Add MyLocationNewOverlay once if not already present and permission is granted
                val hasLocationOverlay = mapView.overlays.any { it is MyLocationNewOverlay }
                if (locationPermissions.allPermissionsGranted && !hasLocationOverlay) {
                    val myLocationOverlay = MyLocationNewOverlay(
                        GpsMyLocationProvider(mapView.context), mapView
                    ).apply { enableMyLocation() }
                    mapView.overlays.add(0, myLocationOverlay)
                }

                // Remove peer/POI Marker overlays (preserve MyLocationOverlay)
                mapView.overlays.removeAll { it is Marker }

                // Self-location marker (supplement to MyLocationOverlay)
                deviceLocation?.let { loc ->
                    mapView.controller.animateTo(loc)
                    val selfMarker = Marker(mapView).apply {
                        position = loc
                        title = "You are here"
                        snippet = "%.4f, %.4f".format(loc.latitude, loc.longitude)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    }
                    mapView.overlays.add(selfMarker)
                }

                // Peer markers
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

                // Offline POI markers
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
                Text("Emergency Overlay", style = MaterialTheme.typography.titleSmall)
                Text("Hospitals, shelters and crowd reports are available offline through cached tiles + mesh updates.")
            }
        }
    }
}

