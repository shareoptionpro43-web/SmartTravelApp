package com.smarttravel.app

import com.smarttravel.app.repository.LocationRepository
import com.smarttravel.app.utils.DistanceFormatter
import com.smarttravel.app.utils.FuelCalculator
import com.smarttravel.app.utils.PolylineDecoder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

// ── PolylineDecoder Tests ──────────────────────────────────────────────────
class PolylineDecoderTest {

    @Test
    fun `decode empty string returns empty list`() {
        val result = PolylineDecoder.decode("")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `decode valid polyline returns correct points`() {
        // Known encoded polyline for a simple 2-point route
        // Encoded: (38.5, -120.2) -> (40.7, -120.95) -> (43.252, -126.453)
        val encoded = "_p~iF~ps|U_ulLnnqC_mqNvxq`@"
        val result = PolylineDecoder.decode(encoded)
        assertTrue(result.isNotEmpty())
        assertEquals(3, result.size)
        assertEquals(38.5, result[0].first, 0.001)
        assertEquals(-120.2, result[0].second, 0.001)
    }

    @Test
    fun `decode single point polyline`() {
        // Encoded single point at (0, 0)
        val encoded = "??"
        val result = PolylineDecoder.decode(encoded)
        assertFalse(result.isEmpty())
    }
}

// ── DistanceFormatter Tests ────────────────────────────────────────────────
class DistanceFormatterTest {

    @Test
    fun `format meters when distance below 1km`() {
        val result = DistanceFormatter.format(0.5)
        assertEquals("500 m", result)
    }

    @Test
    fun `format km when distance above 1km`() {
        val result = DistanceFormatter.format(12.345)
        assertEquals("12.3 km", result)
    }

    @Test
    fun `format exactly 1km`() {
        val result = DistanceFormatter.format(1.0)
        assertEquals("1.0 km", result)
    }

    @Test
    fun `format zero distance`() {
        val result = DistanceFormatter.format(0.0)
        assertEquals("0 m", result)
    }

    @Test
    fun `format duration under 1 hour`() {
        val result = DistanceFormatter.formatDuration(45.0)
        assertEquals("45 min", result)
    }

    @Test
    fun `format duration over 1 hour`() {
        val result = DistanceFormatter.formatDuration(90.0)
        assertEquals("1h 30m", result)
    }

    @Test
    fun `format duration exactly 1 hour`() {
        val result = DistanceFormatter.formatDuration(60.0)
        assertEquals("1h 0m", result)
    }

    @Test
    fun `format duration zero`() {
        val result = DistanceFormatter.formatDuration(0.0)
        assertEquals("0 min", result)
    }
}

// ── FuelCalculator Tests ───────────────────────────────────────────────────
class FuelCalculatorTest {

    @Test
    fun `calculate standard trip`() {
        // 100km, 20km/l mileage, ₹100/liter → 5L → ₹500
        val result = FuelCalculator.calculate(100.0, 20.0, 100.0)
        assertEquals(5.0, result.litersNeeded, 0.001)
        assertEquals(500.0, result.totalCost, 0.001)
    }

    @Test
    fun `calculate zero distance returns zero cost`() {
        val result = FuelCalculator.calculate(0.0, 15.0, 90.0)
        assertEquals(0.0, result.litersNeeded, 0.001)
        assertEquals(0.0, result.totalCost, 0.001)
    }

    @Test
    fun `calculate zero mileage returns zero`() {
        val result = FuelCalculator.calculate(100.0, 0.0, 100.0)
        assertEquals(0.0, result.litersNeeded, 0.001)
        assertEquals(0.0, result.totalCost, 0.001)
    }

    @Test
    fun `calculate short distance accurately`() {
        // 10km, 25km/l, ₹95 → 0.4L → ₹38
        val result = FuelCalculator.calculate(10.0, 25.0, 95.0)
        assertEquals(0.4, result.litersNeeded, 0.001)
        assertEquals(38.0, result.totalCost, 0.001)
    }

    @Test
    fun `calculate long highway trip`() {
        // Delhi to Mumbai: ~1450km, 18km/l, ₹102
        val result = FuelCalculator.calculate(1450.0, 18.0, 102.0)
        assertEquals(80.556, result.litersNeeded, 0.01)
        assertEquals(8216.67, result.totalCost, 1.0)
    }
}

// ── Haversine Distance Tests ───────────────────────────────────────────────
class HaversineTest {

    // We test the math directly since LocationRepository needs DB
    private fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2).pow(2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2).pow(2)
        return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    }

    private fun Double.pow(n: Int): Double = Math.pow(this, n.toDouble())

    @Test
    fun `distance from same point is zero`() {
        val d = haversineDistance(28.6139, 77.2090, 28.6139, 77.2090)
        assertEquals(0.0, d, 0.001)
    }

    @Test
    fun `distance Delhi to Mumbai is approximately 1150km`() {
        // Delhi: 28.6139, 77.2090 | Mumbai: 19.0760, 72.8777
        val d = haversineDistance(28.6139, 77.2090, 19.0760, 72.8777)
        assertTrue("Expected ~1150km but got $d", d in 1100.0..1200.0)
    }

    @Test
    fun `distance Bangalore to Chennai is approximately 290km`() {
        // Bangalore: 12.9716, 77.5946 | Chennai: 13.0827, 80.2707
        val d = haversineDistance(12.9716, 77.5946, 13.0827, 80.2707)
        assertTrue("Expected ~290km but got $d", d in 270.0..320.0)
    }

    @Test
    fun `distance is symmetric`() {
        val d1 = haversineDistance(28.6139, 77.2090, 19.0760, 72.8777)
        val d2 = haversineDistance(19.0760, 72.8777, 28.6139, 77.2090)
        assertEquals(d1, d2, 0.001)
    }
}
