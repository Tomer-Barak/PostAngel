package com.postmuse.screenshare

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class PostMuseApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize dark mode based on saved preference
        val isDarkModeEnabled = PrefsUtil.isDarkModeEnabled(applicationContext)
        if (isDarkModeEnabled) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }
}
