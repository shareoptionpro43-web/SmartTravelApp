package com.smarttravel.app.data.remote.api

import com.smarttravel.app.data.remote.model.*
import retrofit2.Response
import retrofit2.http.*

// ── Nominatim (Geocoding / Search) ─────────────────────────────────────────
interface NominatimApi {
    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("format") format: String = "json",
        @Query("addressdetails") addressDetails: Int = 1,
        @Query("limit") limit: Int = 10,
        @Query("countrycodes") countryCodes: String = "in"
    ): Response<List<NominatimPlace>>

    @GET("reverse")
    suspend fun reverseGeocode(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("format") format: String = "json"
    ): Response<NominatimReverseResult>
}

// ── OSRM (Route Calculation) ───────────────────────────────────────────────
interface OsrmApi {
    @GET("route/v1/driving/{coordinates}")
    suspend fun getRoute(
        @Path("coordinates") coordinates: String,  // "lon1,lat1;lon2,lat2"
        @Query("overview") overview: String = "full",
        @Query("geometries") geometries: String = "polyline",
        @Query("steps") steps: Boolean = true
    ): Response<OsrmRouteResponse>
}

// ── Overpass (Nearby Places) ───────────────────────────────────────────────
interface OverpassApi {
    @POST("api/interpreter")
    @FormUrlEncoded
    suspend fun query(
        @Field("data") query: String
    ): Response<OverpassResponse>
}
