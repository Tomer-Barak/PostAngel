package com.postangel.screenshare

import android.content.Context
import android.content.SharedPreferences

object PrefsUtil {    
    private const val PREFS_NAME = "PostAngelPrefs"
    private const val KEY_OPENAI_API_KEY = "openai_api_key" // Legacy key, kept for backward compatibility
    private const val KEY_DARK_MODE = "dark_mode"
    
    // Default API URLs and model names - now configurable
    private const val DEFAULT_API_URL = "https://api.openai.com/v1/chat/completions"
    private const val DEFAULT_VISION_MODEL = "gpt-4.1-mini"
    private const val DEFAULT_RESPONSE_MODEL = "gpt-4o-mini"
    private const val DEFAULT_POST_GENERATION_MODEL = "gpt-4o-mini"
    
    // Keys for storing model settings
    private const val KEY_API_URL = "api_url"
    private const val KEY_VISION_MODEL = "vision_model"
    private const val KEY_RESPONSE_MODEL = "response_model" 
    private const val KEY_POST_GENERATION_MODEL = "post_generation_model"
    
    // Keys for API URLs
    private const val KEY_VISION_API_URL = "vision_api_url"
    private const val KEY_RESPONSE_API_URL = "response_api_url"
    private const val KEY_POST_GENERATION_API_URL = "post_generation_api_url"
    
    // Key for using global API key setting
    private const val KEY_USE_GLOBAL_API_KEY = "use_global_api_key"
    
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
      fun getServerUrl(context: Context): String {
        return getPrefs(context).getString(KEY_API_URL, DEFAULT_API_URL) ?: DEFAULT_API_URL
    }
    
    fun setServerUrl(context: Context, url: String) {
        getPrefs(context).edit().putString(KEY_API_URL, url).apply()
    }
    
    fun isUsingGlobalApiKey(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_USE_GLOBAL_API_KEY, true)
    }
    
    fun setUseGlobalApiKey(context: Context, useGlobal: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_USE_GLOBAL_API_KEY, useGlobal).apply()
    }
    
    // Vision API URL methods
    fun getVisionApiUrl(context: Context): String {
        val specificUrl = getPrefs(context).getString(KEY_VISION_API_URL, "")
        return if (specificUrl.isNullOrEmpty()) getServerUrl(context) else specificUrl
    }
    
    fun setVisionApiUrl(context: Context, url: String) {
        getPrefs(context).edit().putString(KEY_VISION_API_URL, url).apply()
    }
    
    // Response API URL methods
    fun getResponseApiUrl(context: Context): String {
        val specificUrl = getPrefs(context).getString(KEY_RESPONSE_API_URL, "")
        return if (specificUrl.isNullOrEmpty()) getServerUrl(context) else specificUrl
    }
    
    fun setResponseApiUrl(context: Context, url: String) {
        getPrefs(context).edit().putString(KEY_RESPONSE_API_URL, url).apply()
    }
    
    // Post Generation API URL methods
    fun getPostGenerationApiUrl(context: Context): String {
        val specificUrl = getPrefs(context).getString(KEY_POST_GENERATION_API_URL, "")
        return if (specificUrl.isNullOrEmpty()) getServerUrl(context) else specificUrl
    }
    
    fun setPostGenerationApiUrl(context: Context, url: String) {
        getPrefs(context).edit().putString(KEY_POST_GENERATION_API_URL, url).apply()
    }
    
    fun getVisionModel(context: Context): String {
        return getPrefs(context).getString(KEY_VISION_MODEL, DEFAULT_VISION_MODEL) ?: DEFAULT_VISION_MODEL
    }
    
    fun setVisionModel(context: Context, model: String) {
        getPrefs(context).edit().putString(KEY_VISION_MODEL, model).apply()
    }
    
    fun getResponseModel(context: Context): String {
        return getPrefs(context).getString(KEY_RESPONSE_MODEL, DEFAULT_RESPONSE_MODEL) ?: DEFAULT_RESPONSE_MODEL
    }
    
    fun setResponseModel(context: Context, model: String) {
        getPrefs(context).edit().putString(KEY_RESPONSE_MODEL, model).apply()
    }
    
    fun getPostGenerationModel(context: Context): String {
        return getPrefs(context).getString(KEY_POST_GENERATION_MODEL, DEFAULT_POST_GENERATION_MODEL) ?: DEFAULT_POST_GENERATION_MODEL
    }
    
    fun setPostGenerationModel(context: Context, model: String) {
        getPrefs(context).edit().putString(KEY_POST_GENERATION_MODEL, model).apply()
    }
      fun getOpenAIApiKey(context: Context): String {
        return getPrefs(context).getString(KEY_OPENAI_API_KEY, "") ?: ""
    }
    
    fun setOpenAIApiKey(context: Context, apiKey: String) {
        getPrefs(context).edit().putString(KEY_OPENAI_API_KEY, apiKey).apply()
    }
      fun clearOpenAIApiKey(context: Context) {
        getPrefs(context).edit().remove(KEY_OPENAI_API_KEY).apply()
    }
    
    fun isDarkModeEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_DARK_MODE, false)
    }
    
    fun setDarkModeEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_DARK_MODE, enabled).apply()
    }
}
