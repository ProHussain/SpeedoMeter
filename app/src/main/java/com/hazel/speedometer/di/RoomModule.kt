package com.hazel.speedometer.di

import android.content.Context
import androidx.room.Room
import com.hazel.speedometer.room.dao.TripDao
import com.hazel.speedometer.room.db.SpeedoMeterDb
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
class RoomModule {

    @Provides
    fun provideRoomDatabase(@ApplicationContext context: Context): SpeedoMeterDb {
        return Room.databaseBuilder(
            context,
            SpeedoMeterDb::class.java,
            "speedometer"
        ).build()
    }

    @Provides
    fun provideTripDao(roomDatabase: SpeedoMeterDb): TripDao {
        return roomDatabase.tripDao()
    }
}