package com.volovod.alta

import android.app.Application
import com.my.tracker.MyTracker

class AltaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val appId = BuildConfig.MYTRACKER_APP_ID
        if (appId.isNotBlank()) {
            // Initialize MyTracker with app id
            MyTracker.initTracker(appId, this)
            // Enable debug logs in debug builds
            MyTracker.setDebugMode(BuildConfig.DEBUG)
        }
    }
}
