package com.shohan.walktracker.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "walk_sessions")
data class WalkSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTime: Long,
    val endTime: Long,
    val steps: Int,
    val distanceMeters: Double,
    val durationMs: Long,
    val avgSpeedKmh: Float,
    val maxSpeedKmh: Float,
    val calories: Float
)
