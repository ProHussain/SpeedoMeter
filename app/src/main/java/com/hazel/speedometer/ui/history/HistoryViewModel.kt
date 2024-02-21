package com.hazel.speedometer.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hazel.speedometer.repo.TripRepo
import com.hazel.speedometer.room.beans.Trip
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repo:TripRepo
) : ViewModel() {

    private val _trips = MutableStateFlow<List<Trip>>(emptyList())
    val trips = _trips.asStateFlow()

    private val _deleteTrip = MutableStateFlow(false)
    val deleteTrip = _deleteTrip.asStateFlow()

    init {
        getTrips()
    }

    private fun getTrips() {
        viewModelScope.launch {
            repo.getAllTrips().collectLatest {
                _trips.value = it
            }
        }
    }

    fun deleteTrips(selectedItems: MutableList<Trip>) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repo.deleteTrips(selectedItems.map { it.id }).collectLatest {
                    _deleteTrip.value = true
                    delay(500)
                    _deleteTrip.value = false
                }
            }
        }
    }
}