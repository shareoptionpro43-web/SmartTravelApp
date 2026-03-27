package com.smarttravel.app.ui.map

import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.smarttravel.app.R
import com.smarttravel.app.databinding.FragmentMapBinding
import com.smarttravel.app.repository.Result
import com.smarttravel.app.utils.DistanceFormatter
import com.smarttravel.app.utils.FuelCalculator
import com.smarttravel.app.viewmodel.MapViewModel
import dagger.hilt.android.AndroidEntryPoint
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.plugins.annotation.LineManager
import org.maplibre.android.plugins.annotation.LineOptions
import org.maplibre.android.plugins.annotation.SymbolManager
import org.maplibre.android.plugins.annotation.SymbolOptions
import timber.log.Timber

@AndroidEntryPoint
class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MapViewModel by viewModels()

    private var mapLibreMap: MapLibreMap? = null
    private var lineManager: LineManager? = null
    private var symbolManager: SymbolManager? = null

    private var srcLat: Double? = null; private var srcLon: Double? = null
    private var dstLat: Double? = null; private var dstLon: Double? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        MapLibre.getInstance(requireContext())
        setupMap(savedInstanceState)
        setupUI()
        observeViewModel()
    }

    private fun setupMap(savedInstanceState: Bundle?) {
        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync { map ->
            mapLibreMap = map
            map.setStyle(
                Style.Builder().fromUri("https://demotiles.maplibre.org/style.json")
            ) { style ->
                lineManager = LineManager(binding.mapView, map, style)
                symbolManager = SymbolManager(binding.mapView, map, style).apply {
                    iconAllowOverlap = true
                    textAllowOverlap = true
                }
                Timber.d("Map loaded with style")
            }
            map.uiSettings.isRotateGesturesEnabled = true
            map.uiSettings.isZoomGesturesEnabled = true
        }
    }

    private fun setupUI() {
        // Route calculation button
        binding.btnCalculateRoute.setOnClickListener {
            val src = binding.etSource.text.toString().trim()
            val dst = binding.etDestination.text.toString().trim()
            if (src.isEmpty() || dst.isEmpty()) {
                Toast.makeText(requireContext(), "Enter source and destination", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Parse coords from stored selection or current location
            val sLat = srcLat ?: viewModel.currentLocation.value?.latitude ?: return@setOnClickListener
            val sLon = srcLon ?: viewModel.currentLocation.value?.longitude ?: return@setOnClickListener
            val dLat = dstLat ?: return@setOnClickListener
            val dLon = dstLon ?: return@setOnClickListener
            viewModel.calculateRoute(sLat, sLon, dLat, dLon)
        }

        // My location button
        binding.fabMyLocation.setOnClickListener {
            viewModel.currentLocation.value?.let { loc ->
                mapLibreMap?.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(LatLng(loc.latitude, loc.longitude), 15.0)
                )
            }
        }

        // Fuel cost calculation
        binding.btnCalculateFuel.setOnClickListener {
            val price = binding.etFuelPrice.text.toString().toDoubleOrNull() ?: return@setOnClickListener
            val mileage = binding.etMileage.text.toString().toDoubleOrNull() ?: return@setOnClickListener
            val routeRes = viewModel.routeResult.value
            if (routeRes is Result.Success) {
                viewModel.calculateFuelCost(routeRes.data.distanceKm, price, mileage)
            } else {
                Toast.makeText(requireContext(), "Calculate a route first", Toast.LENGTH_SHORT).show()
            }
        }

        // Tracking toggle
        binding.btnToggleTracking.setOnClickListener {
            val loc = viewModel.currentLocation.value
            if (loc == null) {
                Toast.makeText(requireContext(), "Waiting for GPS fix...", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Toggle logic — simplified; in production use a state flag
            viewModel.startTracking(loc)
            Toast.makeText(requireContext(), "Tracking started", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        viewModel.currentLocation.observe(viewLifecycleOwner) { loc ->
            loc ?: return@observe
            val latLng = LatLng(loc.latitude, loc.longitude)
            binding.tvLocationInfo.text = "📍 ${String.format("%.6f", loc.latitude)}, ${String.format("%.6f", loc.longitude)}  ±${loc.accuracy.toInt()}m"

            // Move camera to user on first fix
            mapLibreMap?.let { map ->
                if (map.cameraPosition.zoom < 5) {
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15.0))
                }
                updateUserMarker(latLng)
            }
        }

        viewModel.routeResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Loading -> binding.progressBar.visibility = View.VISIBLE
                is Result.Success -> {
                    binding.progressBar.visibility = View.GONE
                    val r = result.data
                    binding.tvRouteInfo.text = buildString {
                        append("🛣️ ${DistanceFormatter.format(r.distanceKm)}")
                        append("   ⏱️ ${DistanceFormatter.formatDuration(r.durationMin)}")
                    }
                    binding.cardRouteInfo.visibility = View.VISIBLE
                    drawRoute(r.polylinePoints)
                }
                is Result.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show()
                }
                else -> {}
            }
        }

        viewModel.fuelCost.observe(viewLifecycleOwner) { cost ->
            binding.tvFuelCost.text = "⛽ Fuel Cost: ₹${"%.2f".format(cost)}"
            binding.tvFuelCost.visibility = View.VISIBLE
        }
    }

    private fun updateUserMarker(latLng: LatLng) {
        // In production: use a proper marker layer with custom icon
        Timber.d("User at: $latLng")
    }

    private fun drawRoute(points: List<Pair<Double, Double>>) {
        lineManager?.deleteAll()
        symbolManager?.deleteAll()

        if (points.isEmpty()) return

        val latLngs = points.map { LatLng(it.first, it.second) }

        // Draw polyline
        lineManager?.create(
            LineOptions()
                .withLatLngs(latLngs)
                .withLineColor("#1565C0")
                .withLineWidth(4f)
                .withLineOpacity(0.85f)
        )

        // Fit camera to route
        val bounds = LatLngBounds.Builder().includes(latLngs).build()
        mapLibreMap?.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 80))
    }

    // ── MapView Lifecycle ──────────────────────────────────────────────────
    override fun onStart() { super.onStart(); binding.mapView.onStart() }
    override fun onResume() { super.onResume(); binding.mapView.onResume() }
    override fun onPause() { super.onPause(); binding.mapView.onPause() }
    override fun onStop() { super.onStop(); binding.mapView.onStop() }
    override fun onLowMemory() { super.onLowMemory(); binding.mapView.onLowMemory() }
    override fun onSaveInstanceState(outState: Bundle) { super.onSaveInstanceState(outState); binding.mapView.onSaveInstanceState(outState) }
    override fun onDestroyView() {
        lineManager?.onDestroy()
        symbolManager?.onDestroy()
        binding.mapView.onDestroy()
        _binding = null
        super.onDestroyView()
    }
}
