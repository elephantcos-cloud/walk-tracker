package com.shohan.walktracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shohan.walktracker.ui.screens.*
import com.shohan.walktracker.ui.theme.*
import com.shohan.walktracker.viewmodel.TrackingViewModel

class MainActivity : ComponentActivity() {

    private val requiredPermissions = buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            add(Manifest.permission.ACTIVITY_RECOGNITION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            add(Manifest.permission.POST_NOTIFICATIONS)
    }.toTypedArray()

    private var permissionsGranted by mutableStateOf(false)

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permissionsGranted = results.values.all { it }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionsGranted = requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        setContent {
            WalkTrackerTheme {
                if (permissionsGranted) {
                    MainApp()
                } else {
                    PermissionScreen { permLauncher.launch(requiredPermissions) }
                }
            }
        }
    }
}

@Composable
private fun MainApp() {
    val vm: TrackingViewModel = viewModel()
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        containerColor = BgDark,
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceDark,
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick  = { selectedTab = 0 },
                    icon     = { Icon(Icons.Filled.DirectionsWalk, null) },
                    label    = { Text("হাঁটো") },
                    colors   = NavigationBarItemDefaults.colors(
                        selectedIconColor   = GreenPrimary,
                        selectedTextColor   = GreenPrimary,
                        indicatorColor      = GreenPrimary.copy(0.15f),
                        unselectedIconColor = TextHint,
                        unselectedTextColor = TextHint
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick  = { selectedTab = 1 },
                    icon     = { Icon(Icons.Filled.History, null) },
                    label    = { Text("ইতিহাস") },
                    colors   = NavigationBarItemDefaults.colors(
                        selectedIconColor   = GreenPrimary,
                        selectedTextColor   = GreenPrimary,
                        indicatorColor      = GreenPrimary.copy(0.15f),
                        unselectedIconColor = TextHint,
                        unselectedTextColor = TextHint
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick  = { selectedTab = 2 },
                    icon     = { Icon(Icons.Filled.BarChart, null) },
                    label    = { Text("পরিসংখ্যান") },
                    colors   = NavigationBarItemDefaults.colors(
                        selectedIconColor   = GreenPrimary,
                        selectedTextColor   = GreenPrimary,
                        indicatorColor      = GreenPrimary.copy(0.15f),
                        unselectedIconColor = TextHint,
                        unselectedTextColor = TextHint
                    )
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> LiveScreen(vm)
                1 -> HistoryScreen(vm)
                2 -> StatsScreen(vm)
            }
        }
    }
}

@Composable
private fun PermissionScreen(onRequest: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(32.dp),
        verticalArrangement   = Arrangement.Center,
        horizontalAlignment   = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.LocationOn, contentDescription = null,
            tint = GreenPrimary, modifier = Modifier.size(72.dp))
        Spacer(Modifier.height(24.dp))
        Text("অনুমতি দরকার", style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold, color = TextPrimary)
        Text("Permission Required", color = TextSecondary)
        Spacer(Modifier.height(16.dp))
        Text(
            "সঠিকভাবে কাজ করতে হলে Location ও Activity Recognition অনুমতি দিতে হবে।",
            color = TextSecondary,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onRequest,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
        ) {
            Text("অনুমতি দাও", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}
