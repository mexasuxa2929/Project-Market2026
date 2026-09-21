package mexa.club.desktop_app.market.ui.maps

data class MapZoneData(
    val id: String,
    val name: String,
    val region: String,
    val district: String,
    val centerLat: Double,
    val centerLng: Double,
    val polygonCoords: List<List<Double>>,
    val color: String,
    val active: Boolean,
)

fun zonesToJson(zones: List<MapZoneData>): String {
    val sb = StringBuilder("[")
    zones.forEachIndexed { i, z ->
        if (i > 0) sb.append(",")
        sb.append("{\"id\":\"${esc(z.id)}\",\"name\":\"${esc(z.name)}\",")
        sb.append("\"region\":\"${esc(z.region)}\",\"district\":\"${esc(z.district)}\",")
        sb.append("\"centerLat\":${z.centerLat},\"centerLng\":${z.centerLng},")
        sb.append("\"color\":\"${esc(z.color)}\",")
        sb.append("\"polygonCoords\":${coordsToJson(z.polygonCoords)}")
        sb.append("}")
    }
    sb.append("]")
    return sb.toString()
}

data class WarehouseListData(
    val id: String,
    val name: String,
)

fun warehouseListToJson(list: List<WarehouseListData>): String {
    val sb = StringBuilder("[")
    list.forEachIndexed { i, w ->
        if (i > 0) sb.append(",")
        sb.append("{\"id\":\"${esc(w.id)}\",\"name\":\"${esc(w.name)}\"}")
    }
    sb.append("]")
    return sb.toString()
}

fun esc(s: String): String =
    s.replace("\\", "\\\\").replace("'", "\\'").replace("\"", "\\\"")

fun coordsToJson(coords: List<List<Double>>): String {
    val sb = StringBuilder("[")
    coords.forEachIndexed { i, c ->
        if (i > 0) sb.append(",")
        sb.append("[${c[0]},${c[1]}]")
    }
    sb.append("]")
    return sb.toString()
}
