package com.postmuse.screenshare

import android.content.Context

/**
 * Utility class to migrate existing data to secure storage
 */
object MigrationUtil {
    /**
     * Migrate API key from regular SharedPreferences to secure storage
     * @param context Application context
     * @return true if migration was successful or not needed, false if migration failed
     */
    fun migrateApiKeyToSecureStorage(context: Context): Boolean {
        try {
            // Check if we already have a key in secure storage
            if (SecureKeyStore.hasOpenAIApiKey(context)) {
                return true // No migration needed
            }
            
            // Get API key from old storage
            val apiKey = PrefsUtil.getOpenAIApiKey(context)
            if (apiKey.isEmpty()) {
                return true // No key to migrate
            }
            
            // Save to secure storage
            SecureKeyStore.setOpenAIApiKey(context, apiKey)
            
            // Optionally clear the old storage (commented out for safety initially)
            // We could implement this later after testing the migration thoroughly
            // PrefsUtil.clearOpenAIApiKey(context)
            
            return true
        } catch (e: Exception) {
            android.util.Log.e("MigrationUtil", "Failed to migrate API key to secure storage", e)
            return false
        }
    }
}
