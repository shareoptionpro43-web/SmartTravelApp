package com.smarttravel.app.ui.nearby

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.smarttravel.app.data.remote.model.NearbyPlace
import com.smarttravel.app.data.remote.model.PlaceCategory
import com.smarttravel.app.databinding.FragmentNearbyBinding
import com.smarttravel.app.repository.Result
import com.smarttravel.app.utils.DistanceFormatter
import com.smarttravel.app.viewmodel.MapViewModel
import com.smarttravel.app.viewmodel.NearbyViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NearbyFragment : Fragment() {

    private var _binding: FragmentNearbyBinding? = null
    private val binding get() = _binding!!
    private val viewModel: NearbyViewModel by viewModels()
    private val mapViewModel: MapViewModel by viewModels()
    private lateinit var adapter: NearbyPlacesAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNearbyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupChips()
        setupRecyclerView()
        observeViewModel()
        loadForCurrentLocation(PlaceCategory.HOTEL)
    }

    private fun setupChips() {
        PlaceCategory.values().forEach { category ->
            val chip = Chip(requireContext()).apply {
                text = category.displayName
                isCheckable = true
                isChecked = category == PlaceCategory.HOTEL
                setOnCheckedChangeListener { _, checked ->
                    if (checked) loadForCurrentLocation(category)
                }
            }
            binding.chipGroup.addView(chip)
        }
    }

    private fun setupRecyclerView() {
        adapter = NearbyPlacesAdapter()
        binding.rvNearby.layoutManager = LinearLayoutManager(requireContext())
        binding.rvNearby.adapter = adapter
    }

    private fun loadForCurrentLocation(category: PlaceCategory) {
        val loc = mapViewModel.currentLocation.value
        if (loc == null) {
            Toast.makeText(requireContext(), "Waiting for GPS location...", Toast.LENGTH_SHORT).show()
            return
        }
        viewModel.loadNearby(loc.latitude, loc.longitude, category)
    }

    private fun observeViewModel() {
        viewModel.places.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.rvNearby.visibility = View.GONE
                }
                is Result.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.rvNearby.visibility = View.VISIBLE
                    adapter.submitList(result.data)
                    binding.tvCount.text = "${result.data.size} places found nearby"
                }
                is Result.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show()
                }
                else -> {}
            }
        }
    }

    override fun onDestroyView() { _binding = null; super.onDestroyView() }
}

// ── Nearby Places Adapter ──────────────────────────────────────────────────
class NearbyPlacesAdapter : androidx.recyclerview.widget.ListAdapter<NearbyPlace,
        NearbyPlacesAdapter.ViewHolder>(NearbyDiffCallback()) {

    inner class ViewHolder(val binding: com.smarttravel.app.databinding.ItemNearbyPlaceBinding)
        : androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {
        fun bind(place: NearbyPlace) {
            binding.tvName.text = place.name
            binding.tvAddress.text = place.address.ifEmpty { "No address available" }
            binding.tvDistance.text = DistanceFormatter.format(place.distanceMeters / 1000)
            binding.tvCategory.text = place.category.displayName
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        com.smarttravel.app.databinding.ItemNearbyPlaceBinding.inflate(
            LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))
}

class NearbyDiffCallback : androidx.recyclerview.widget.DiffUtil.ItemCallback<NearbyPlace>() {
    override fun areItemsTheSame(a: NearbyPlace, b: NearbyPlace) = a.id == b.id
    override fun areContentsTheSame(a: NearbyPlace, b: NearbyPlace) = a == b
}
