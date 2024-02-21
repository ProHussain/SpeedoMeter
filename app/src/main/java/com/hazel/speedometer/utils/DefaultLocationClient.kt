package com.hazel.speedometer.utils

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import timber.log.Timber

class DefaultLocationClient(
    private val context: Context,
    private val client: FusedLocationProviderClient
) : LocationClient {

    private lateinit var locationCallback: LocationCallback

    override fun startLocationUpdates(interval: Long): Flow<Location> {
        return callbackFlow {
            if (!context.checkLocationPermission()) {
                Timber.e("Location permission is not granted")
                throw Exception("Location permission is not granted")
            }

            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if (!gpsEnabled && !networkEnabled) {
                Timber.e("Location is not enabled")
                throw Exception("Location is not enabled")
            }

            val request = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,interval
            ).build()

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    super.onLocationResult(locationResult)
                    locationResult.locations.lastOrNull()?.let {
                        launch {
                            send(it)
                        }
                    }
                }
            }

            client.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())

            awaitClose {
                client.removeLocationUpdates(locationCallback)
            }
        }
    }

    override fun pauseLocationUpdates() {
        Timber.d("Pausing location updates")
        client.removeLocationUpdates(locationCallback)
    }

    override fun resumeLocationUpdates() {
        Timber.d("Resuming location updates")
        startLocationUpdates(1000L)
    }

    override fun stopLocationUpdates() {
        Timber.d("Stopping location updates")
        client.removeLocationUpdates(locationCallback)
    }
}