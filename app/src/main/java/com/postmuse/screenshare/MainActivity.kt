package com.postmuse.screenshare

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import java.io.File

class MainActivity : AppCompatActivity() {
    
    private lateinit var settingsButton: Button
    private lateinit var privacyButton: Button
    private lateinit var knowledgeBaseButton: Button
    
    companion object {
        private const val TAG = "MainActivity"
        const val TOPICS_DIR_NAME = "Topics" // Same as in ShareReceiverActivity
    }    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
          // Ensure Topics directory exists
        ensureTopicsDirectoryExists()
        // Initialize UI elements
        settingsButton = findViewById(R.id.settingsButton)
        
        // Set up click listener for settings button
        settingsButton.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
          // Add privacy policy button if it exists in the layout
        try {
            privacyButton = findViewById(R.id.privacyButton)
            privacyButton.setOnClickListener {
                val intent = Intent(this, PrivacyPolicyActivity::class.java)
                startActivity(intent)
            }
        } catch (e: Exception) {
            // Privacy button might not be in the layout yet
        }
        
        // Add knowledge base button
        try {
            knowledgeBaseButton = findViewById(R.id.knowledgeBaseButton)
            knowledgeBaseButton.setOnClickListener {
                val intent = Intent(this, KnowledgeBaseActivity::class.java)
                startActivity(intent)
            }
        } catch (e: Exception) {
            // Knowledge base button might not be in the layout yet
        }
          // Main activity is just informational
        // The main functionality is in ShareReceiverActivity
    }
    
    /**
     * Ensures that the Topics directory exists in the app's internal storage.
     * This directory stores text files that make up the knowledge base.
     */
    private fun ensureTopicsDirectoryExists() {
        val topicsDir = File(filesDir, TOPICS_DIR_NAME)
        if (!topicsDir.exists()) {
            if (topicsDir.mkdirs()) {
                Log.i(TAG, "Topics directory created successfully at ${topicsDir.absolutePath}")
                // Create a sample topic file if this is first run
                createSampleTopic(topicsDir)
            } else {
                Log.e(TAG, "Failed to create Topics directory at ${topicsDir.absolutePath}")
            }
        } else {
            Log.i(TAG, "Topics directory already exists at ${topicsDir.absolutePath}")
        }
    }
    
    /**
     * Creates a sample topic file to help users understand how the knowledge base works.
     */
    private fun createSampleTopic(topicsDir: File) {
        try {
            val sampleFile = File(topicsDir, "Sample Topic.txt")
            if (!sampleFile.exists()) {
                sampleFile.writeText(
                    """
                    This is a sample topic file.
                    
                    In your topic files, you should include information about products,
                    services, or ideas that you want to promote when relevant opportunities
                    arise on social media.
                    
                    The app will analyze social media posts you share and suggest responses
                    based on the content in these topic files when there's a relevant match.
                    
                    You can edit this file or create new ones using the Knowledge Base manager.
                    """.trimIndent()
                )
                Log.i(TAG, "Created sample topic file at ${sampleFile.absolutePath}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating sample topic", e)
        }
    }
}
