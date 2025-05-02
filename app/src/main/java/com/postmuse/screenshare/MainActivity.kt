package com.postangel.screenshare

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
    private lateinit var historyDescriptionTextView: TextView
    private lateinit var createPostButton: Button
    private lateinit var settingsButton: Button
    private lateinit var knowledgeBaseButton: Button
    private lateinit var historyButton: Button
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
        
        // Update UI based on current mode (light/dark)
        updateAppearanceForCurrentMode()
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
        historyDescriptionTextView = findViewById(R.id.historyDescriptionTextView)
          // Buttons
        createPostButton = findViewById(R.id.createPostButton)
        settingsButton = findViewById(R.id.settingsButton)
        knowledgeBaseButton = findViewById(R.id.knowledgeBaseButton)
        historyButton = findViewById(R.id.historyButton)
        
        // Privacy button might not be in all layout versions
        try {
            privacyButton = findViewById(R.id.privacyButton)
        } catch (e: Exception) {
            Log.d(TAG, "Privacy button not found in layout", e)
        }
    }
    
    /**
     * Updates the UI based on whether app is in dark mode (PostDemon) or light mode (PostAngel)
     */    private fun updateAppearanceForCurrentMode() {
        val appName = ModeHelper.getAppName(this)
        titleTextView.text = appName
        
        // Update description text based on the mode
        if (ModeHelper.isDarkModeActive(this)) {
            // Dark mode - PostDemon
            descriptionTextView.text = "Take a screenshot of a social media post, share it to PostDemon, and get AI-generated sarcastic responses that contradict the topics with clever wit."
            historyDescriptionTextView.text = "View your history of sarcastic responses and witty contradictions in one place."
        } else {
            // Light mode - PostAngel
            descriptionTextView.text = "Take a screenshot of a social media post, share it to PostAngel, and get AI-generated responses that promote your topics."
            historyDescriptionTextView.text = "View all your previously generated promotional posts and responses in one place."
        }
    }
    
    /**
     * Called when returning to this activity - update UI in case dark mode changed
     */
    override fun onResume() {
        super.onResume()
        updateAppearanceForCurrentMode()
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
        
        // History button
        historyButton.setOnClickListener {
            val intent = Intent(this, PostHistoryActivity::class.java)
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
                // createSampleTopic(topicsDir)
            } else {
                Log.e(TAG, "Failed to create Topics directory at ${topicsDir.absolutePath}")
            }
        } else {
            Log.i(TAG, "Topics directory already exists at ${topicsDir.absolutePath}")
        }
    }
}
