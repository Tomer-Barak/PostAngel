package com.postangel.screenshare

import android.content.Context

/**
 * Helper class for temporarily changing the app mode for analysis
 * without affecting the saved preference
 */
object TemporaryModeManager {
    
    // Flag to track if we're using temporary mode
    private var usingTemporaryMode = false
    
    // The temporary mode (true = dark/demon mode, false = light/angel mode)
    private var temporaryDarkMode = false
    
    /**
     * Check if the app is currently using a temporary mode
     */
    fun isUsingTemporaryMode(): Boolean {
        return usingTemporaryMode
    }
    
    /**
     * Get the current mode, considering temporary override if active
     * @return true if dark mode is active, false otherwise
     */
    fun getCurrentMode(context: Context): Boolean {
        return if (usingTemporaryMode) {
            temporaryDarkMode
        } else {
            PrefsUtil.isDarkModeEnabled(context)
        }
    }
    
    /**
     * Temporarily override the current mode
     * @param darkMode true for dark mode, false for light mode
     */
    fun setTemporaryMode(darkMode: Boolean) {
        temporaryDarkMode = darkMode
        usingTemporaryMode = true
    }
    
    /**
     * Clear the temporary mode and revert to the user's preference
     */
    fun clearTemporaryMode() {
        usingTemporaryMode = false
    }
      /**
     * Toggle temporary mode to the opposite of current active mode
     * @return the new mode (true for dark, false for light)
     */
    fun toggleTemporaryMode(context: Context): Boolean {
        // If already using temporary mode, toggle the temporary mode
        // Otherwise toggle based on the saved preference
        val currentMode = if (usingTemporaryMode) {
            temporaryDarkMode
        } else {
            PrefsUtil.isDarkModeEnabled(context)
        }
        
        temporaryDarkMode = !currentMode
        usingTemporaryMode = true
        return temporaryDarkMode
    }
}
