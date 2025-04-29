package com.postmuse.screenshare

import android.util.Log
import org.json.JSONObject

/**
 * Utility class for logging that handles sensitive information securely.
 */
object SecureLogUtil {
    /**
     * Log a debug message with sanitized content
     * @param tag Log tag
     * @param message Message prefix
     * @param jsonContent JSON content that might contain sensitive data
     */
    fun logDebug(tag: String, message: String, jsonContent: String) {
        try {
            val sanitizedJson = sanitizeJson(jsonContent)
            Log.d(tag, "$message: $sanitizedJson")
        } catch (e: Exception) {
            // If we can't parse as JSON, just log a note that content was hidden
            Log.d(tag, "$message: [Content hidden for security]")
        }
    }

    /**
     * Log an error with sanitized content
     * @param tag Log tag
     * @param message Error message
     * @param jsonContent JSON content that might contain sensitive data
     */
    fun logError(tag: String, message: String, jsonContent: String) {
        try {
            val sanitizedJson = sanitizeJson(jsonContent)
            Log.e(tag, "$message: $sanitizedJson")
        } catch (e: Exception) {
            Log.e(tag, "$message: [Content hidden for security]")
        }
    }

    /**
     * Sanitize JSON by removing or masking sensitive fields
     */
    private fun sanitizeJson(jsonContent: String): String {
        try {
            val jsonObject = JSONObject(jsonContent)
            
            // Remove or mask sensitive fields if they exist
            val sensitiveFields = listOf(
                "api_key", "apiKey", "key", "authorization", "Authorization",
                "Bearer", "bearer", "token", "access_token", "refresh_token"
            )
            
            for (field in sensitiveFields) {
                if (jsonObject.has(field)) {
                    jsonObject.put(field, "********")
                }
            }
            
            // Special case for Authorization header in any nested objects
            maskAuthorizationHeaders(jsonObject)
            
            return jsonObject.toString()
        } catch (e: Exception) {
            return "[JSON content hidden]"
        }
    }
    
    /**
     * Recursively look for and mask authorization headers in JSON objects
     */
    private fun maskAuthorizationHeaders(jsonObject: JSONObject) {
        val keys = jsonObject.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = jsonObject.opt(key)
            
            if (value is JSONObject) {
                maskAuthorizationHeaders(value)
            } else if (key.equals("Authorization", ignoreCase = true) || 
                      key.equals("headers", ignoreCase = true) || 
                      (value is String && value.toString().startsWith("Bearer "))) {
                jsonObject.put(key, "Bearer ********")
            }
        }
    }
}
