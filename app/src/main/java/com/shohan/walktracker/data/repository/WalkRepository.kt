package com.shohan.walktracker.data.repository

import com.shohan.walktracker.data.db.AppDatabase
import com.shohan.walktracker.data.db.WalkSession
import java.util.Calendar

class WalkRepository(private val db: AppDatabase) {

    val allSessions = db.walkDao().getAllSessions()
    val totalSessions = db.walkDao().getTotalSessions()
    val bestDistance = db.walkDao().getBestDistance()
    val bestSpeed = db.walkDao().getBestSpeed()

    private fun startOfDay(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun startOfWeek(): Long = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    fun getTodaySessions() = db.walkDao().getTodaySessions(startOfDay())
    fun getTodaySteps() = db.walkDao().getTodaySteps(startOfDay())
    fun getTodayDistance() = db.walkDao().getTodayDistance(startOfDay())
    fun getTodayCalories() = db.walkDao().getTodayCalories(startOfDay())
    fun getTodayDuration() = db.walkDao().getTodayDuration(startOfDay())
    fun getWeekSteps() = db.walkDao().getWeekSteps(startOfWeek())
    fun getWeekDistance() = db.walkDao().getWeekDistance(startOfWeek())

    suspend fun saveSession(session: WalkSession) = db.walkDao().insertSession(session)
    suspend fun deleteSession(session: WalkSession) = db.walkDao().deleteSession(session)
}
