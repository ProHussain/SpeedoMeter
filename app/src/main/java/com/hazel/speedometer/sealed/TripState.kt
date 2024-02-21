package com.hazel.speedometer.sealed

import com.hazel.speedometer.ui.main.MainViewModel

sealed class TripState {
    object Started : TripState()
    object Paused : TripState()
    object Stopped : TripState()
    object Idle : TripState()
}