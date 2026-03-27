package com.smarttravel.app.ui.search

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.smarttravel.app.data.remote.model.NominatimPlace
import com.smarttravel.app.databinding.FragmentSearchBinding
import com.smarttravel.app.repository.Result
import com.smarttravel.app.viewmodel.SearchViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SearchViewModel by viewModels()
    private lateinit var adapter: SearchResultsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSearch()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = SearchResultsAdapter { place ->
            viewModel.selectPlace(place)
            Toast.makeText(requireContext(), "Selected: ${place.displayName.take(40)}", Toast.LENGTH_SHORT).show()
        }
        binding.rvResults.layoutManager = LinearLayoutManager(requireContext())
        binding.rvResults.adapter = adapter
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener { text ->
            val query = text.toString().trim()
            if (query.length >= 3) viewModel.search(query)
            else adapter.submitList(emptyList())
        }

        binding.btnClear.setOnClickListener {
            binding.etSearch.text?.clear()
            adapter.submitList(emptyList())
        }
    }

    private fun observeViewModel() {
        viewModel.searchResults.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Loading -> binding.progressBar.visibility = View.VISIBLE
                is Result.Success -> {
                    binding.progressBar.visibility = View.GONE
                    adapter.submitList(result.data)
                    binding.tvResultCount.text = "${result.data.size} results found"
                }
                is Result.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        }
    }

    override fun onDestroyView() { _binding = null; super.onDestroyView() }
}

// ── Search Results Adapter ─────────────────────────────────────────────────
class SearchResultsAdapter(
    private val onItemClick: (NominatimPlace) -> Unit
) : androidx.recyclerview.widget.ListAdapter<NominatimPlace,
        SearchResultsAdapter.ViewHolder>(SearchDiffCallback()) {

    inner class ViewHolder(val binding: com.smarttravel.app.databinding.ItemSearchResultBinding)
        : androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {
        fun bind(place: NominatimPlace) {
            binding.tvPlaceName.text = place.displayName.split(",").firstOrNull() ?: place.displayName
            binding.tvPlaceAddress.text = place.displayName
            binding.tvPlaceType.text = place.type.replaceFirstChar { it.uppercase() }
            binding.root.setOnClickListener { onItemClick(place) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        com.smarttravel.app.databinding.ItemSearchResultBinding.inflate(
            LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))
}

class SearchDiffCallback : androidx.recyclerview.widget.DiffUtil.ItemCallback<NominatimPlace>() {
    override fun areItemsTheSame(a: NominatimPlace, b: NominatimPlace) = a.placeId == b.placeId
    override fun areContentsTheSame(a: NominatimPlace, b: NominatimPlace) = a == b
}
