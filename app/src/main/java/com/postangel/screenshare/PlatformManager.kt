package com.postangel.screenshare

import android.content.Context

/**
 * Manages temporary social media platform settings
 */
object PlatformManager {
    private var temporaryPlatform: String? = null
    private var usingTemporaryPlatform = false

    /**
     * Check if currently using a temporary platform setting
     */
    fun isUsingTemporaryPlatform(): Boolean {
        return usingTemporaryPlatform
    }

    /**
     * Get the current platform (either temporary or from preferences)
     */
    fun getCurrentPlatform(context: Context): String {
        return if (usingTemporaryPlatform && temporaryPlatform != null) {
            temporaryPlatform!!
        } else {
            PrefsUtil.getSocialMediaPlatform(context)
        }
    }

    /**
     * Toggle or set the platform to a specific value temporarily
     * @return The new platform after toggle or change
     */
    fun toggleTemporaryPlatform(context: Context): String {
        val currentPlatform = getCurrentPlatform(context)
        // Toggle between LinkedIn and X
        temporaryPlatform = if (currentPlatform == PrefsUtil.PLATFORM_LINKEDIN) {
            PrefsUtil.PLATFORM_X
        } else {
            PrefsUtil.PLATFORM_LINKEDIN
        }
        usingTemporaryPlatform = true
        return temporaryPlatform!!
    }
    
    /**
     * Set a specific platform temporarily
     */
    fun setTemporaryPlatform(platform: String) {
        temporaryPlatform = platform
        usingTemporaryPlatform = true
    }

    /**
     * Clear temporary platform setting
     */
    fun clearTemporaryPlatform() {
        temporaryPlatform = null
        usingTemporaryPlatform = false
    }
}
