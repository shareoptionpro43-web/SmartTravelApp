package com.smarttravel.app.ui.tags

import android.os.Bundle
import android.view.*
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.smarttravel.app.R
import com.smarttravel.app.data.local.entity.SavedPlace
import com.smarttravel.app.databinding.FragmentTagsBinding
import com.smarttravel.app.viewmodel.MapViewModel
import com.smarttravel.app.viewmodel.SavedPlacesViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TagsFragment : Fragment() {

    private var _binding: FragmentTagsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SavedPlacesViewModel by viewModels()
    private val mapViewModel: MapViewModel by viewModels()
    private lateinit var adapter: SavedPlacesAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTagsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()

        binding.fabAddPlace.setOnClickListener { showAddPlaceDialog() }
    }

    private fun setupRecyclerView() {
        adapter = SavedPlacesAdapter(
            onDelete = { place -> viewModel.deletePlace(place) }
        )
        binding.rvSavedPlaces.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSavedPlaces.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.places.observe(viewLifecycleOwner) { places ->
            adapter.submitList(places)
            binding.tvEmpty.visibility = if (places.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun showAddPlaceDialog() {
        val loc = mapViewModel.currentLocation.value
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_place, null)
        val etName = dialogView.findViewById<android.widget.EditText>(R.id.et_place_name)
        val spinnerLabel = dialogView.findViewById<android.widget.Spinner>(R.id.spinner_label)

        val labels = arrayOf("HOME", "OFFICE", "CUSTOM")
        spinnerLabel.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, labels)

        AlertDialog.Builder(requireContext())
            .setTitle("Save Current Location")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val name = etName.text.toString().trim()
                val label = spinnerLabel.selectedItem.toString()
                if (name.isEmpty()) {
                    Toast.makeText(requireContext(), "Enter a name", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (loc == null) {
                    Toast.makeText(requireContext(), "GPS not available", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                mapViewModel.savePlace(name, label, "Current location", loc.latitude, loc.longitude)
                Toast.makeText(requireContext(), "Place saved!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() { _binding = null; super.onDestroyView() }
}

// ── Saved Places Adapter ───────────────────────────────────────────────────
class SavedPlacesAdapter(
    private val onDelete: (SavedPlace) -> Unit
) : androidx.recyclerview.widget.ListAdapter<SavedPlace,
        SavedPlacesAdapter.ViewHolder>(PlaceDiffCallback()) {

    inner class ViewHolder(val binding: com.smarttravel.app.databinding.ItemSavedPlaceBinding)
        : androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {
        fun bind(place: SavedPlace) {
            binding.tvName.text = place.name
            binding.tvLabel.text = place.label
            binding.tvCoords.text = "${"%.4f".format(place.latitude)}, ${"%.4f".format(place.longitude)}"
            binding.tvRadius.text = "${place.radiusMeters.toInt()}m radius"
            binding.btnDelete.setOnClickListener { onDelete(place) }
            val icon = when (place.label) {
                "HOME" -> "🏠"
                "OFFICE" -> "🏢"
                else -> "📌"
            }
            binding.tvIcon.text = icon
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        com.smarttravel.app.databinding.ItemSavedPlaceBinding.inflate(
            LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))
}

class PlaceDiffCallback : androidx.recyclerview.widget.DiffUtil.ItemCallback<SavedPlace>() {
    override fun areItemsTheSame(a: SavedPlace, b: SavedPlace) = a.id == b.id
    override fun areContentsTheSame(a: SavedPlace, b: SavedPlace) = a == b
}
