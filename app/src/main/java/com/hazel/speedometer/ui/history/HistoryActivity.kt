package com.hazel.speedometer.ui.history

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.hazel.speedometer.databinding.ActivityHistoryBinding
import com.hazel.speedometer.ui.history.adapter.HistoryAdapter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class HistoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHistoryBinding
    private lateinit var adapter: HistoryAdapter
    private val viewModel by viewModels<HistoryViewModel>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initAdapter()
        initCollectors()
        initClickListeners()
    }

    private fun initClickListeners() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (adapter.selectionMode) {
                    adapter.clearSelection()
                    binding.btnDelete.visibility = View.GONE
                } else {
                    finish()
                }
            }
        })

        binding.btnDelete.setOnClickListener {
            viewModel.deleteTrips(adapter.selectedItems)
        }
    }

    private fun initCollectors() {
        lifecycleScope.launch{
            viewModel.trips.collectLatest {
                adapter.submitList(it)
            }
        }

        lifecycleScope.launch {
            viewModel.deleteTrip.collectLatest {
                if (it) {
                    adapter.clearSelection()
                    binding.btnDelete.visibility = View.GONE
                }
            }
        }
    }

    private fun initAdapter() {
        adapter = HistoryAdapter(
            onTripSelection = {
                if (adapter.selectionMode) {
                    binding.btnDelete.visibility = View.VISIBLE
                }
            }
        )
        binding.rvHistory.adapter = adapter
        binding.rvHistory.setHasFixedSize(true)
    }
}