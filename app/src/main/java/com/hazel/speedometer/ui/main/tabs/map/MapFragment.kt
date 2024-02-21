package com.hazel.speedometer.ui.main.tabs.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.hazel.speedometer.R
import com.hazel.speedometer.databinding.FragmentMapBinding
import com.hazel.speedometer.sealed.TripState
import com.hazel.speedometer.ui.main.MainViewModel
import com.hazel.speedometer.utils.checkLocationIsEnabled
import com.hazel.speedometer.utils.checkLocationPermission
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
class MapFragment : Fragment(), OnMapReadyCallback {
    companion object {
        @JvmStatic
        fun newInstance() = MapFragment()
    }

    private lateinit var binding: FragmentMapBinding
    private val viewModel by lazy {
        ViewModelProvider(requireActivity() as ViewModelStoreOwner)[MainViewModel::class.java]
    }

    private lateinit var map: GoogleMap
    private var userInteractWithMap = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initCollectors()
        initClickListeners()
        initMap()
    }

    @SuppressLint("ClickableViewAccessibility")
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

    private fun initCollectors() {
        lifecycleScope.launch {
            viewModel.location.collectLatest {
                if (it != null) {
                    if (this@MapFragment::map.isInitialized) {
                        if (!userInteractWithMap)
                            updateMapUI(it)
                    }
                    binding.tvLatitude.text = String.format("%.4f", it.latitude)
                    binding.tvLongitude.text = String.format("%.4f", it.longitude)
                }
            }
        }

        lifecycleScope.launch {
            viewModel.currentSpeed.collectLatest {
                binding.tvSpeed.text = String.format("%.1f", it) + " km/h"
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
                        if (this@MapFragment::map.isInitialized) {
                            map.clear()
                        }
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
                        endTripMarker()
                    }
                }
            }
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
                            mapSetup()
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
                requireContext().startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            snackBar.show()
        }
    }

    private fun updateMapUI(it: Location) {
        val startMarker = MarkerOptions()
            .position(
                LatLng(
                    viewModel.startLocation.latitude,
                    viewModel.startLocation.longitude
                )
            )
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
        map.addMarker(startMarker)
        map.addPolyline(viewModel.getPolylineOptions())
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 15f))
    }

    private fun endTripMarker() {
        val endMarker = MarkerOptions()
            .position(
                LatLng(
                    viewModel.endLocation.latitude,
                    viewModel.endLocation.longitude
                )
            )
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
        map.addMarker(endMarker)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initMap() {
        val mapFragment = childFragmentManager.findFragmentById(R.id.mapView) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        if (requireContext().checkLocationPermission()) {
            mapSetup()
        }
        map.setOnCameraMoveListener {
            userInteractWithMap = true
            binding.mapContainer.parent.requestDisallowInterceptTouchEvent(true)
        }
        map.setOnCameraIdleListener {
            userInteractWithMap = false
            binding.mapContainer.parent.requestDisallowInterceptTouchEvent(false)
        }
    }

    override fun onResume() {
        super.onResume()
        if (requireContext().checkLocationPermission()) {
            mapSetup()
        }
    }

    @SuppressLint("MissingPermission")
    private fun mapSetup() {
        map.isMyLocationEnabled = true
        map.uiSettings.isZoomControlsEnabled = true
        val location = viewModel.getCurrentLocation(requireContext())
        val latLng = LatLng(location.latitude, location.longitude)
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
    }
}