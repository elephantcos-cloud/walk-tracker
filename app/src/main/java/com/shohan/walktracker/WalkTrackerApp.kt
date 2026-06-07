package com.shohan.walktracker

import android.app.Application
import org.osmdroid.config.Configuration
import java.io.File

class WalkTrackerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // OSMDroid config
        Configuration.getInstance().apply {
            userAgentValue = packageName
            osmdroidTileCache = File(cacheDir, "osmdroid")
        }
    }
}
