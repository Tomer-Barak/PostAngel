package com.postangel.screenshare

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

/**
 * Helper class for handling app mode changes between PostAngel (light mode) and PostDemon (dark mode)
 */
object ModeHelper {

    /**
     * Check if the app is currently in dark mode (PostDemon mode)
     */
    fun isDarkModeActive(context: Context): Boolean {
        return PrefsUtil.isDarkModeEnabled(context)
    }

    /**
     * Get the appropriate app name based on the current mode
     */
    fun getAppName(context: Context): String {
        return if (isDarkModeActive(context)) "PostDemon" else "PostAngel"
    }

    /**
     * Get the appropriate system prompt for creating posts based on current mode
     */
    fun getPostGenerationSystemPrompt(context: Context): String {
        return if (isDarkModeActive(context)) {
            // Dark mode = PostDemon with sarcastic, contradictory tone
            "You are a sarcastic, slightly cynical social media assistant creating posts. Your posts should be clever, slightly contradictory, and use dark humor."
        } else {
            // Light mode = Standard PostAngel
            "You are a social media assistant creating promotional posts."
        }
    }
    
    /**
     * Get the appropriate content for post generation prompt based on current mode
     */
    fun modifyPostGenerationPrompt(context: Context, basePrompt: String): String {
        return if (isDarkModeActive(context)) {
            // Modify prompt for dark mode
            basePrompt.replace(
                "The promotion should be subtle and thoughtful",
                "The post should be sarcastic and subversive"
            ).replace(
                "The post should be conversational and personable",
                "The post should be witty and mildly cynical"
            )
        } else {
            // Keep the original prompt for light mode
            basePrompt
        }
    }
    
    /**
     * Get the appropriate system prompt for analyzing social media posts based on current mode
     */
    fun getAnalysisSystemPrompt(context: Context): String {
        return if (isDarkModeActive(context)) {
            // Dark mode = PostDemon with sarcastic, contradictory tone
            "You are a social media assistant analyzing posts for opportunities to respond with a sarcastic, cleverly contradictory tone."
        } else {
            // Light mode = Standard PostAngel
            "You are a social media assistant analyzing posts for promotional opportunities."
        }
    }
    
    /**
     * Modify the analysis instructions based on current mode
     */
    fun modifyAnalysisInstructions(context: Context, instructions: String): String {
        return if (isDarkModeActive(context)) {
            instructions.replace(
                "The response should be respectful and professional",
                "The response should be witty, sarcastic, and subtly contrary while still being appropriate"
            )
        } else {
            instructions
        }
    }
    
    /**
     * Get the appropriate system prompt for response generation based on current mode
     */
    fun getResponseGenerationSystemPrompt(context: Context): String {
        return if (isDarkModeActive(context)) {
            // Dark mode = PostDemon with sarcastic, contradictory tone
            "You are a witty assistant that drafts sarcastic and cleverly contradictory social media replies. Use the provided context but frame your response with dark humor and a touch of cynicism."
        } else {
            // Light mode = Standard PostAngel
            "You are a helpful assistant that drafts concise and relevant social media replies. Use the provided context to respond to the original post content."
        }
    }
}
