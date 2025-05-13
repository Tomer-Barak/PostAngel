package com.postangel.screenshare

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

/**
 * Helper class for handling app mode changes between PostAngel (light mode) and PostDemon (dark mode)
 * and platform-specific content (LinkedIn vs X)
 */
object ModeHelper {
    /**
     * Check if the app is currently in dark mode (PostDemon mode)
     * Takes into account temporary mode settings
     */
    fun isDarkModeActive(context: Context): Boolean {
        return if (TemporaryModeManager.isUsingTemporaryMode()) {
            TemporaryModeManager.getCurrentMode(context)
        } else {
            PrefsUtil.isDarkModeEnabled(context)
        }
    }
    
    /**
     * Get the current social media platform to use
     * Takes into account temporary platform settings
     */
    fun getCurrentPlatform(context: Context): String {
        return PlatformManager.getCurrentPlatform(context)
    }

    /**
     * Check if the current platform is LinkedIn
     */
    fun isLinkedInPlatform(context: Context): Boolean {
        return getCurrentPlatform(context) == PrefsUtil.PLATFORM_LINKEDIN
    }

    /**
     * Get the appropriate app name based on the current mode
     */
    fun getAppName(context: Context): String {
        return if (isDarkModeActive(context)) "PostDemon" else "PostAngel"
    }
    
    /**
     * Get platform-specific text for UI elements
     */
    fun getPlatformName(context: Context): String {
        return if (isLinkedInPlatform(context)) "LinkedIn" else "X"
    }

    /**
     * Get the appropriate system prompt for creating posts based on current mode and platform
     */
    fun getPostGenerationSystemPrompt(context: Context): String {
        val isLinkedIn = isLinkedInPlatform(context)
        
        return if (isDarkModeActive(context)) {
            // Dark mode = PostDemon with sarcastic, contradictory tone
            if (isLinkedIn) {
                "You are a sarcastic, slightly cynical social media assistant creating LinkedIn posts. Your posts should be clever, slightly contradictory, use dark humor, but still maintain a professional tone appropriate for LinkedIn."
            } else {
                "You are a sarcastic, slightly cynical social media assistant creating posts for X. Your posts should be clever, slightly contradictory, and use dark humor. Be concise and punchy."
            }
        } else {
            // Light mode = Standard PostAngel
            if (isLinkedIn) {
                "You are a social media assistant creating promotional LinkedIn posts. Your posts should be professional, thoughtful, and optimized for a business audience."
            } else {
                "You are a social media assistant creating promotional posts for X. Your posts should be concise, engaging, and optimized for virality."
            }
        }
    }
    
    /**
     * Get the appropriate content for post generation prompt based on current mode and platform
     */
    fun modifyPostGenerationPrompt(context: Context, basePrompt: String): String {
        val isLinkedIn = isLinkedInPlatform(context)
        val characterLimit = if (isLinkedIn) "3000" else "280"
        
        // First modify based on platform
        var modifiedPrompt = basePrompt
            .replace("Ensure the post is under 280 characters!", "Ensure the post is under $characterLimit characters!")
        
        if (isLinkedIn) {
            modifiedPrompt = modifiedPrompt
                .replace("Create an engaging, informative post", "Create a professional, informative LinkedIn post")
                .replace("Include relevant hashtags if appropriate", "Include relevant hashtags and possibly a call to action")
        } else { // X
            modifiedPrompt = modifiedPrompt
                .replace("Create an engaging, informative post", "Create a concise, catchy post for X (formerly Twitter)")
                .replace("Include relevant hashtags if appropriate", "Include 1-2 relevant hashtags to increase visibility")
        }
        
        // Then apply dark/light mode modifications
        return if (isDarkModeActive(context)) {
            // Modify prompt for dark mode
            modifiedPrompt.replace(
                "The promotion should be subtle and thoughtful",
                "The post should be sarcastic and subversive"
            ).replace(
                "The post should be conversational and personable",
                "The post should be witty and mildly cynical"
            )
        } else {
            // Keep the modified prompt for light mode
            modifiedPrompt
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
    }    /**
     * Get the appropriate system prompt for response generation based on current mode and platform
     */
    fun getResponseGenerationSystemPrompt(context: Context): String {
        val isLinkedIn = isLinkedInPlatform(context)
        
        return if (isDarkModeActive(context)) {
            // Dark mode = PostDemon with sarcastic, contradictory tone
            if (isLinkedIn) {
                "You are a witty assistant that drafts sarcastic and cleverly contradictory LinkedIn replies. Use the provided context but frame your response with dark humor and a touch of cynicism. FORMAT FOR LINKEDIN: Your response should be substantially more detailed (300-800 characters), professional in tone despite the sarcasm, and should include thoughtful observations with layers of irony. Maintain the cynical tone but with a corporate-appropriate veneer that subtly mocks business jargon and LinkedIn culture."
            } else {
                "You are a witty assistant that drafts sarcastic and cleverly contradictory replies for X (Twitter). Use the provided context but frame your response with dark humor and a touch of cynicism. FORMAT FOR X: Keep responses under 280 characters, punchy, with sharp wit and no corporate speak."
            }
        } else {
            // Light mode = Standard PostAngel
            if (isLinkedIn) {
                "You are a helpful assistant that drafts professional and relevant LinkedIn replies. Use the provided context to respond to the original post content. FORMAT FOR LINKEDIN: Your response should be more detailed (150-500 characters), professional in tone, and could include thoughtful insights or questions. Add value to the conversation in a business-appropriate way."
            } else {
                "You are a helpful assistant that drafts concise and relevant replies for X (Twitter). Use the provided context to respond to the original post content. FORMAT FOR X: Keep responses under 280 characters, engaging, and to the point."
            }
        }
    }
}
