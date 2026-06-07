package com.shohan.walktracker.data

import org.osmdroid.util.GeoPoint

data class TrackingState(
    val isTracking: Boolean = false,
    val isPaused: Boolean = false,
    val steps: Int = 0,
    val distanceMeters: Double = 0.0,
    val currentSpeedKmh: Float = 0f,
    val avgSpeedKmh: Float = 0f,
    val maxSpeedKmh: Float = 0f,
    val durationMs: Long = 0L,
    val calories: Float = 0f,
    val pace: Float = 0f,           // min/km
    val cadence: Float = 0f,        // steps/min
    val routePoints: List<GeoPoint> = emptyList(),
    val gpsAccuracy: Float = 0f,
    val useGps: Boolean = true
)
