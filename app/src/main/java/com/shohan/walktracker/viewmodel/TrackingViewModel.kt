package com.shohan.walktracker.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shohan.walktracker.TrackingService
import com.shohan.walktracker.data.db.AppDatabase
import com.shohan.walktracker.data.db.WalkSession
import com.shohan.walktracker.data.repository.WalkRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TrackingViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = WalkRepository(AppDatabase.getInstance(app))

    val state = TrackingService.state

    val allSessions = repo.allSessions.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val todaySteps    = repo.getTodaySteps().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val todayDistance = repo.getTodayDistance().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val todayCalories = repo.getTodayCalories().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)
    val todayDuration = repo.getTodayDuration().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)
    val weekSteps     = repo.getWeekSteps().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val weekDistance  = repo.getWeekDistance().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val totalSessions = repo.totalSessions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val bestDistance  = repo.bestDistance.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
    val bestSpeed     = repo.bestSpeed.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    private fun ctx() = getApplication<Application>()

    fun startTracking() {
        ctx().startForegroundService(
            Intent(ctx(), TrackingService::class.java).apply { action = TrackingService.ACTION_START }
        )
    }

    fun pauseTracking() {
        ctx().startService(
            Intent(ctx(), TrackingService::class.java).apply { action = TrackingService.ACTION_PAUSE }
        )
    }

    fun resumeTracking() {
        ctx().startService(
            Intent(ctx(), TrackingService::class.java).apply { action = TrackingService.ACTION_RESUME }
        )
    }

    fun stopTracking() {
        val s = state.value
        if (s.steps > 0 || s.distanceMeters > 10.0) {
            viewModelScope.launch {
                repo.saveSession(
                    WalkSession(
                        startTime      = System.currentTimeMillis() - s.durationMs,
                        endTime        = System.currentTimeMillis(),
                        steps          = s.steps,
                        distanceMeters = s.distanceMeters,
                        durationMs     = s.durationMs,
                        avgSpeedKmh    = s.avgSpeedKmh,
                        maxSpeedKmh    = s.maxSpeedKmh,
                        calories       = s.calories
                    )
                )
            }
        }
        ctx().startService(
            Intent(ctx(), TrackingService::class.java).apply { action = TrackingService.ACTION_STOP }
        )
    }

    fun deleteSession(session: WalkSession) {
        viewModelScope.launch { repo.deleteSession(session) }
    }
}
