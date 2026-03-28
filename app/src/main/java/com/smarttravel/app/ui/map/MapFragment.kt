package com.smarttravel.app.ui.map

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.smarttravel.app.databinding.FragmentMapBinding
import com.smarttravel.app.repository.Result
import com.smarttravel.app.utils.DistanceFormatter
import com.smarttravel.app.viewmodel.MapViewModel
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MapViewModel by viewModels()

    private var srcLat: Double? = null; private var srcLon: Double? = null
    private var dstLat: Double? = null; private var dstLon: Double? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        observeViewModel()
    }

    private fun setupUI() {
        binding.btnCalculateRoute.setOnClickListener {
            val sLat = srcLat ?: viewModel.currentLocation.value?.latitude ?: run {
                Toast.makeText(requireContext(), "Waiting for GPS...", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val sLon = srcLon ?: viewModel.currentLocation.value?.longitude ?: return@setOnClickListener
            val dLat = dstLat ?: run {
                Toast.makeText(requireContext(), "Enter destination", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val dLon = dstLon ?: return@setOnClickListener
            viewModel.calculateRoute(sLat, sLon, dLat, dLon)
        }

        binding.fabMyLocation.setOnClickListener {
            val loc = viewModel.currentLocation.value
            if (loc == null) {
                Toast.makeText(requireContext(), "GPS not available", Toast.LENGTH_SHORT).show()
            } else {
                binding.tvLocationInfo.text = "📍 ${String.format("%.6f", loc.latitude)}, ${String.format("%.6f", loc.longitude)}"
            }
        }

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

        binding.btnToggleTracking.setOnClickListener {
            val loc = viewModel.currentLocation.value ?: run {
                Toast.makeText(requireContext(), "Waiting for GPS fix...", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.startTracking(loc)
            Toast.makeText(requireContext(), "Tracking started", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        viewModel.currentLocation.observe(viewLifecycleOwner) { loc ->
            loc ?: return@observe
            binding.tvLocationInfo.text = "📍 ${String.format("%.6f", loc.latitude)}, ${String.format("%.6f", loc.longitude)}  ±${loc.accuracy.toInt()}m"
            Timber.d("Location updated: ${loc.latitude}, ${loc.longitude}")
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

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
