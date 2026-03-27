package com.smarttravel.app.data.remote.model

import com.google.gson.annotations.SerializedName

// ── Nominatim Models ───────────────────────────────────────────────────────
data class NominatimPlace(
    @SerializedName("place_id") val placeId: Long,
    @SerializedName("display_name") val displayName: String,
    @SerializedName("lat") val lat: String,
    @SerializedName("lon") val lon: String,
    @SerializedName("type") val type: String,
    @SerializedName("address") val address: NominatimAddress?
)

data class NominatimAddress(
    @SerializedName("road") val road: String?,
    @SerializedName("city") val city: String?,
    @SerializedName("state") val state: String?,
    @SerializedName("country") val country: String?,
    @SerializedName("postcode") val postcode: String?
)

data class NominatimReverseResult(
    @SerializedName("display_name") val displayName: String,
    @SerializedName("lat") val lat: String,
    @SerializedName("lon") val lon: String,
    @SerializedName("address") val address: NominatimAddress?
)

// ── OSRM Models ────────────────────────────────────────────────────────────
data class OsrmRouteResponse(
    @SerializedName("code") val code: String,
    @SerializedName("routes") val routes: List<OsrmRoute>?,
    @SerializedName("waypoints") val waypoints: List<OsrmWaypoint>?
)

data class OsrmRoute(
    @SerializedName("geometry") val geometry: String,         // encoded polyline
    @SerializedName("distance") val distance: Double,         // meters
    @SerializedName("duration") val duration: Double,         // seconds
    @SerializedName("legs") val legs: List<OsrmLeg>
)

data class OsrmLeg(
    @SerializedName("distance") val distance: Double,
    @SerializedName("duration") val duration: Double,
    @SerializedName("steps") val steps: List<OsrmStep>
)

data class OsrmStep(
    @SerializedName("distance") val distance: Double,
    @SerializedName("duration") val duration: Double,
    @SerializedName("name") val name: String,
    @SerializedName("maneuver") val maneuver: OsrmManeuver?
)

data class OsrmManeuver(
    @SerializedName("type") val type: String,
    @SerializedName("modifier") val modifier: String?
)

data class OsrmWaypoint(
    @SerializedName("name") val name: String,
    @SerializedName("location") val location: List<Double>
)

// ── Overpass Models ────────────────────────────────────────────────────────
data class OverpassResponse(
    @SerializedName("elements") val elements: List<OverpassElement>
)

data class OverpassElement(
    @SerializedName("type") val type: String,
    @SerializedName("id") val id: Long,
    @SerializedName("lat") val lat: Double?,
    @SerializedName("lon") val lon: Double?,
    @SerializedName("tags") val tags: Map<String, String>?
) {
    val name: String get() = tags?.get("name") ?: "Unknown"
    val amenity: String get() = tags?.get("amenity") ?: tags?.get("tourism") ?: ""
    val address: String get() {
        val street = tags?.get("addr:street") ?: ""
        val city = tags?.get("addr:city") ?: ""
        return listOf(street, city).filter { it.isNotEmpty() }.joinToString(", ")
    }
}

// ── App-internal Place model ───────────────────────────────────────────────
data class NearbyPlace(
    val id: Long,
    val name: String,
    val category: PlaceCategory,
    val lat: Double,
    val lon: Double,
    val address: String,
    val distanceMeters: Double = 0.0
)

enum class PlaceCategory(val displayName: String, val overpassTag: String) {
    HOTEL("Hotels", "tourism=hotel"),
    RAILWAY("Railway Stations", "railway=station"),
    BUS_STOP("Bus Stops", "highway=bus_stop"),
    RESTAURANT("Restaurants", "amenity=restaurant")
}
