package com.postangel.screenshare

import android.content.Context
import android.content.SharedPreferences

object PrefsUtil {    
    private const val PREFS_NAME = "PostAngelPrefs"
    private const val KEY_SERVER_URL = "server_url"
    private const val DEFAULT_SERVER_URL = "https://api.openai.com/v1/chat/completions" // OpenAI API URL
    private const val KEY_OPENAI_API_KEY = "openai_api_key"
    private const val KEY_DARK_MODE = "dark_mode"
    
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    fun getServerUrl(context: Context): String {
        return getPrefs(context).getString(KEY_SERVER_URL, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL
    }
    
    fun setServerUrl(context: Context, serverUrl: String) {
        getPrefs(context).edit().putString(KEY_SERVER_URL, serverUrl).apply()
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
