package com.shohan.walktracker.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WalkDao {
    @Insert
    suspend fun insertSession(session: WalkSession): Long

    @Delete
    suspend fun deleteSession(session: WalkSession)

    @Query("SELECT * FROM walk_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<WalkSession>>

    @Query("SELECT * FROM walk_sessions WHERE startTime >= :startOfDay ORDER BY startTime DESC")
    fun getTodaySessions(startOfDay: Long): Flow<List<WalkSession>>

    @Query("SELECT COALESCE(SUM(steps), 0) FROM walk_sessions WHERE startTime >= :startOfDay")
    fun getTodaySteps(startOfDay: Long): Flow<Int>

    @Query("SELECT COALESCE(SUM(distanceMeters), 0.0) FROM walk_sessions WHERE startTime >= :startOfDay")
    fun getTodayDistance(startOfDay: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(calories), 0.0) FROM walk_sessions WHERE startTime >= :startOfDay")
    fun getTodayCalories(startOfDay: Long): Flow<Float>

    @Query("SELECT COALESCE(SUM(durationMs), 0) FROM walk_sessions WHERE startTime >= :startOfDay")
    fun getTodayDuration(startOfDay: Long): Flow<Long>

    @Query("SELECT COALESCE(SUM(steps), 0) FROM walk_sessions WHERE startTime >= :startOfWeek")
    fun getWeekSteps(startOfWeek: Long): Flow<Int>

    @Query("SELECT COALESCE(SUM(distanceMeters), 0.0) FROM walk_sessions WHERE startTime >= :startOfWeek")
    fun getWeekDistance(startOfWeek: Long): Flow<Double>

    @Query("SELECT COALESCE(MAX(distanceMeters), 0.0) FROM walk_sessions")
    fun getBestDistance(): Flow<Double>

    @Query("SELECT COALESCE(MAX(maxSpeedKmh), 0.0) FROM walk_sessions")
    fun getBestSpeed(): Flow<Float>

    @Query("SELECT COUNT(*) FROM walk_sessions")
    fun getTotalSessions(): Flow<Int>
}
