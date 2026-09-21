package com.example.mobile_app.ui.screens.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobile_app.data.model.address.GeoCoverageZone
import com.example.mobile_app.ui.theme.Primary
import com.example.mobile_app.util.tr
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polygon
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch
import org.json.JSONArray

/** Bitta xizmat hududi: chizish uchun nuqtalar + rang. */
private data class CoveragePolygon(
    val name: String?,
    val points: List<LatLng>,
    val color: Color
)

@Composable
fun MapPickerScreen(
    initialLat: Double? = null,
    initialLng: Double? = null,
    onBack: () -> Unit = {},
    onLoadCoverage: suspend () -> List<GeoCoverageZone> = { emptyList() },
    onConfirm: (lat: Double, lng: Double) -> Unit = { _, _ -> }
) {
    var picked by remember(initialLat, initialLng) {
        mutableStateOf(
            if (initialLat != null && initialLng != null) LatLng(initialLat, initialLng) else null
        )
    }
    var zones by remember { mutableStateOf<List<CoveragePolygon>>(emptyList()) }
    var zonesLoaded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    // Foydalanuvchi joylashuvi — xarita ochilganda yaqin hududga 18x zoom.
    var userPos by remember { mutableStateOf<LatLng?>(null) }
    var locationAsked by remember { mutableStateOf(false) }
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.any { it }) {
            userPos = lastKnownLatLng(context)
        }
        locationAsked = true
    }

    // Xizmat hududlari (geo-service poligonlari) — bir marta yuklanadi.
    LaunchedEffect(Unit) {
        if (!locationAsked && !hasLocationPermission(context)) {
            permissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else if (!locationAsked) {
            userPos = lastKnownLatLng(context)
            locationAsked = true
        }
        try {
            zones = onLoadCoverage()
                .mapNotNull { z ->
                    val pts = parsePolygonPoints(z.polygon)
                    if (pts.size < 3) null
                    else CoveragePolygon(z.name, pts, parseZoneColor(z.color))
                }
        } catch (_: Exception) {
            zones = emptyList()
        } finally {
            zonesLoaded = true
        }
    }

    // Tanlangan nuqta biror xizmat hududi ichidami? Hudud yo'q bo'lsa — cheklov yo'q.
    val pickedInside = picked?.let { p -> zones.isEmpty() || zones.any { isInside(p, it.points) } } ?: false

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            picked ?: userPos ?: zones.firstOrNull()?.let { centroid(it.points) } ?: LatLng(41.311081, 69.240562),
            15f
        )
    }
    // Kamera avtomatik yaqinlashuvi (faqat 1 marta — user harakatiga aralashmaydi).
    // Ustuvorlik: tanlangan nuqta > user joylashuvi > birinchi hudud.
    // Nuqta uchun 15x, hudud sharhi uchun 12x.
    var autoZoomDone by remember { mutableStateOf(false) }
    LaunchedEffect(userPos, zonesLoaded) {
        if (autoZoomDone || picked != null) return@LaunchedEffect
        val target = userPos ?: if (zones.isNotEmpty()) centroid(zones.first().points) else null
        if (target != null && (userPos != null || zonesLoaded)) {
            autoZoomDone = true
            val zoom = if (userPos != null) 15f else 12f
            scope.launch {
                try {
                    cameraPositionState.animate(
                        com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(target, zoom),
                        800
                    )
                } catch (_: Exception) {}
            }
        }
    }

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        containerColor = Color.White
    ) { padding ->
        // Xarita to'liq edge-to-edge: tepada status-bar ostigacha, pastda
        // bottom-sheet ostigacha kiradi (orqa fonda ko'rinadi).
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                onMapClick = { latLng -> picked = latLng }
            ) {
                // Xizmat hududlari — HAMMASI bir xil binafsha rangda.
                zones.forEach { zone ->
                    Polygon(
                        points = zone.points,
                        fillColor = Primary.copy(alpha = 0.15f),
                        strokeColor = Primary,
                        strokeWidth = 5f
                    )
                }
                picked?.let {
                    Marker(
                        state = MarkerState(position = it),
                        title = if (pickedInside) null else tr("map_picker_outside")
                    )
                }
            }

            // Orqaga — suzuvchi oq dumaloq tugma (status-bar ostida).
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(12.dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color(0xFF0F172A))
            }

            // Pastki karta — xarita ustiga chiqadi (xarita uning tagigacha kiradi).
            Card(
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(8.dp),
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp)
                ) {
                    if (!zonesLoaded) {
                        androidx.compose.foundation.layout.Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                color = Primary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = tr("map_picker_loading_zones"),
                                fontSize = 13.sp,
                                color = Color(0xFF6B7280)
                            )
                        }
                    } else if (picked != null && !pickedInside) {
                        Text(
                            text = tr("map_picker_outside"),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFEF4444)
                        )
                    } else {
                        Text(
                            text = picked?.let { "${it.latitude}, ${it.longitude}" } ?: tr("map_picker_hint"),
                            fontSize = 13.sp,
                            color = Color(0xFF6B7280)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            picked?.let { onConfirm(it.latitude, it.longitude) }
                        },
                        enabled = picked != null && pickedInside,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Text(tr("map_picker_confirm"), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
            }
        }
    }
}

/** Lokatsiya ruxsati bormi. */
private fun hasLocationPermission(context: android.content.Context): Boolean {
    return androidx.core.content.ContextCompat.checkSelfPermission(
        context, android.Manifest.permission.ACCESS_FINE_LOCATION
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
}

/** Oxirgi ma'lum joylashuv (GPS → Network). Yangi dependency shart emas. */
private fun lastKnownLatLng(context: android.content.Context): LatLng? {
    if (!hasLocationPermission(context)) return null
    return try {
        val lm = context.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
        val gps = try {
            lm.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
        } catch (_: Exception) { null }
        val net = try {
            lm.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
        } catch (_: Exception) { null }
        val best = if (gps != null && net != null) {
            if (gps.time >= net.time) gps else net
        } else gps ?: net
        best?.let { LatLng(it.latitude, it.longitude) }
    } catch (_: Exception) {
        null
    }
}

/** Backend polygon formati: [[lat,lng],[lat,lng],...] JSON string. */
private fun parsePolygonPoints(raw: String?): List<LatLng> {
    if (raw.isNullOrBlank()) return emptyList()
    return try {
        val arr = JSONArray(raw.trim())
        List(arr.length()) { i ->
            val p = arr.getJSONArray(i)
            LatLng(p.getDouble(0), p.getDouble(1))
        }.filter { it.latitude in -90.0..90.0 && it.longitude in -180.0..180.0 }
    } catch (_: Exception) {
        emptyList()
    }
}

/** Ray-casting: nuqta poligon ichidami. */
private fun isInside(point: LatLng, polygon: List<LatLng>): Boolean {
    if (polygon.size < 3) return false
    var inside = false
    var j = polygon.size - 1
    for (i in polygon.indices) {
        val xi = polygon[i].longitude
        val yi = polygon[i].latitude
        val xj = polygon[j].longitude
        val yj = polygon[j].latitude
        if ((yi > point.latitude) != (yj > point.latitude) &&
            point.longitude < (xj - xi) * (point.latitude - yi) / (yj - yi) + xi
        ) {
            inside = !inside
        }
        j = i
    }
    return inside
}

private fun centroid(points: List<LatLng>): LatLng {
    if (points.isEmpty()) return LatLng(41.311081, 69.240562)
    return LatLng(
        points.sumOf { it.latitude } / points.size,
        points.sumOf { it.longitude } / points.size
    )
}

/** Zona rangi (#RRGGBB) — xato bo'lsa Primary. */
private fun parseZoneColor(code: String?): Color {
    if (code.isNullOrBlank()) return Primary
    return try {
        val hex = code.trim().removePrefix("#")
        val full = when (hex.length) {
            3 -> "FF" + hex.map { "$it$it" }.joinToString("")
            6 -> "FF$hex"
            8 -> hex
            else -> return Primary
        }
        Color(full.toLong(16))
    } catch (_: Exception) {
        Primary
    }
}
