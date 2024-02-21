package com.hazel.speedometer.ui.main

import android.Manifest.permission
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.google.android.material.tabs.TabLayoutMediator
import com.hazel.speedometer.databinding.ActivityMainBinding
import com.hazel.speedometer.sealed.TripState
import com.hazel.speedometer.ui.history.HistoryActivity
import com.hazel.speedometer.ui.main.adapter.ViewPagerAdapter
import com.hazel.speedometer.utils.reduceDragSensitivity
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: ViewPagerAdapter
    private val viewModel by viewModels<MainViewModel>()
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initPermissions()
        initTabs()
        initClickListeners()
        initCollectors()
    }

    private fun initCollectors() {
        lifecycleScope.launch {
            viewModel.tripDuration.collectLatest {
                val hours = it / 1000 / 60 / 60
                val minutes = it / 1000 / 60 % 60
                val seconds = it / 1000 % 60
                binding.tvDuration.text = String.format("%02d:%02d:%02d", hours, minutes, seconds)
            }
        }

        lifecycleScope.launch {
            viewModel.tripState.collectLatest {
                if (it == TripState.Stopped) {
                    endTripDialog()
                }
            }
        }
    }

    private fun initClickListeners() {
        binding.ibHistory.setOnClickListener{
            startActivity(Intent(this, HistoryActivity::class.java))
        }
    }

    private fun initPermissions() {
        val permissions = mutableListOf<String>()
        permissions.add(permission.ACCESS_FINE_LOCATION)
        permissions.add(permission.ACCESS_COARSE_LOCATION)
        Dexter.withContext(this)
            .withPermissions(permissions)
            .withListener(object : com.karumi.dexter.listener.multi.MultiplePermissionsListener {
                override fun onPermissionsChecked(p0: MultiplePermissionsReport?) {

                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    token?.continuePermissionRequest()
                }
            })
            .check()
    }

    private fun initTabs() {
        adapter = ViewPagerAdapter(this)
        binding.viewPager.adapter = adapter
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            when (position) {
                0 -> tab.text = "Digital"
                1 -> tab.text = "Analogue"
                2 -> tab.text = "Map"
            }
        }.attach()
        binding.viewPager.offscreenPageLimit = 3
    }

    private fun endTripDialog() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Trip Ended")
            .setMessage("Do you want to save this trip?")
            .setPositiveButton("Yes") { _, _ ->
                viewModel.saveTrip()
            }
            .setNegativeButton("No") { _, _ ->
                viewModel.resetTrip()
            }
            .create()
        dialog.setCancelable(false)
        dialog.show()
    }
}