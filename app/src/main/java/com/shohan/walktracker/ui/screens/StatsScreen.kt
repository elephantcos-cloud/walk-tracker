package com.shohan.walktracker.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.shohan.walktracker.ui.theme.*
import com.shohan.walktracker.viewmodel.TrackingViewModel

@Composable
fun StatsScreen(vm: TrackingViewModel) {
    val todaySteps    by vm.todaySteps.collectAsState()
    val todayDist     by vm.todayDistance.collectAsState()
    val todayCal      by vm.todayCalories.collectAsState()
    val todayDur      by vm.todayDuration.collectAsState()
    val weekSteps     by vm.weekSteps.collectAsState()
    val weekDist      by vm.weekDistance.collectAsState()
    val totalSessions by vm.totalSessions.collectAsState()
    val bestDist      by vm.bestDistance.collectAsState()
    val bestSpeed     by vm.bestSpeed.collectAsState()

    val todayMin = todayDur / 60000
    val todaySec = (todayDur % 60000) / 1000

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Text("পরিসংখ্যান", style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold, color = TextPrimary)
        Text("Statistics", color = TextSecondary,
            style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(20.dp))

        // ── আজকের তথ্য ───────────────────────────────────────────────────────
        SectionHeader("🌅 আজকের তথ্য", "Today")
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BigStatCard("$todaySteps", "ধাপ", "Steps",
                Icons.Filled.DirectionsWalk, GreenPrimary, Modifier.weight(1f))
            BigStatCard("%.2f".format(todayDist / 1000), "কিমি", "km",
                Icons.Filled.Route, BluePrimary, Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BigStatCard("%.0f".format(todayCal), "ক্যালোরি", "kcal",
                Icons.Filled.LocalFireDepartment, Color(0xFFFF4444), Modifier.weight(1f))
            BigStatCard("%d:%02d".format(todayMin, todaySec), "সময়", "min:sec",
                Icons.Filled.Timer, OrangeAccent, Modifier.weight(1f))
        }

        Spacer(Modifier.height(24.dp))

        // ── এই সপ্তাহ ──────────────────────────────────────────────────────
        SectionHeader("📅 এই সপ্তাহ", "This Week")
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BigStatCard("$weekSteps", "ধাপ", "Steps",
                Icons.Filled.DirectionsWalk, GreenPrimary, Modifier.weight(1f))
            BigStatCard("%.1f".format(weekDist / 1000), "কিমি", "km",
                Icons.Filled.Route, BluePrimary, Modifier.weight(1f))
        }

        Spacer(Modifier.height(24.dp))

        // ── সর্বোচ্চ রেকর্ড ─────────────────────────────────────────────────
        SectionHeader("🏆 সেরা রেকর্ড", "Personal Records")
        Spacer(Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                RecordRow(Icons.Filled.EmojiEvents,
                    "সর্বোচ্চ দূরত্ব", "Best Distance",
                    "%.2f কিমি".format(bestDist / 1000), GreenPrimary)
                Divider(color = CardDarker)
                RecordRow(Icons.Filled.Bolt,
                    "সর্বোচ্চ গতি", "Best Speed",
                    "%.1f km/h".format(bestSpeed), OrangeAccent)
                Divider(color = CardDarker)
                RecordRow(Icons.Filled.FitnessCenter,
                    "মোট সেশন", "Total Sessions",
                    "$totalSessions টি", BluePrimary)
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun SectionHeader(bengali: String, english: String) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(bengali, style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(Modifier.width(8.dp))
        Text(english, style = MaterialTheme.typography.labelMedium,
            color = TextSecondary)
    }
}

@Composable
private fun BigStatCard(
    value: String, bengaliUnit: String, englishUnit: String,
    icon: ImageVector, color: Color, modifier: Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null,
                    tint = color, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold, color = TextPrimary)
            Text("$bengaliUnit / $englishUnit",
                color = TextSecondary, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun RecordRow(
    icon: ImageVector, bengali: String, english: String,
    value: String, color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null,
                tint = color, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(bengali, color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)
                Text(english, color = TextHint,
                    style = MaterialTheme.typography.labelMedium)
            }
        }
        Text(value, color = color, style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold)
    }
}
