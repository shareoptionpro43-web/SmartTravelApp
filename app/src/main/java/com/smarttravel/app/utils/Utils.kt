package com.smarttravel.app.utils

import android.content.Context
import android.os.Environment
import com.smarttravel.app.data.local.entity.TravelSession
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

// ── Polyline Decoder (OSRM encoded geometry) ───────────────────────────────
object PolylineDecoder {
    fun decode(encoded: String): List<Pair<Double, Double>> {
        val points = mutableListOf<Pair<Double, Double>>()
        var index = 0
        var lat = 0
        var lng = 0

        while (index < encoded.length) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if ((result and 1) != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0; result = 0
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if ((result and 1) != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            points.add(Pair(lat / 1e5, lng / 1e5))
        }
        return points
    }
}

// ── CSV Export Helper ──────────────────────────────────────────────────────
@Singleton
class CsvExportHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val fileNameFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())

    fun export(sessions: List<TravelSession>): String {
        val fileName = "travel_history_${fileNameFormat.format(Date())}.csv"
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: context.filesDir
        val file = File(dir, fileName)

        FileWriter(file).use { writer ->
            // Header
            writer.append("ID,Date,Start Time,End Time,Start Lat,Start Lng,End Lat,End Lng,Distance (km),Duration (min),Stops,Fuel Cost (₹)\n")
            // Rows
            sessions.forEach { s ->
                writer.append("${s.id},")
                writer.append("${s.date},")
                writer.append("${dateFormat.format(Date(s.startTime))},")
                writer.append("${s.endTime?.let { dateFormat.format(Date(it)) } ?: ""},")
                writer.append("${s.startLat},${s.startLng},")
                writer.append("${s.endLat ?: ""},${s.endLng ?: ""},")
                writer.append("${"%.2f".format(s.totalDistanceKm)},")
                writer.append("${s.totalDurationMin},")
                writer.append("${s.stopCount},")
                writer.append("${"%.2f".format(s.fuelCost)}\n")
            }
        }

        Timber.d("CSV exported to: ${file.absolutePath}")
        return file.absolutePath
    }
}

// ── Location Permission Helper ─────────────────────────────────────────────
object LocationPermissionHelper {
    val REQUIRED_PERMISSIONS = arrayOf(
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION
    )

    fun hasPermissions(context: Context): Boolean =
        REQUIRED_PERMISSIONS.all {
            context.checkSelfPermission(it) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
}

// ── Distance Formatter ─────────────────────────────────────────────────────
object DistanceFormatter {
    fun format(km: Double): String = when {
        km < 1.0 -> "${(km * 1000).toInt()} m"
        else -> "${"%.1f".format(km)} km"
    }

    fun formatDuration(minutes: Double): String {
        val hrs = minutes.toLong() / 60
        val mins = minutes.toLong() % 60
        return if (hrs > 0) "${hrs}h ${mins}m" else "${mins} min"
    }
}

// ── Fuel Calculator ────────────────────────────────────────────────────────
object FuelCalculator {
    data class FuelResult(
        val litersNeeded: Double,
        val totalCost: Double
    )

    fun calculate(distanceKm: Double, mileageKmPerLiter: Double, pricePerLiter: Double): FuelResult {
        val liters = if (mileageKmPerLiter > 0) distanceKm / mileageKmPerLiter else 0.0
        return FuelResult(liters, liters * pricePerLiter)
    }
}
