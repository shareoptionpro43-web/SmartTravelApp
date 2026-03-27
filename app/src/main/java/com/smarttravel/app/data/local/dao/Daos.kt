package com.smarttravel.app.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.smarttravel.app.data.local.entity.*
import kotlinx.coroutines.flow.Flow

// ── Travel Session DAO ─────────────────────────────────────────────────────
@Dao
interface TravelSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: TravelSession): Long

    @Update
    suspend fun update(session: TravelSession)

    @Delete
    suspend fun delete(session: TravelSession)

    @Query("SELECT * FROM travel_sessions ORDER BY start_time DESC")
    fun getAllSessions(): Flow<List<TravelSession>>

    @Query("SELECT * FROM travel_sessions WHERE date = :date ORDER BY start_time DESC")
    fun getSessionsByDate(date: String): Flow<List<TravelSession>>

    @Query("SELECT * FROM travel_sessions WHERE id = :id")
    suspend fun getSessionById(id: Long): TravelSession?

    @Query("SELECT * FROM travel_sessions WHERE end_time IS NULL LIMIT 1")
    suspend fun getActiveSession(): TravelSession?

    @Query("SELECT SUM(total_distance_km) FROM travel_sessions WHERE date = :date")
    suspend fun getTotalDistanceForDate(date: String): Double?

    @Query("SELECT * FROM travel_sessions ORDER BY start_time DESC LIMIT :limit")
    suspend fun getRecentSessions(limit: Int): List<TravelSession>

    @Query("SELECT * FROM travel_sessions WHERE date BETWEEN :startDate AND :endDate")
    fun getSessionsInRange(startDate: String, endDate: String): Flow<List<TravelSession>>
}

// ── Location Point DAO ─────────────────────────────────────────────────────
@Dao
interface LocationPointDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(point: LocationPoint): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(points: List<LocationPoint>)

    @Query("SELECT * FROM location_points WHERE session_id = :sessionId ORDER BY timestamp ASC")
    suspend fun getPointsForSession(sessionId: Long): List<LocationPoint>

    @Query("SELECT * FROM location_points WHERE session_id = :sessionId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastPointForSession(sessionId: Long): LocationPoint?

    @Query("DELETE FROM location_points WHERE session_id = :sessionId")
    suspend fun deleteForSession(sessionId: Long)

    @Query("SELECT COUNT(*) FROM location_points WHERE session_id = :sessionId")
    suspend fun getCountForSession(sessionId: Long): Int
}

// ── Saved Place DAO ────────────────────────────────────────────────────────
@Dao
interface SavedPlaceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(place: SavedPlace): Long

    @Update
    suspend fun update(place: SavedPlace)

    @Delete
    suspend fun delete(place: SavedPlace)

    @Query("SELECT * FROM saved_places ORDER BY created_at DESC")
    fun getAllPlaces(): Flow<List<SavedPlace>>

    @Query("SELECT * FROM saved_places WHERE label = :label LIMIT 1")
    suspend fun getPlaceByLabel(label: String): SavedPlace?

    @Query("SELECT * FROM saved_places WHERE id = :id")
    suspend fun getPlaceById(id: Long): SavedPlace?
}

// ── Daily Summary DAO ──────────────────────────────────────────────────────
@Dao
interface DailySummaryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(summary: DailySummary)

    @Update
    suspend fun update(summary: DailySummary)

    @Query("SELECT * FROM daily_summaries WHERE date = :date")
    suspend fun getSummaryForDate(date: String): DailySummary?

    @Query("SELECT * FROM daily_summaries ORDER BY date DESC LIMIT :limit")
    fun getRecentSummaries(limit: Int): Flow<List<DailySummary>>

    @Query("SELECT * FROM daily_summaries ORDER BY date DESC")
    fun getAllSummaries(): Flow<List<DailySummary>>

    @Query("SELECT * FROM daily_summaries WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun getSummariesInRange(startDate: String, endDate: String): Flow<List<DailySummary>>
}
