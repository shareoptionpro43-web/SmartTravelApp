package com.smarttravel.app.ui.analytics

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.smarttravel.app.data.local.entity.DailySummary
import com.smarttravel.app.databinding.FragmentAnalyticsBinding
import com.smarttravel.app.repository.Result
import com.smarttravel.app.viewmodel.AnalyticsViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class AnalyticsFragment : Fragment() {

    private var _binding: FragmentAnalyticsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AnalyticsViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAnalyticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupChart()
        observeViewModel()

        binding.btnExportCsv.setOnClickListener {
            viewModel.exportToCSV()
        }
    }

    private fun setupChart() {
        binding.barChart.apply {
            description.isEnabled = false
            setDrawGridBackground(false)
            legend.isEnabled = true
            animateY(800)
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
            }
            axisLeft.apply {
                axisMinimum = 0f
                setDrawGridLines(true)
            }
            axisRight.isEnabled = false
        }
    }

    private fun observeViewModel() {
        viewModel.recentSummaries.observe(viewLifecycleOwner) { summaries ->
            if (summaries.isNullOrEmpty()) {
                binding.tvNoData.visibility = View.VISIBLE
                binding.barChart.visibility = View.GONE
                return@observe
            }

            binding.tvNoData.visibility = View.GONE
            binding.barChart.visibility = View.VISIBLE

            // Update summary cards
            val totalDist = summaries.sumOf { it.totalDistanceKm }
            val totalTime = summaries.sumOf { it.totalTravelTimeMin }
            val totalSessions = summaries.sumOf { it.sessionCount }

            binding.tvTotalDistance.text = "${"%.1f".format(totalDist)} km"
            binding.tvTotalTime.text = "${totalTime / 60}h ${totalTime % 60}m"
            binding.tvTotalTrips.text = "$totalSessions trips"

            updateChart(summaries)
        }

        viewModel.exportResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Loading -> Toast.makeText(requireContext(), "Exporting...", Toast.LENGTH_SHORT).show()
                is Result.Success -> shareFile(result.data)
                is Result.Error -> Toast.makeText(requireContext(), "Export failed: ${result.message}", Toast.LENGTH_LONG).show()
                else -> {}
            }
        }
    }

    private fun updateChart(summaries: List<DailySummary>) {
        val sorted = summaries.takeLast(7).reversed()
        val entries = sorted.mapIndexed { i, s ->
            BarEntry(i.toFloat(), s.totalDistanceKm.toFloat())
        }
        val labels = sorted.map { it.date.substring(5) } // MM-dd

        val dataSet = BarDataSet(entries, "Distance (km)").apply {
            color = Color.parseColor("#1565C0")
            valueTextColor = Color.DKGRAY
            valueTextSize = 10f
        }

        binding.barChart.apply {
            data = BarData(dataSet).apply { barWidth = 0.6f }
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            invalidate()
        }
    }

    private fun shareFile(path: String) {
        val file = File(path)
        val uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Share Travel History"))
    }

    override fun onDestroyView() { _binding = null; super.onDestroyView() }
}
