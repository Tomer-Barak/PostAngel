package com.postangel.screenshare

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Utility class for storing and retrieving post history
 */
object PostHistoryManager {
    private const val TAG = "PostHistoryManager"
    private const val PREFS_NAME = "PostHistoryPrefs"
    private const val KEY_POSTS = "post_history"
    private const val MAX_POSTS = 100 // Maximum number of posts to store

    private const val JSON_ID = "id"
    private const val JSON_CONTENT = "content"
    private const val JSON_TIMESTAMP = "timestamp"
    private const val JSON_IS_DARK_MODE = "is_dark_mode"
    private const val JSON_SOURCE = "source"

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US)

    /**
     * Save a post to the history
     */
    fun savePost(context: Context, content: String, isDarkMode: Boolean, source: String) {
        try {
            // Create a new history entry
            val entry = PostHistoryEntry(
                id = UUID.randomUUID().toString(),
                content = content,
                timestamp = Date(),
                isDarkMode = isDarkMode,
                source = source
            )

            // Get existing posts
            val posts = getPosts(context).toMutableList()
            
            // Add new post at beginning
            posts.add(0, entry)
            
            // If over maximum limit, remove oldest posts
            while (posts.size > MAX_POSTS) {
                posts.removeAt(posts.size - 1)
            }
            
            // Save updated history
            savePosts(context, posts)
            
            Log.d(TAG, "Saved post to history: $content")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving post to history", e)
        }
    }

    /**
     * Get all posts from history
     */
    fun getPosts(context: Context): List<PostHistoryEntry> {
        val prefs = getPrefs(context)
        val postsJson = prefs.getString(KEY_POSTS, "[]") ?: "[]"
        
        return try {
            val postsArray = JSONArray(postsJson)
            val result = mutableListOf<PostHistoryEntry>()
            
            for (i in 0 until postsArray.length()) {
                val postObj = postsArray.getJSONObject(i)
                try {
                    val entry = PostHistoryEntry(
                        id = postObj.getString(JSON_ID),
                        content = postObj.getString(JSON_CONTENT),
                        timestamp = dateFormat.parse(postObj.getString(JSON_TIMESTAMP)) ?: Date(),
                        isDarkMode = postObj.getBoolean(JSON_IS_DARK_MODE),
                        source = postObj.optString(JSON_SOURCE, PostHistoryEntry.SOURCE_SHARE)
                    )
                    result.add(entry)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing post entry", e)
                }
            }
            
            result
        } catch (e: JSONException) {
            Log.e(TAG, "Error loading post history", e)
            emptyList()
        }
    }

    /**
     * Delete a post from history by ID
     */
    fun deletePost(context: Context, postId: String): Boolean {
        try {
            val posts = getPosts(context).toMutableList()
            val initialSize = posts.size
            posts.removeIf { it.id == postId }
            
            if (posts.size < initialSize) {
                savePosts(context, posts)
                return true
            }
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting post", e)
            return false
        }
    }

    /**
     * Clear all post history
     */
    fun clearHistory(context: Context) {
        getPrefs(context).edit().remove(KEY_POSTS).apply()
    }

    /**
     * Save posts list to SharedPreferences
     */
    private fun savePosts(context: Context, posts: List<PostHistoryEntry>) {
        val jsonArray = JSONArray()
        
        posts.forEach { entry ->
            val jsonObject = JSONObject().apply {
                put(JSON_ID, entry.id)
                put(JSON_CONTENT, entry.content)
                put(JSON_TIMESTAMP, dateFormat.format(entry.timestamp))
                put(JSON_IS_DARK_MODE, entry.isDarkMode)
                put(JSON_SOURCE, entry.source)
            }
            jsonArray.put(jsonObject)
        }
        
        getPrefs(context).edit()
            .putString(KEY_POSTS, jsonArray.toString())
            .apply()
    }

    /**
     * Get SharedPreferences instance
     */
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
