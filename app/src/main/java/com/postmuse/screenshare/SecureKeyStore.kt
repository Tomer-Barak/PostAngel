package com.postmuse.screenshare

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.KeyStore

/**
 * Utility class for securely handling API keys and sensitive information
 */
object SecureKeyStore {
    private const val ENCRYPTED_PREFS_FILE = "secure_preferences"
    private const val OPENAI_API_KEY = "openai_api_key"
    
    /**
     * Get the OpenAI API key from secure storage
     * @param context Application context
     * @return The stored API key or empty string if not set
     */
    fun getOpenAIApiKey(context: Context): String {
        val encryptedPrefs = getEncryptedSharedPreferences(context)
        return encryptedPrefs.getString(OPENAI_API_KEY, "") ?: ""
    }
    
    /**
     * Save the OpenAI API key to secure storage
     * @param context Application context
     * @param apiKey The API key to store
     */
    fun setOpenAIApiKey(context: Context, apiKey: String) {
        val encryptedPrefs = getEncryptedSharedPreferences(context)
        encryptedPrefs.edit().putString(OPENAI_API_KEY, apiKey).apply()
    }
    
    /**
     * Delete the stored OpenAI API key
     * @param context Application context
     */
    fun clearOpenAIApiKey(context: Context) {
        val encryptedPrefs = getEncryptedSharedPreferences(context)
        encryptedPrefs.edit().remove(OPENAI_API_KEY).apply()
    }
    
    /**
     * Check if an OpenAI API key has been stored
     * @param context Application context
     * @return True if an API key exists
     */
    fun hasOpenAIApiKey(context: Context): Boolean {
        val encryptedPrefs = getEncryptedSharedPreferences(context)
        val key = encryptedPrefs.getString(OPENAI_API_KEY, "")
        return !key.isNullOrEmpty()
    }
    
    /**
     * Get encrypted shared preferences instance
     * @param context Application context
     * @return EncryptedSharedPreferences instance
     */
    private fun getEncryptedSharedPreferences(context: Context): SharedPreferences {
        // Create or get the master key for encryption
        val masterKeyAlias = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        
        // Create encrypted shared preferences
        return EncryptedSharedPreferences.create(
            context,
            ENCRYPTED_PREFS_FILE,
            masterKeyAlias,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
}
