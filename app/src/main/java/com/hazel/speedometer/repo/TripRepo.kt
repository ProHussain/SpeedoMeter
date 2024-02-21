package com.hazel.speedometer.repo

import com.hazel.speedometer.room.beans.Trip
import com.hazel.speedometer.room.dao.TripDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class TripRepo @Inject constructor(
    private val tripDao: TripDao
) {

    suspend fun insertTrip(trip: Trip) {
        tripDao.insertTrip(trip)
    }

    fun getAllTrips(): Flow<List<Trip>> {
        return tripDao.getAllTrips()
    }

    suspend fun deleteTrips(selectedItems: List<Int>) = flow {
        emit(tripDao.deleteTrips(selectedItems))
    }
}