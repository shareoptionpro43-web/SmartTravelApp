package com.smarttravel.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.smarttravel.app.data.local.dao.*
import com.smarttravel.app.data.local.entity.*

@Database(
    entities = [
        TravelSession::class,
        LocationPoint::class,
        SavedPlace::class,
        DailySummary::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SmartTravelDatabase : RoomDatabase() {
    abstract fun travelSessionDao(): TravelSessionDao
    abstract fun locationPointDao(): LocationPointDao
    abstract fun savedPlaceDao(): SavedPlaceDao
    abstract fun dailySummaryDao(): DailySummaryDao
}
