package com.hazel.speedometer.room.beans

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Duration

@Entity
data class Trip(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val start: String = "",
    val end: String = "",
    val distance: Double = 0.0,
    val averageSpeed: Double = 0.0,
    val maxSpeed: Double = 0.0,
    val duration: Int = 0,
    val date: Long = 0L
)
