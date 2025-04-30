package com.postmuse.screenshare

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import java.io.File

class MainActivity : AppCompatActivity() {
      private lateinit var titleTextView: TextView
    private lateinit var descriptionTextView: TextView
    private lateinit var createPostDescriptionTextView: TextView
    private lateinit var topicsDescriptionTextView: TextView
    private lateinit var apiKeyDescriptionTextView: TextView
    
    private lateinit var createPostButton: Button
    private lateinit var settingsButton: Button
    private lateinit var knowledgeBaseButton: Button
    private lateinit var privacyButton: Button
    
    companion object {
        private const val TAG = "MainActivity"
        const val TOPICS_DIR_NAME = "Topics" // Same as in ShareReceiverActivity
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Ensure Topics directory exists
        ensureTopicsDirectoryExists()
        
        // Initialize UI elements
        initializeViews()
        setupClickListeners()
    }
    
    /**
     * Initialize all UI elements from the layout
     */
    private fun initializeViews() {        // TextViews
        titleTextView = findViewById(R.id.titleTextView)
        descriptionTextView = findViewById(R.id.descriptionTextView)
        createPostDescriptionTextView = findViewById(R.id.createPostDescriptionTextView)
        topicsDescriptionTextView = findViewById(R.id.topicsDescriptionTextView)
        apiKeyDescriptionTextView = findViewById(R.id.apiKeyDescriptionTextView)
        
        // Buttons
        createPostButton = findViewById(R.id.createPostButton)
        settingsButton = findViewById(R.id.settingsButton)
        knowledgeBaseButton = findViewById(R.id.knowledgeBaseButton)
        
        // Privacy button might not be in all layout versions
        try {
            privacyButton = findViewById(R.id.privacyButton)
        } catch (e: Exception) {
            Log.d(TAG, "Privacy button not found in layout", e)
        }
    }
    
    /**
     * Set up click listeners for all buttons
     */
    private fun setupClickListeners() {
        // Create Post button
        createPostButton.setOnClickListener {
            Log.d(TAG, "Create Post button clicked")
            val intent = Intent(this, CreatePostActivity::class.java)
            startActivity(intent)
        }
        
        // Settings button
        settingsButton.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
        
        // Knowledge Base button
        knowledgeBaseButton.setOnClickListener {
            val intent = Intent(this, KnowledgeBaseActivity::class.java)
            startActivity(intent)
        }
        
        // Privacy button (if available)
        try {
            privacyButton.setOnClickListener {
                val intent = Intent(this, PrivacyPolicyActivity::class.java)
                startActivity(intent)
            }
        } catch (e: Exception) {
            Log.d(TAG, "Privacy button click listener not set", e)
        }
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
