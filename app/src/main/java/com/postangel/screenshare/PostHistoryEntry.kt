package com.postangel.screenshare

import java.util.Date

/**
 * Data class representing an entry in the post history
 */
data class PostHistoryEntry(
    val id: String,         // Unique identifier
    val content: String,    // Actual post content
    val timestamp: Date,    // When the post was created
    val isDarkMode: Boolean, // Whether it was created in PostDemon (dark) or PostAngel (light) mode
    val source: String,      // Where it came from ("share" or "create")
    val extraInfo: String = ""  // Additional information in JSON format
) {
    companion object {
        const val SOURCE_SHARE = "share"
        const val SOURCE_CREATE = "create"
    }
}
