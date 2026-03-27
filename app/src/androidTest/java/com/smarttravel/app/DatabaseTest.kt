package com.smarttravel.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smarttravel.app.data.local.SmartTravelDatabase
import com.smarttravel.app.data.local.entity.SavedPlace
import com.smarttravel.app.data.local.entity.TravelSession
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseTest {

    private lateinit var db: SmartTravelDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, SmartTravelDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() = db.close()

    // ── TravelSession ────────────────────────────────────────────────────
    @Test
    fun insertAndReadSession() = runTest {
        val session = TravelSession(
            startTime = System.currentTimeMillis(),
            startLat = 19.076,
            startLng = 72.877,
            date = "2024-01-15"
        )
        val id = db.travelSessionDao().insert(session)
        assertTrue(id > 0)

        val retrieved = db.travelSessionDao().getSessionById(id)
        assertNotNull(retrieved)
        assertEquals(19.076, retrieved!!.startLat, 0.001)
        assertEquals("2024-01-15", retrieved.date)
    }

    @Test
    fun updateSession() = runTest {
        val session = TravelSession(
            startTime = System.currentTimeMillis(),
            startLat = 12.971,
            startLng = 77.594,
            date = "2024-01-16"
        )
        val id = db.travelSessionDao().insert(session)
        val updated = session.copy(id = id, totalDistanceKm = 25.5, totalDurationMin = 45)
        db.travelSessionDao().update(updated)

        val retrieved = db.travelSessionDao().getSessionById(id)
        assertEquals(25.5, retrieved!!.totalDistanceKm, 0.001)
        assertEquals(45L, retrieved.totalDurationMin)
    }

    @Test
    fun activeSessionIsNullWhenNoneActive() = runTest {
        val active = db.travelSessionDao().getActiveSession()
        assertNull(active)
    }

    @Test
    fun activeSessionReturnedWhenEndTimeNull() = runTest {
        val session = TravelSession(
            startTime = System.currentTimeMillis(),
            startLat = 0.0, startLng = 0.0,
            date = "2024-01-17"
        )
        val id = db.travelSessionDao().insert(session)
        val active = db.travelSessionDao().getActiveSession()
        assertNotNull(active)
        assertEquals(id, active!!.id)
    }

    // ── SavedPlace ───────────────────────────────────────────────────────
    @Test
    fun insertAndReadSavedPlace() = runTest {
        val place = SavedPlace(
            name = "Home",
            label = "HOME",
            address = "123 Main St, Mumbai",
            latitude = 19.076,
            longitude = 72.877
        )
        val id = db.savedPlaceDao().insert(place)
        assertTrue(id > 0)

        val allPlaces = db.savedPlaceDao().getAllPlaces().first()
        assertEquals(1, allPlaces.size)
        assertEquals("Home", allPlaces[0].name)
        assertEquals("HOME", allPlaces[0].label)
    }

    @Test
    fun getPlaceByLabel() = runTest {
        db.savedPlaceDao().insert(SavedPlace(name = "Home", label = "HOME", address = "", latitude = 19.076, longitude = 72.877))
        db.savedPlaceDao().insert(SavedPlace(name = "Office", label = "OFFICE", address = "", latitude = 19.1, longitude = 72.9))

        val home = db.savedPlaceDao().getPlaceByLabel("HOME")
        assertNotNull(home)
        assertEquals("Home", home!!.name)

        val office = db.savedPlaceDao().getPlaceByLabel("OFFICE")
        assertNotNull(office)
        assertEquals("Office", office!!.name)
    }

    @Test
    fun deleteSavedPlace() = runTest {
        val place = SavedPlace(name = "Gym", label = "CUSTOM", address = "", latitude = 0.0, longitude = 0.0)
        val id = db.savedPlaceDao().insert(place)
        val inserted = db.savedPlaceDao().getPlaceById(id)!!

        db.savedPlaceDao().delete(inserted)
        val allPlaces = db.savedPlaceDao().getAllPlaces().first()
        assertTrue(allPlaces.isEmpty())
    }

    @Test
    fun insertMultipleSessionsAndQueryByDate() = runTest {
        val dao = db.travelSessionDao()
        val date1 = "2024-01-20"
        val date2 = "2024-01-21"

        dao.insert(TravelSession(startTime = 1000L, startLat = 0.0, startLng = 0.0, date = date1))
        dao.insert(TravelSession(startTime = 2000L, startLat = 1.0, startLng = 1.0, date = date1))
        dao.insert(TravelSession(startTime = 3000L, startLat = 2.0, startLng = 2.0, date = date2))

        val sessionsDay1 = dao.getSessionsByDate(date1).first()
        assertEquals(2, sessionsDay1.size)

        val sessionsDay2 = dao.getSessionsByDate(date2).first()
        assertEquals(1, sessionsDay2.size)
    }
}
