package com.example.mobile_app.data.model.address

import androidx.compose.runtime.Immutable

/**
 * Manzil modeli: id + name + lat + lng (viloyat/tuman matn maydonlari olib tashlandi).
 */
@Immutable
data class AddressResponse(
    val id: String,
    val label: String?,
    val name: String?,
    val latitude: Double?,
    val longitude: Double?,
    val line2: String?,
    val phone: String?,
    val defaultAddress: Boolean?,
    val createdAt: String?,
    val updatedAt: String?
) {
    val displayName: String
        get() = name?.trim().takeIf { !it.isNullOrBlank() }
            ?: label?.trim().takeIf { !it.isNullOrBlank() }
            ?: ""
}

data class CreateAddressRequest(
    val label: String? = null,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val line2: String? = null,
    val phone: String? = null,
    val defaultAddress: Boolean = false
)

data class UpdateAddressRequest(
    val label: String? = null,
    val name: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val line2: String? = null,
    val phone: String? = null,
    val defaultAddress: Boolean? = null
)

/** Geo qidiruv natijasi: nom + nuqta */
@Immutable
data class GeoPlace(
    val name: String,
    val lat: Double,
    val lng: Double
)

data class GeoSearchApiWrapper(
    val success: Boolean = false,
    val data: List<GeoPlace>?
)

data class GeoReverseApiWrapper(
    val success: Boolean = false,
    val data: GeoPlace?
)

/** Xizmat hududi (geo-service poligoni): xaritada chiziladi. */
@Immutable
data class GeoCoverageZone(
    val id: String?,
    val name: String?,
    val polygon: String?,
    val color: String?
)

data class GeoCoverageApiWrapper(
    val success: Boolean = false,
    val data: List<GeoCoverageZone>?
)

/** Nuqta xizmat hududidami (zona nomi bilan). */
@Immutable
data class CoverageCheckResult(
    val served: Boolean = false,
    val zoneName: String? = null
)

data class CoverageCheckApiWrapper(
    val success: Boolean = false,
    val data: CoverageCheckResult?
)

data class AddressApiWrapper(
    val success: Boolean,
    val data: AddressResponse?,
    val error: ErrorData?
)

data class AddressListWrapper(
    val success: Boolean,
    val data: List<AddressResponse>?,
    val error: ErrorData?
)

data class ErrorData(val code: String?, val message: String?)
