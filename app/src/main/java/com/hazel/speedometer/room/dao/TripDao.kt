package com.hazel.speedometer.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.hazel.speedometer.room.beans.Trip
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {

    @Insert
    suspend fun insertTrip(trip: Trip)

    @Query("SELECT * FROM Trip")
    fun getAllTrips(): Flow<List<Trip>>

    @Query("DELETE FROM Trip WHERE id IN (:selectedItems)")
    suspend fun deleteTrips(selectedItems: List<Int>) : Int
}