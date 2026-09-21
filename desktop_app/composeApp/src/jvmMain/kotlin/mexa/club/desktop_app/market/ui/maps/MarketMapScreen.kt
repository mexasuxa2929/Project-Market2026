package mexa.club.desktop_app.market.ui.maps

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import mexa.club.desktop_app.market.api.ApiClient
import mexa.club.desktop_app.market.api.stringField

private data class GeoZoneData(
    val id: String,
    val name: String,
    val region: String,
    val district: String,
    val centerLat: String,
    val centerLng: String,
    val polygon: String,
    val color: String,
    val active: Boolean,
)

data class WarehousePosition(
    val latitude: Double,
    val longitude: Double,
)

/**
 * Xarita ekranining holati navigatsiya orasida saqlanadi.
 * Ekran boshqa sahifaga o'tganda `remember` holati yo'qolardi,
 * natijada qaytganda poligonlar va tanlangan ombor yo'qolardi.
 */
object MapScreenStateHolder {
    var selectedWarehouseId by mutableStateOf<String?>(null)
    var mapZones by mutableStateOf<List<MapZoneData>>(emptyList())
    var mapAllWarehouses by mutableStateOf<List<WarehouseListData>>(emptyList())
    var warehousePosition by mutableStateOf<WarehousePosition?>(null)

    fun reset() {
        selectedWarehouseId = null
        mapZones = emptyList()
        mapAllWarehouses = emptyList()
        warehousePosition = null
    }
}

private fun parsePolygonCoords(polygonStr: String): List<List<Double>> {
    if (polygonStr.isBlank() || !polygonStr.startsWith("[")) return emptyList()
    val result = mutableListOf<List<Double>>()
    return try {
        val arr = ApiClient.json().parseToJsonElement(polygonStr) as? JsonArray ?: return emptyList()
        for (el in arr) {
            val pair = el as? JsonArray ?: continue
            val lng = pair.getOrNull(1)?.let { (it as? JsonPrimitive)?.content?.toDoubleOrNull() }
            val lat = pair.getOrNull(0)?.let { (it as? JsonPrimitive)?.content?.toDoubleOrNull() }
            if (lat != null && lng != null) result.add(listOf(lng, lat))
        }
        result
    } catch (_: Exception) {
        emptyList()
    }
}

@Composable
fun MarketMapScreen(modifier: Modifier = Modifier) {
    var loading by remember { mutableStateOf(true) }
    var mapZones by remember { mutableStateOf(MapScreenStateHolder.mapZones) }
    var mapAllWarehouses by remember { mutableStateOf(MapScreenStateHolder.mapAllWarehouses) }
    var selectedWarehouseId by remember { mutableStateOf(MapScreenStateHolder.selectedWarehouseId) }
    var warehousePosition by remember { mutableStateOf(MapScreenStateHolder.warehousePosition) }
    val scope = rememberCoroutineScope()

    suspend fun fetchZonesByWarehouse(whId: String): List<GeoZoneData>? = withContext(Dispatchers.IO) {
        val url = "/api/admin/geo-zones/by-warehouse/$whId"
        val text = runCatching { ApiClient.get(url) }.getOrElse { return@withContext null }
        val data = ApiClient.parseJsonObject(text)?.let { it["data"] as? JsonArray }
            ?: ApiClient.parseJsonObject(text)?.let { it["content"] as? JsonArray }
            ?: return@withContext emptyList()
        data.mapNotNull { el ->
            val o = el as? JsonObject ?: return@mapNotNull null
            GeoZoneData(
                id = o.stringField("id"),
                name = o.stringField("name"),
                region = o.stringField("region"),
                district = o.stringField("district"),
                centerLat = o.stringField("centerLat"),
                centerLng = o.stringField("centerLng"),
                polygon = o.stringField("polygon"),
                color = o.stringField("color").ifEmpty { "#6366F1" },
                active = (o["active"] as? JsonPrimitive)?.booleanOrNull ?: true,
            )
        }
    }

    suspend fun fetchAllWarehousesList(): List<WarehouseListData>? = withContext(Dispatchers.IO) {
        val text = runCatching { ApiClient.get("/api/warehouses") }.getOrElse { return@withContext null }
        val data = ApiClient.parseJsonObject(text)?.let { it["data"] as? JsonObject }?.let { it["content"] as? JsonArray }
            ?: ApiClient.parseJsonObject(text)?.let { it["content"] as? JsonArray }
            ?: return@withContext emptyList()
        data.mapNotNull { el ->
            val o = el as? JsonObject ?: return@mapNotNull null
            WarehouseListData(
                id = o.stringField("id"),
                name = o.stringField("name"),
            )
        }
    }

    suspend fun fetchWarehousePosition(whId: String): WarehousePosition? = withContext(Dispatchers.IO) {
        val text = runCatching { ApiClient.get("/api/admin/geo-zones/warehouse/$whId/position") }.getOrNull() ?: return@withContext null
        val data = ApiClient.parseJsonObject(text)?.let { it["data"] as? JsonObject } ?: return@withContext null
        val lat = (data["latitude"] as? JsonPrimitive)?.content?.toDoubleOrNull() ?: return@withContext null
        val lng = (data["longitude"] as? JsonPrimitive)?.content?.toDoubleOrNull() ?: return@withContext null
        WarehousePosition(lat, lng)
    }

    fun reloadZones(whId: String?) {
        scope.launch {
            if (whId == null) {
                mapZones = emptyList()
                MapScreenStateHolder.mapZones = mapZones
                warehousePosition = null
                MapScreenStateHolder.warehousePosition = null
                return@launch
            }
            val zones = fetchZonesByWarehouse(whId) ?: run {
                return@launch
            }
            mapZones = zones.map { z ->
                MapZoneData(
                    id = z.id,
                    name = z.name,
                    region = z.region,
                    district = z.district,
                    centerLat = z.centerLat.toDoubleOrNull() ?: 41.377491,
                    centerLng = z.centerLng.toDoubleOrNull() ?: 64.585262,
                    polygonCoords = parsePolygonCoords(z.polygon),
                    color = z.color.ifEmpty { "#6366F1" },
                    active = z.active,
                )
            }
            MapScreenStateHolder.mapZones = mapZones
            warehousePosition = fetchWarehousePosition(whId)
            MapScreenStateHolder.warehousePosition = warehousePosition
        }
    }

    fun onWarehouseSelected(whId: String?) {
        selectedWarehouseId = whId
        MapScreenStateHolder.selectedWarehouseId = whId
        reloadZones(whId)
    }

    fun reloadAllWarehousesList() {
        scope.launch {
            val fetched = fetchAllWarehousesList()
            if (fetched == null) {
                return@launch
            }
            mapAllWarehouses = fetched
            MapScreenStateHolder.mapAllWarehouses = mapAllWarehouses
        }
    }

    LaunchedEffect(Unit) {
        reloadAllWarehousesList()
        val restoredWhId = selectedWarehouseId
        if (restoredWhId != null) {
            reloadZones(restoredWhId)
        }
    }

    Box(modifier.fillMaxSize()) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
        JcefMapView(
            zones = mapZones,
            warehouseList = mapAllWarehouses,
            selectedWarehouseId = selectedWarehouseId,
            warehousePositionLat = warehousePosition?.latitude,
            warehousePositionLng = warehousePosition?.longitude,
            selectedZoneId = null,
            onZoneClick = {},
            onSaveZone = { method, id, body ->
                Thread {
                    try {
                        val r = runBlocking {
                            when (method) {
                                "POST" -> ApiClient.post("/api/admin/geo-zones", body)
                                "PUT" -> ApiClient.put("/api/admin/geo-zones/$id", body)
                                "DELETE" -> ApiClient.delete("/api/admin/geo-zones/$id")
                                else -> throw IllegalArgumentException("Unknown method: $method")
                            }
                        }
                        reloadZones(selectedWarehouseId)
                    } catch (e: Exception) {
                    }
                }.start()
            },
            onWarehouseSelected = { whId -> onWarehouseSelected(whId) },
            onWarehouseMoved = { whId, lat, lng ->
                warehousePosition = WarehousePosition(lat, lng)
                MapScreenStateHolder.warehousePosition = warehousePosition
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            ApiClient.put("/api/admin/geo-zones/warehouse/$whId/position?latitude=$lat&longitude=$lng", "")
                        }
                    } catch (e: Exception) {
                    }
                }
            },
            onWarehousePositionDeleted = { whId ->
                warehousePosition = null
                MapScreenStateHolder.warehousePosition = null
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            ApiClient.delete("/api/admin/geo-zones/warehouse/$whId/position")
                        }
                    } catch (e: Exception) {
                    }
                }
            },
            onReloadZones = {
                reloadZones(selectedWarehouseId)
            },
            onReady = { loading = false },
            modifier = Modifier.fillMaxSize(),
        )
    }
}
