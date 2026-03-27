package com.smarttravel.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

// ── Travel Session ─────────────────────────────────────────────────────────
@Entity(tableName = "travel_sessions")
data class TravelSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "start_time") val startTime: Long,
    @ColumnInfo(name = "end_time") val endTime: Long? = null,
    @ColumnInfo(name = "start_lat") val startLat: Double,
    @ColumnInfo(name = "start_lng") val startLng: Double,
    @ColumnInfo(name = "end_lat") val endLat: Double? = null,
    @ColumnInfo(name = "end_lng") val endLng: Double? = null,
    @ColumnInfo(name = "total_distance_km") val totalDistanceKm: Double = 0.0,
    @ColumnInfo(name = "total_duration_min") val totalDurationMin: Long = 0,
    @ColumnInfo(name = "stop_count") val stopCount: Int = 0,
    @ColumnInfo(name = "fuel_cost") val fuelCost: Double = 0.0,
    @ColumnInfo(name = "date") val date: String  // yyyy-MM-dd
)

// ── Location Point ─────────────────────────────────────────────────────────
@Entity(tableName = "location_points")
data class LocationPoint(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "session_id") val sessionId: Long,
    @ColumnInfo(name = "latitude") val latitude: Double,
    @ColumnInfo(name = "longitude") val longitude: Double,
    @ColumnInfo(name = "accuracy") val accuracy: Float,
    @ColumnInfo(name = "speed") val speed: Float,
    @ColumnInfo(name = "timestamp") val timestamp: Long
)

// ── Saved Place ────────────────────────────────────────────────────────────
@Entity(tableName = "saved_places")
data class SavedPlace(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "label") val label: String,  // HOME / OFFICE / CUSTOM
    @ColumnInfo(name = "address") val address: String,
    @ColumnInfo(name = "latitude") val latitude: Double,
    @ColumnInfo(name = "longitude") val longitude: Double,
    @ColumnInfo(name = "radius_meters") val radiusMeters: Float = 100f,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

// ── Daily Summary ──────────────────────────────────────────────────────────
@Entity(tableName = "daily_summaries")
data class DailySummary(
    @PrimaryKey val date: String,  // yyyy-MM-dd
    @ColumnInfo(name = "total_distance_km") val totalDistanceKm: Double = 0.0,
    @ColumnInfo(name = "total_travel_time_min") val totalTravelTimeMin: Long = 0,
    @ColumnInfo(name = "total_stops") val totalStops: Int = 0,
    @ColumnInfo(name = "session_count") val sessionCount: Int = 0,
    @ColumnInfo(name = "total_fuel_cost") val totalFuelCost: Double = 0.0
)
