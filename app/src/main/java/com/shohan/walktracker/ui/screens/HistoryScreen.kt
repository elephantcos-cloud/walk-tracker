package com.shohan.walktracker.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.shohan.walktracker.data.db.WalkSession
import com.shohan.walktracker.ui.theme.*
import com.shohan.walktracker.viewmodel.TrackingViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(vm: TrackingViewModel) {
    val sessions by vm.allSessions.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(horizontal = 16.dp)
    ) {
        // Header
        Spacer(Modifier.height(16.dp))
        Text("ইতিহাস", style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold, color = TextPrimary)
        Text("History", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Spacer(Modifier.height(16.dp))

        if (sessions.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.DirectionsWalk, contentDescription = null,
                        tint = TextHint, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("এখনো কোনো হাঁটার রেকর্ড নেই",
                        color = TextHint, style = MaterialTheme.typography.bodyLarge)
                    Text("No records yet", color = TextHint,
                        style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(sessions) { session ->
                    SessionCard(session) { vm.deleteSession(session) }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun SessionCard(session: WalkSession, onDelete: () -> Unit) {
    var showDelete by remember { mutableStateOf(false) }
    val dateStr  = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        .format(Date(session.startTime))
    val distKm   = session.distanceMeters / 1000.0
    val min      = session.durationMs / 60000
    val sec      = (session.durationMs % 60000) / 1000

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(GreenPrimary.copy(0.15f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.DirectionsWalk, contentDescription = null,
                            tint = GreenPrimary, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(dateStr, color = TextPrimary,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold)
                        Text("${"%.2f".format(distKm)} কিমি  •  $min:${"" + "%02d".format(sec)}",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium)
                    }
                }
                IconButton(onClick = { showDelete = !showDelete }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = null, tint = TextHint)
                }
            }

            Spacer(Modifier.height(12.dp))
            Divider(color = CardDarker)
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                MiniStat("${session.steps}", "ধাপ", GreenPrimary)
                MiniStat("%.1f".format(session.avgSpeedKmh), "km/h গড়", BluePrimary)
                MiniStat("%.0f".format(session.calories), "kcal", OrangeAccent)
                MiniStat("%.1f".format(session.maxSpeedKmh), "km/h সর্বোচ্চ", Color(0xFFAA00FF))
            }

            if (showDelete) {
                Spacer(Modifier.height(12.dp))
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFF4444)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.DeleteOutline, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("মুছে ফেলো (Delete)")
                }
            }
        }
    }
}

@Composable
private fun MiniStat(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge)
        Text(label, color = TextHint, style = MaterialTheme.typography.labelMedium)
    }
}
