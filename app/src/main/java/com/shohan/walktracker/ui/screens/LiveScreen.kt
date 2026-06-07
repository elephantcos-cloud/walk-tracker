package com.shohan.walktracker.ui.screens

import android.view.MotionEvent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import com.shohan.walktracker.data.TrackingState
import com.shohan.walktracker.ui.theme.*
import com.shohan.walktracker.viewmodel.TrackingViewModel
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.Marker
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.*

@Composable
fun LiveScreen(vm: TrackingViewModel) {
    val state by vm.state.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .verticalScroll(scrollState)
            .padding(bottom = 16.dp)
    ) {
        // ── Top bar ───────────────────────────────────────────────────────────
        TopBar(state)

        // ── Big speed display ─────────────────────────────────────────────────
        SpeedCircle(state)

        // ── Stats grid ────────────────────────────────────────────────────────
        StatsGrid(state)

        Spacer(Modifier.height(12.dp))

        // ── Route map ─────────────────────────────────────────────────────────
        RouteMapCard(state)

        Spacer(Modifier.height(20.dp))

        // ── Control buttons ───────────────────────────────────────────────────
        ControlButtons(state, vm)
    }
}

// ── Top Bar ───────────────────────────────────────────────────────────────────
@Composable
private fun TopBar(state: TrackingState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("পথচলা", style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold, color = GreenPrimary)
            Text("Walking Tracker", style = MaterialTheme.typography.labelMedium,
                color = TextSecondary)
        }
        StatusBadge(state)
    }
}

@Composable
private fun StatusBadge(state: TrackingState) {
    val (color, text) = when {
        !state.isTracking -> Pair(TextHint, "প্রস্তুত")
        state.isPaused    -> Pair(OrangeAccent, "বিরতি")
        else              -> Pair(GreenPrimary, "● হাঁটছি")
    }
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.8f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "alpha"
    )
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(text, color = color.copy(alpha = if (state.isTracking && !state.isPaused) pulse else 1f),
            style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    }
}

// ── Speed Circle ─────────────────────────────────────────────────────────────
@Composable
private fun SpeedCircle(state: TrackingState) {
    val animSpeed by animateFloatAsState(
        targetValue = state.currentSpeedKmh,
        animationSpec = tween(500),
        label = "speed"
    )
    val maxSpeed = 10f
    val fraction = (animSpeed / maxSpeed).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(200.dp)) {
            val stroke = 16.dp.toPx()
            val radius = (size.minDimension / 2) - stroke

            // Background arc
            drawArc(
                color = CardDarker,
                startAngle = 140f,
                sweepAngle = 260f,
                useCenter = false,
                style = Stroke(stroke, cap = StrokeCap.Round),
                topLeft = Offset(stroke, stroke),
                size = Size(size.width - stroke * 2, size.height - stroke * 2)
            )

            // Speed arc
            if (fraction > 0f) {
                val brush = Brush.sweepGradient(
                    listOf(GreenPrimary, BluePrimary),
                    center = Offset(size.width / 2, size.height / 2)
                )
                drawArc(
                    brush = brush,
                    startAngle = 140f,
                    sweepAngle = 260f * fraction,
                    useCenter = false,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                    topLeft = Offset(stroke, stroke),
                    size = Size(size.width - stroke * 2, size.height - stroke * 2)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "%.1f".format(animSpeed),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.ExtraBold,
                color = if (state.isTracking && !state.isPaused) GreenPrimary else TextSecondary,
                fontSize = 52.sp
            )
            Text("km/h", color = TextSecondary, style = MaterialTheme.typography.titleMedium)
            Text("বর্তমান গতি", color = TextHint, style = MaterialTheme.typography.labelMedium)
        }
    }
}

// ── Stats Grid ────────────────────────────────────────────────────────────────
@Composable
private fun StatsGrid(state: TrackingState) {
    val distKm = state.distanceMeters / 1000.0
    val min    = state.durationMs / 60000
    val sec    = (state.durationMs % 60000) / 1000
    val pace   = if (state.pace > 0) "%d:%02d".format(state.pace.toInt(), ((state.pace % 1) * 60).toInt()) else "--"

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard("পদক্ষেপ", "${state.steps}", "Steps",
                Icons.Filled.DirectionsWalk, GreenPrimary, Modifier.weight(1f))
            StatCard("দূরত্ব", "%.2f".format(distKm), "কিমি / km",
                Icons.Filled.Route, BluePrimary, Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard("সময়", "%d:%02d".format(min, sec), "মিনিট:সেকেন্ড",
                Icons.Filled.Timer, OrangeAccent, Modifier.weight(1f))
            StatCard("ক্যালোরি", "%.0f".format(state.calories), "kcal",
                Icons.Filled.LocalFireDepartment, Color(0xFFFF4444), Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard("গড় গতি", "%.1f".format(state.avgSpeedKmh), "km/h avg",
                Icons.Filled.Speed, Color(0xFFAA00FF), Modifier.weight(1f))
            StatCard("পেস", pace, "মিনিট/কিমি",
                Icons.Filled.TrendingUp, GreenDark, Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard("সর্বোচ্চ গতি", "%.1f".format(state.maxSpeedKmh), "km/h max",
                Icons.Filled.Bolt, OrangeAccent, Modifier.weight(1f))
            StatCard("ক্যাডেন্স", "%.0f".format(state.cadence), "ধাপ/মিনিট",
                Icons.Filled.MonitorHeart, BluePrimary, Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    unit: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null,
                    tint = accentColor, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(label, color = TextSecondary,
                    style = MaterialTheme.typography.labelMedium)
            }
            Spacer(Modifier.height(8.dp))
            Text(value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(unit, color = TextHint,
                style = MaterialTheme.typography.labelMedium)
        }
    }
}

// ── Route Map ─────────────────────────────────────────────────────────────────
@Composable
private fun RouteMapCard(state: TrackingState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(220.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box {
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(17.0)
                        isTilesScaledToDpi = true
                    }
                },
                update = { mapView ->
                    mapView.overlays.clear()
                    val pts = state.routePoints
                    if (pts.size >= 2) {
                        val polyline = Polyline().apply {
                            setPoints(pts)
                            outlinePaint.color = android.graphics.Color.parseColor("#00E676")
                            outlinePaint.strokeWidth = 8f
                        }
                        mapView.overlays.add(polyline)
                    }
                    if (pts.isNotEmpty()) {
                        val last = pts.last()
                        mapView.controller.animateTo(last)
                        val marker = Marker(mapView).apply {
                            position = last
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            title = "আমি এখানে"
                        }
                        mapView.overlays.add(marker)
                    }
                    mapView.invalidate()
                },
                modifier = Modifier.fillMaxSize()
            )
            if (state.routePoints.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().background(CardDark),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Map, contentDescription = null,
                            tint = TextHint, modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("হাঁটা শুরু করলে রুট দেখাবে",
                            color = TextHint, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            // GPS accuracy badge
            if (state.isTracking && state.gpsAccuracy > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(
                            if (state.gpsAccuracy < 15) GreenPrimary.copy(0.9f)
                            else OrangeAccent.copy(0.9f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("GPS ±${state.gpsAccuracy.toInt()}m",
                        color = Color.Black, style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ── Control Buttons ───────────────────────────────────────────────────────────
@Composable
private fun ControlButtons(state: TrackingState, vm: TrackingViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when {
            // Not tracking → big Start button
            !state.isTracking -> {
                Button(
                    onClick = { vm.startTracking() },
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null,
                        modifier = Modifier.size(28.dp), tint = Color.Black)
                    Spacer(Modifier.width(10.dp))
                    Text("হাঁটা শুরু করো", fontSize = 18.sp,
                        fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }

            // Paused → Resume + Stop
            state.isPaused -> {
                Button(
                    onClick = { vm.resumeTracking() },
                    modifier = Modifier.weight(1f).height(60.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.Black)
                    Spacer(Modifier.width(6.dp))
                    Text("চালিয়ে যাও", color = Color.Black, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { vm.stopTracking() },
                    modifier = Modifier.weight(1f).height(60.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4444)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Filled.Stop, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("শেষ করো", fontWeight = FontWeight.Bold)
                }
            }

            // Tracking → Pause + Stop
            else -> {
                Button(
                    onClick = { vm.pauseTracking() },
                    modifier = Modifier.weight(1f).height(60.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Filled.Pause, contentDescription = null, tint = Color.Black)
                    Spacer(Modifier.width(6.dp))
                    Text("বিরতি", color = Color.Black, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { vm.stopTracking() },
                    modifier = Modifier.weight(1f).height(60.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF4444)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Filled.Stop, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("শেষ করো", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
