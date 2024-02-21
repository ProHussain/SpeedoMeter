package com.hazel.speedometer.ui.main

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import com.hazel.speedometer.repo.TripRepo
import com.hazel.speedometer.room.beans.Trip
import com.hazel.speedometer.sealed.TripState
import com.hazel.speedometer.utils.DefaultLocationClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val locationClient: DefaultLocationClient,
    private val tripRepo: TripRepo
) : ViewModel() {

    private val _currentSpeed = MutableStateFlow(0.0)
    val currentSpeed = _currentSpeed.asStateFlow()

    private val _distance = MutableStateFlow(0.0)
    val distance = _distance.asStateFlow()

    private val _maxSpeed = MutableStateFlow(0.0)
    val maxSpeed = _maxSpeed.asStateFlow()

    private val _avgSpeed = MutableStateFlow(0.0)
    val avgSpeed = _avgSpeed.asStateFlow()

    private val _tripDuration = MutableStateFlow(0)
    val tripDuration = _tripDuration.asStateFlow()

    private val _location = MutableStateFlow<Location?>(null)
    val location = _location.asStateFlow()

    lateinit var startLocation: Location
    lateinit var endLocation: Location
    private var callbackCount = 0
    private var totalSpeed = 0.0

    val tripState = MutableStateFlow<TripState>(TripState.Idle)

    fun startLocationUpdates() {
        tripState.value = TripState.Started
        startTripDuration()
        viewModelScope.launch {
            locationClient.startLocationUpdates(1000L).catch {
                Timber.e(it)
            }.collectLatest {
                val speed = it.speed * 3.6
                if (location.value == null) {
                    startLocation = it
                }
                _location.value = it
                _currentSpeed.value = speed
                _distance.value += speed / 3600
                _maxSpeed.value = maxOf(_maxSpeed.value, speed)

                callbackCount++
                totalSpeed += speed
                _avgSpeed.value = totalSpeed / callbackCount
            }
        }
    }

    fun pauseLocationUpdates() {
        tripState.value = TripState.Paused
        locationClient.pauseLocationUpdates()
    }

    fun resumeLocationUpdates() {
        tripState.value = TripState.Started
        startLocationUpdates()
    }

    fun stopLocationUpdates() {
        endLocation = _location.value?:Location("")
        tripState.value = TripState.Stopped
        locationClient.stopLocationUpdates()
    }

    private fun startTripDuration() {
        viewModelScope.launch {
            while (tripState.value == TripState.Started) {
                _tripDuration.value += 1000
                delay(1000)
            }
        }
    }

    fun saveTrip() {
        val endLocation =
            _location.value!!.latitude.toString() + ", " + _location.value!!.longitude.toString()
        val startLocation =
            startLocation.latitude.toString() + ", " + startLocation.longitude.toString()
        val trip = Trip(
            start = startLocation,
            end = endLocation,
            distance = _distance.value,
            maxSpeed = _maxSpeed.value,
            averageSpeed = _avgSpeed.value,
            duration = tripDuration.value,
            date = System.currentTimeMillis()
        )

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                tripRepo.insertTrip(trip)
                resetValues()
            }
        }
    }

    fun resetTrip() {
        resetValues()
    }

    private fun resetValues() {
        _currentSpeed.value = 0.0
        _location.value = null
        _distance.value = 0.0
        _maxSpeed.value = 0.0
        _avgSpeed.value = 0.0
        _tripDuration.value = 0
        callbackCount = 0
        totalSpeed = 0.0

        tripState.value = TripState.Idle
        startLocation = Location("")
        endLocation = Location("")
    }

    @SuppressLint("MissingPermission")
    fun getCurrentLocation(context: Context): Location {
        val locationManager =
            context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val dummyLocation = Location("")
        dummyLocation.latitude = 37.422
        dummyLocation.longitude = -122.084

        return locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            ?: locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
            ?: dummyLocation
    }

    fun getPolylineOptions(): PolylineOptions {
        val polylineOptions = PolylineOptions()
        polylineOptions.add(LatLng(startLocation.latitude, startLocation.longitude))
        polylineOptions.add(LatLng(location.value!!.latitude, location.value!!.longitude))
        return polylineOptions
    }
}