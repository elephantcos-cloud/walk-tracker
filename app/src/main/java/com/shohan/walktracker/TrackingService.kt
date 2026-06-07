package com.shohan.walktracker

import android.app.*
import android.content.Context
import android.content.Intent
import android.hardware.*
import android.location.Location
import android.os.*
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.shohan.walktracker.data.TrackingState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.osmdroid.util.GeoPoint
import kotlin.math.roundToInt

class TrackingService : Service(), SensorEventListener {

    private lateinit var fusedClient: FusedLocationProviderClient
    private lateinit var sensorManager: SensorManager
    private var stepDetector: Sensor? = null

    // Session state
    private var sessionStartTime = 0L
    private var pausedDuration = 0L
    private var pauseStartTime = 0L
    private var lastLocation: Location? = null
    private var lastStepTime = 0L
    private var stepTimestamps = ArrayDeque<Long>(10)

    private val handler = Handler(Looper.getMainLooper())

    // Timer: updates duration, pace, cadence every second
    private val timerRunnable = object : Runnable {
        override fun run() {
            val s = _state.value
            if (s.isTracking && !s.isPaused) {
                val elapsed = System.currentTimeMillis() - sessionStartTime - pausedDuration
                val avgSpeed = if (elapsed > 0)
                    (s.distanceMeters / (elapsed / 1000.0) * 3.6).toFloat()
                else 0f
                val pace = if (avgSpeed > 0.5f) 60f / avgSpeed else 0f
                val cadence = computeCadence()
                val calories = calcCalories(elapsed)
                _state.update {
                    it.copy(
                        durationMs = elapsed,
                        avgSpeedKmh = avgSpeed,
                        pace = pace,
                        cadence = cadence,
                        calories = calories
                    )
                }
                updateNotification()
            }
            handler.postDelayed(this, 1000)
        }
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { loc ->
                if (isValidLocation(loc)) onNewLocation(loc)
            }
        }
    }

    // ── Companion ─────────────────────────────────────────────────────────────
    companion object {
        private val _state = MutableStateFlow(TrackingState())
        val state = _state.asStateFlow()

        const val ACTION_START  = "ACTION_START"
        const val ACTION_PAUSE  = "ACTION_PAUSE"
        const val ACTION_RESUME = "ACTION_RESUME"
        const val ACTION_STOP   = "ACTION_STOP"

        private const val CHANNEL_ID = "walk_tracker_channel"
        private const val NOTIF_ID   = 101
    }

    // ── Lifecycle ──────────────────────────────────────────────────────────────
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        fusedClient    = LocationServices.getFusedLocationProviderClient(this)
        sensorManager  = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepDetector   = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START  -> beginSession()
            ACTION_PAUSE  -> pauseSession()
            ACTION_RESUME -> resumeSession()
            ACTION_STOP   -> endSession()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        stopLocation()
        sensorManager.unregisterListener(this)
    }

    // ── Session Control ────────────────────────────────────────────────────────
    private fun beginSession() {
        sessionStartTime = System.currentTimeMillis()
        pausedDuration   = 0L
        stepTimestamps.clear()
        _state.value = TrackingState(isTracking = true)
        startForeground(NOTIF_ID, buildNotification())
        startLocation()
        stepDetector?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST)
        }
        handler.post(timerRunnable)
    }

    private fun pauseSession() {
        pauseStartTime = System.currentTimeMillis()
        _state.update { it.copy(isPaused = true, currentSpeedKmh = 0f) }
        stopLocation()
    }

    private fun resumeSession() {
        pausedDuration += System.currentTimeMillis() - pauseStartTime
        _state.update { it.copy(isPaused = false) }
        startLocation()
    }

    private fun endSession() {
        _state.update { it.copy(isTracking = false, isPaused = false, currentSpeedKmh = 0f) }
        handler.removeCallbacksAndMessages(null)
        stopLocation()
        sensorManager.unregisterListener(this)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // ── Location ───────────────────────────────────────────────────────────────
    private fun startLocation() {
        val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
            .setMinUpdateIntervalMillis(1000L)
            .setMinUpdateDistanceMeters(1f)
            .build()
        try {
            fusedClient.requestLocationUpdates(req, locationCallback, Looper.getMainLooper())
        } catch (_: SecurityException) {}
    }

    private fun stopLocation() {
        fusedClient.removeLocationUpdates(locationCallback)
        lastLocation = null
    }

    /**
     * Filters noise:
     * - Accuracy must be < 30m
     * - Must have moved at least 1.5m
     * - Implied speed must not exceed 15 km/h (walking limit)
     */
    private fun isValidLocation(loc: Location): Boolean {
        val s = _state.value
        if (!s.isTracking || s.isPaused) return false
        if (loc.accuracy > 30f) return false
        val last = lastLocation ?: return true
        val dist = loc.distanceTo(last)
        if (dist < 1.5f) return false
        val secs = (loc.time - last.time) / 1000f
        if (secs > 0 && (dist / secs * 3.6f) > 15f) return false
        return true
    }

    private fun onNewLocation(loc: Location) {
        val last = lastLocation
        val addedDist = last?.distanceTo(loc)?.toDouble() ?: 0.0

        // Raw GPS speed, fallback to computed
        val speedKmh = if (loc.hasSpeed() && loc.speed > 0.3f)
            loc.speed * 3.6f
        else if (last != null) {
            val dt = (loc.time - last.time) / 1000f
            if (dt > 0) (addedDist / dt * 3.6).toFloat() else 0f
        } else 0f

        val point = GeoPoint(loc.latitude, loc.longitude)
        _state.update { cur ->
            val newDist  = cur.distanceMeters + addedDist
            val newMax   = maxOf(cur.maxSpeedKmh, speedKmh)
            val newPts   = (cur.routePoints + point).takeLast(2000)
            cur.copy(
                distanceMeters  = newDist,
                currentSpeedKmh = speedKmh,
                maxSpeedKmh     = newMax,
                routePoints     = newPts,
                gpsAccuracy     = loc.accuracy,
                useGps          = true
            )
        }
        lastLocation = loc
    }

    // ── Step Sensor ────────────────────────────────────────────────────────────
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_STEP_DETECTOR) return
        val s = _state.value
        if (!s.isTracking || s.isPaused) return

        val now = System.currentTimeMillis()
        stepTimestamps.addLast(now)
        // keep only last 10 steps for cadence
        while (stepTimestamps.size > 10) stepTimestamps.removeFirst()
        lastStepTime = now

        // Pedometer-based distance when GPS unavailable / inaccurate
        val strideM = 0.75  // metres per step
        _state.update { cur ->
            val newSteps = cur.steps + 1
            val distToUse = if (cur.gpsAccuracy < 1f || cur.gpsAccuracy > 30f) {
                // GPS not active → use pedometer
                cur.distanceMeters + strideM
            } else cur.distanceMeters
            val speedFallback = if (cur.gpsAccuracy > 30f && cur.cadence > 0)
                (cur.cadence * strideM * 60 / 1000).toFloat()  // km/h
            else cur.currentSpeedKmh
            cur.copy(
                steps           = newSteps,
                distanceMeters  = distToUse,
                currentSpeedKmh = speedFallback
            )
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // ── Helpers ────────────────────────────────────────────────────────────────
    private fun computeCadence(): Float {
        if (stepTimestamps.size < 2) return 0f
        val span = (stepTimestamps.last() - stepTimestamps.first()) / 1000f
        return if (span > 0) (stepTimestamps.size - 1) / span * 60f else 0f
    }

    private fun calcCalories(durationMs: Long): Float {
        // MET ≈ 3.5 for moderate walking, weight 65 kg
        val hours = durationMs / 3_600_000f
        return (3.5f * 65f * hours)
    }

    // ── Notification ──────────────────────────────────────────────────────────
    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, "পথচলা ট্র্যাকার",
                NotificationManager.IMPORTANCE_LOW)
            ch.description = "হাঁটার তথ্য দেখায়"
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    private fun buildNotification(): Notification {
        val s   = _state.value
        val km  = "%.2f".format(s.distanceMeters / 1000.0)
        val min = s.durationMs / 60000
        val sec = (s.durationMs % 60000) / 1000
        val pi  = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val status = if (s.isPaused) "বিরতি" else "হাঁটছি"
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("পথচলা — $status")
            .setContentText("${s.steps} ধাপ  •  $km কিমি  •  $min:${"%02d".format(sec)}")
            .setSmallIcon(android.R.drawable.ic_menu_directions)
            .setContentIntent(pi)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun updateNotification() {
        val mgr = getSystemService(NotificationManager::class.java)
        mgr.notify(NOTIF_ID, buildNotification())
    }
}
