package com.hazel.speedometer.room.db

import com.hazel.speedometer.room.beans.Trip
import com.hazel.speedometer.room.dao.TripDao

@androidx.room.Database(entities = [Trip::class], version = 1, exportSchema = false)
abstract class SpeedoMeterDb : androidx.room.RoomDatabase() {
    abstract fun tripDao(): TripDao
}