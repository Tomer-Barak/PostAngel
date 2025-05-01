// filepath: c:\Users\tomer\My Drive\Stuff\BarakBot_Social_Media\app\src\main\java\com\postangel\screenshare\PostAngelApplication.kt
package com.postangel.screenshare

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import android.util.Log

class PostAngelApplication : Application() {
    companion object {
        private const val TAG = "PostAngelApplication"
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize dark mode based on saved preference
        val isDarkModeEnabled = PrefsUtil.isDarkModeEnabled(applicationContext)
        
        // Apply the appropriate theme and log the app mode
        if (isDarkModeEnabled) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            Log.d(TAG, "Starting app in PostDemon mode (dark theme)")
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            Log.d(TAG, "Starting app in PostAngel mode (light theme)")
        }
    }
}
