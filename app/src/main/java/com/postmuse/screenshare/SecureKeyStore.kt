package com.postangel.screenshare

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
    private const val GLOBAL_API_KEY = "global_api_key"
    private const val VISION_API_KEY = "vision_api_key"
    private const val RESPONSE_API_KEY = "response_api_key"
    private const val POST_GENERATION_API_KEY = "post_generation_api_key"
    
    /**
     * Get the OpenAI API key from secure storage (legacy method)
     * @param context Application context
     * @return The stored API key or empty string if not set
     */
    fun getOpenAIApiKey(context: Context): String {
        val encryptedPrefs = getEncryptedSharedPreferences(context)
        return encryptedPrefs.getString(OPENAI_API_KEY, "") ?: ""
    }
    
    /**
     * Save the OpenAI API key to secure storage (legacy method)
     * @param context Application context
     * @param apiKey The API key to store
     */
    fun setOpenAIApiKey(context: Context, apiKey: String) {
        val encryptedPrefs = getEncryptedSharedPreferences(context)
        encryptedPrefs.edit().putString(OPENAI_API_KEY, apiKey).apply()
        
        // Also save as global API key for backward compatibility
        setGlobalApiKey(context, apiKey)
    }
    
    /**
     * Get the global API key from secure storage
     * @param context Application context
     * @return The stored global API key or empty string if not set
     */
    fun getGlobalApiKey(context: Context): String {
        val encryptedPrefs = getEncryptedSharedPreferences(context)
        return encryptedPrefs.getString(GLOBAL_API_KEY, "") ?: ""
    }
    
    /**
     * Save the global API key to secure storage
     * @param context Application context
     * @param apiKey The API key to store
     */
    fun setGlobalApiKey(context: Context, apiKey: String) {
        val encryptedPrefs = getEncryptedSharedPreferences(context)
        encryptedPrefs.edit().putString(GLOBAL_API_KEY, apiKey).apply()
    }
    
    /**
     * Get specific API key for a service
     * @param context Application context
     * @param keyName Name of the key to retrieve
     * @return The stored API key or empty string if not set
     */
    private fun getApiKey(context: Context, keyName: String): String {
        val encryptedPrefs = getEncryptedSharedPreferences(context)
        return encryptedPrefs.getString(keyName, "") ?: ""
    }
    
    /**
     * Save specific API key for a service
     * @param context Application context
     * @param keyName Name of the key to store
     * @param apiKey The API key to store
     */
    private fun setApiKey(context: Context, keyName: String, apiKey: String) {
        val encryptedPrefs = getEncryptedSharedPreferences(context)
        encryptedPrefs.edit().putString(keyName, apiKey).apply()
    }
    
    /**
     * Get the Vision API key from secure storage
     * @param context Application context
     * @param useGlobalIfEmpty Use global key if specific key is empty
     * @return The stored API key or global key if appropriate
     */
    fun getVisionApiKey(context: Context, useGlobalIfEmpty: Boolean = true): String {
        val key = getApiKey(context, VISION_API_KEY)
        return if (key.isEmpty() && useGlobalIfEmpty) getGlobalApiKey(context) else key
    }
    
    /**
     * Set the Vision API key
     * @param context Application context
     * @param apiKey The API key to store
     */
    fun setVisionApiKey(context: Context, apiKey: String) {
        setApiKey(context, VISION_API_KEY, apiKey)
    }
    
    /**
     * Get the Response API key from secure storage
     * @param context Application context
     * @param useGlobalIfEmpty Use global key if specific key is empty
     * @return The stored API key or global key if appropriate
     */
    fun getResponseApiKey(context: Context, useGlobalIfEmpty: Boolean = true): String {
        val key = getApiKey(context, RESPONSE_API_KEY)
        return if (key.isEmpty() && useGlobalIfEmpty) getGlobalApiKey(context) else key
    }
    
    /**
     * Set the Response API key
     * @param context Application context
     * @param apiKey The API key to store
     */
    fun setResponseApiKey(context: Context, apiKey: String) {
        setApiKey(context, RESPONSE_API_KEY, apiKey)
    }
    
    /**
     * Get the Post Generation API key from secure storage
     * @param context Application context
     * @param useGlobalIfEmpty Use global key if specific key is empty
     * @return The stored API key or global key if appropriate
     */
    fun getPostGenerationApiKey(context: Context, useGlobalIfEmpty: Boolean = true): String {
        val key = getApiKey(context, POST_GENERATION_API_KEY)
        return if (key.isEmpty() && useGlobalIfEmpty) getGlobalApiKey(context) else key
    }
    
    /**
     * Set the Post Generation API key
     * @param context Application context
     * @param apiKey The API key to store
     */
    fun setPostGenerationApiKey(context: Context, apiKey: String) {
        setApiKey(context, POST_GENERATION_API_KEY, apiKey)
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
