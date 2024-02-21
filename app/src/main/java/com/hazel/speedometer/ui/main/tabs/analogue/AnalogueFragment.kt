package com.hazel.speedometer.ui.main.tabs.analogue

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.hazel.speedometer.R
import com.hazel.speedometer.databinding.FragmentAnalogueBinding
import com.hazel.speedometer.sealed.TripState
import com.hazel.speedometer.ui.main.MainViewModel
import com.hazel.speedometer.utils.checkLocationIsEnabled
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.karumi.dexter.listener.multi.SnackbarOnAnyDeniedMultiplePermissionsListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AnalogueFragment : Fragment() {

    companion object {
        @JvmStatic
        fun newInstance() = AnalogueFragment()
    }

    private lateinit var binding: FragmentAnalogueBinding
    private val viewModel by lazy {
        ViewModelProvider(requireActivity() as ViewModelStoreOwner)[MainViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAnalogueBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initCollectors()
        initClickListeners()
    }

    private fun initClickListeners() {
        binding.stats.btnStart.setOnClickListener {
            startTrip()
        }
        binding.stats.btnPause.setOnClickListener {
            if (viewModel.tripState.value == TripState.Started)
                viewModel.pauseLocationUpdates()
            else
                viewModel.resumeLocationUpdates()
        }
        binding.stats.btnStop.setOnClickListener {
            viewModel.stopLocationUpdates()
        }
    }

    private fun startTrip() {
        if (requireContext().checkLocationIsEnabled()) {
            val permissions = mutableListOf<String>()
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
            Dexter.withContext(requireContext())
                .withPermissions(permissions)
                .withListener(object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        if (report?.areAllPermissionsGranted() == true) {
                            viewModel.startLocationUpdates()
                        } else {
                            val snackBar = SnackbarOnAnyDeniedMultiplePermissionsListener.Builder
                                .with(binding.root, "Location permission is required")
                                .withOpenSettingsButton("Settings")
                                .build()
                            snackBar.onPermissionsChecked(report)
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        permissions: MutableList<PermissionRequest>?,
                        token: PermissionToken?,
                    ) {
                        token?.continuePermissionRequest()
                    }
                })
                .check()
        } else {
            val snackBar = Snackbar.make(
                binding.root,
                "Please enable location services",
                Snackbar.LENGTH_LONG
            )
            snackBar.setAction("Settings") {
                requireContext().startActivity(
                    android.content.Intent(
                        android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS
                    )
                )
            }
            snackBar.show()
        }
    }

    private fun initCollectors() {
        lifecycleScope.launch {
            viewModel.currentSpeed.collectLatest {
                binding.speedMeter.speedTo(it.toFloat())
            }
        }

        lifecycleScope.launch {
            viewModel.distance.collectLatest {
                binding.stats.tvDistanceValue.text = String.format("%.2f", it)
            }
        }

        lifecycleScope.launch {
            viewModel.maxSpeed.collectLatest {
                binding.stats.tvMaxSpeedValue.text = String.format("%.1f", it)
            }
        }

        lifecycleScope.launch {
            viewModel.avgSpeed.collectLatest {
                binding.stats.tvAvgSpeedValue.text = String.format("%.1f", it)
            }
        }

        lifecycleScope.launch {
            viewModel.tripState.collectLatest {
                when (it) {
                    TripState.Idle -> {
                        // Do nothing
                    }
                    TripState.Paused -> {
                        binding.stats.btnStart.visibility = View.GONE
                        binding.stats.btnPause.visibility = View.VISIBLE
                        binding.stats.btnStop.visibility = View.VISIBLE
                        binding.stats.btnPause.text = getString(R.string.resume)
                    }
                    TripState.Started -> {
                        binding.stats.btnStart.visibility = View.GONE
                        binding.stats.btnPause.visibility = View.VISIBLE
                        binding.stats.btnStop.visibility = View.VISIBLE
                        binding.stats.btnPause.text = getString(R.string.pause)
                    }
                    TripState.Stopped -> {
                        binding.stats.btnStart.visibility = View.VISIBLE
                        binding.stats.btnPause.visibility = View.GONE
                        binding.stats.btnStop.visibility = View.GONE
                    }
                }
            }
        }
    }
}