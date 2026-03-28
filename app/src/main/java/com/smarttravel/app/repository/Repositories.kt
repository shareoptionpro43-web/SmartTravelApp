package com.smarttravel.app.repository

import com.smarttravel.app.data.local.dao.*
import com.smarttravel.app.data.local.entity.*
import com.smarttravel.app.data.remote.api.*
import com.smarttravel.app.data.remote.model.*
import com.smarttravel.app.utils.PolylineDecoder
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val cause: Throwable? = null) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

// ── Location Repository ────────────────────────────────────────────────────
@Singleton
class LocationRepository @Inject constructor(
    private val sessionDao: TravelSessionDao,
    private val pointDao: LocationPointDao
) {
    suspend fun startSession(lat: Double, lng: Double, date: String): Long {
        val session = TravelSession(
            startTime = System.currentTimeMillis(),
            startLat = lat,
            startLng = lng,
            date = date
        )
        return sessionDao.insert(session)
    }

    suspend fun endSession(sessionId: Long, endLat: Double, endLng: Double) {
        val session = sessionDao.getSessionById(sessionId) ?: return
        val points = pointDao.getPointsForSession(sessionId)
        val distance = calculateTotalDistance(points)
        val duration = (System.currentTimeMillis() - session.startTime) / 60000
        sessionDao.update(
            session.copy(
                endTime = System.currentTimeMillis(),
                endLat = endLat,
                endLng = endLng,
                totalDistanceKm = distance,
                totalDurationMin = duration
            )
        )
    }

    suspend fun addLocationPoint(sessionId: Long, lat: Double, lng: Double, accuracy: Float, speed: Float) {
        pointDao.insert(
            LocationPoint(
                sessionId = sessionId,
                latitude = lat,
                longitude = lng,
                accuracy = accuracy,
                speed = speed,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun getActiveSession() = sessionDao.getActiveSession()
    fun getAllSessions() = sessionDao.getAllSessions()
    fun getSessionsByDate(date: String) = sessionDao.getSessionsByDate(date)
    suspend fun getPointsForSession(id: Long) = pointDao.getPointsForSession(id)

    private fun calculateTotalDistance(points: List<LocationPoint>): Double {
        if (points.size < 2) return 0.0
        var total = 0.0
        for (i in 1 until points.size) {
            total += haversineDistance(
                points[i - 1].latitude, points[i - 1].longitude,
                points[i].latitude, points[i].longitude
            )
        }
        return total
    }

    fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
    }
}

// ── Route Repository ───────────────────────────────────────────────────────
@Singleton
class RouteRepository @Inject constructor(
    private val osrmApi: OsrmApi
) {
    data class RouteResult(
        val distanceKm: Double,
        val durationMin: Double,
        val polylinePoints: List<Pair<Double, Double>>,
        val steps: List<OsrmStep>
    )

    suspend fun getRoute(srcLat: Double, srcLon: Double, dstLat: Double, dstLon: Double): Result<RouteResult> {
        return try {
            val coords = "$srcLon,$srcLat;$dstLon,$dstLat"
            val response = osrmApi.getRoute(coords)
            if (response.isSuccessful) {
                val route = response.body()?.routes?.firstOrNull()
                    ?: return Result.Error("No route found")
                val points = PolylineDecoder.decode(route.geometry)
                Result.Success(
                    RouteResult(
                        distanceKm = route.distance / 1000,
                        durationMin = route.duration / 60,
                        polylinePoints = points,
                        steps = route.legs.flatMap { it.steps }
                    )
                )
            } else {
                Result.Error("OSRM error: ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error", e)
        }
    }
}

// ── Search Repository ──────────────────────────────────────────────────────
@Singleton
class SearchRepository @Inject constructor(
    private val nominatimApi: NominatimApi
) {
    suspend fun searchPlaces(query: String): Result<List<NominatimPlace>> {
        return try {
            val response = nominatimApi.search(query)
            if (response.isSuccessful) {
                Result.Success(response.body() ?: emptyList())
            } else {
                Result.Error("Search failed: ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error", e)
        }
    }

    suspend fun reverseGeocode(lat: Double, lon: Double): Result<NominatimReverseResult> {
        return try {
            val response = nominatimApi.reverseGeocode(lat, lon)
            if (response.isSuccessful && response.body() != null) {
                Result.Success(response.body()!!)
            } else {
                Result.Error("Reverse geocode failed")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error", e)
        }
    }
}

// ── Nearby Repository ──────────────────────────────────────────────────────
@Singleton
class NearbyRepository @Inject constructor(
    private val overpassApi: OverpassApi,
    private val locationRepository: LocationRepository
) {
    suspend fun getNearbyPlaces(lat: Double, lon: Double, category: PlaceCategory, radiusMeters: Int = 2000): Result<List<NearbyPlace>> {
        return try {
            val tag = category.overpassTag.split("=")
            val key = tag[0]; val value = tag[1]
            val query = """
                [out:json][timeout:25];
                (
                  node["$key"="$value"](around:$radiusMeters,$lat,$lon);
                  way["$key"="$value"](around:$radiusMeters,$lat,$lon);
                );
                out center;
            """.trimIndent()
            val response = overpassApi.query(query)
            if (response.isSuccessful) {
                val elements = response.body()?.elements ?: emptyList()
                val places = elements.mapNotNull { el ->
                    val elLat = el.lat ?: return@mapNotNull null
                    val elLon = el.lon ?: return@mapNotNull null
                    NearbyPlace(
                        id = el.id,
                        name = el.name,
                        category = category,
                        lat = elLat,
                        lon = elLon,
                        address = el.address,
                        distanceMeters = locationRepository.haversineDistance(lat, lon, elLat, elLon) * 1000
                    )
                }.sortedBy { it.distanceMeters }
                Result.Success(places)
            } else {
                Result.Error("Overpass error: ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error", e)
        }
    }
}

// ── Analytics Repository ───────────────────────────────────────────────────
@Singleton
class AnalyticsRepository @Inject constructor(
    private val sessionDao: TravelSessionDao,
    private val summaryDao: DailySummaryDao
) {
    fun getRecentSummaries(limit: Int = 7) = summaryDao.getRecentSummaries(limit)
    fun getAllSummaries() = summaryDao.getAllSummaries()
    fun getAllSessions() = sessionDao.getAllSessions()
    fun getSessionsInRange(start: String, end: String) = sessionDao.getSessionsInRange(start, end)
    suspend fun getAllSessionsOnce() = sessionDao.getRecentSessions(1000)
}

// ── Saved Place Repository ─────────────────────────────────────────────────
@Singleton
class SavedPlaceRepository @Inject constructor(
    private val placeDao: SavedPlaceDao
) {
    fun getAllPlaces(): Flow<List<SavedPlace>> = placeDao.getAllPlaces()
    suspend fun savePlace(place: SavedPlace): Long = placeDao.insert(place)
    suspend fun updatePlace(place: SavedPlace) = placeDao.update(place)
    suspend fun deletePlace(place: SavedPlace) = placeDao.delete(place)
    suspend fun getPlaceByLabel(label: String) = placeDao.getPlaceByLabel(label)
    suspend fun getPlaceById(id: Long) = placeDao.getPlaceById(id)
}
