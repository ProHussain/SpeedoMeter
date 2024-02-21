package com.hazel.speedometer.di

import android.content.Context
import com.google.android.gms.location.LocationServices
import com.hazel.speedometer.utils.DefaultLocationClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
class LocationModule {

    @Provides
    fun provideLocationDefaultClient(
        @ApplicationContext context: Context,
    ): DefaultLocationClient {
        return DefaultLocationClient(
            context,
            LocationServices.getFusedLocationProviderClient(context)
        )
    }
}