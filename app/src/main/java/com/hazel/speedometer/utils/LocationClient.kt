package com.hazel.speedometer.utils

import android.location.Location
import kotlinx.coroutines.flow.Flow

interface LocationClient {
    fun startLocationUpdates(interval: Long) : Flow<Location>
    fun pauseLocationUpdates()
    fun resumeLocationUpdates()
    fun stopLocationUpdates()
}