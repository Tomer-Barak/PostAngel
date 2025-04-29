package com.postmuse.screenshare

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class SettingsActivity : AppCompatActivity() {        private lateinit var serverUrlEditText: EditText
    private lateinit var secureApiKeyEditText: SecureApiKeyEditText
    private lateinit var saveButton: Button
    private lateinit var privacyPolicyLink: TextView
    private lateinit var knowledgeBaseButton: Button
    private lateinit var apiSecurityInfoButton: ImageButton
    private lateinit var learnMoreLink: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)        // Initialize UI elements
        serverUrlEditText = findViewById(R.id.serverUrlEditText)
        
        // Get the secure API key card view
        val apiKeyCardView = findViewById<View>(R.id.apiKeySecurityCard)
        secureApiKeyEditText = apiKeyCardView.findViewById(R.id.secureApiKeyEditText)
        secureApiKeyEditText.setHint(getString(R.string.enter_openai_api_key))
        
        // Get security info button inside the card
        apiSecurityInfoButton = apiKeyCardView.findViewById(R.id.apiSecurityInfoButton)
        learnMoreLink = apiKeyCardView.findViewById(R.id.learnMoreLink)
        
        saveButton = findViewById(R.id.saveButton)
          // Try to find privacy policy link if it exists
        try {
            privacyPolicyLink = findViewById(R.id.privacyPolicyLink)
            privacyPolicyLink.setOnClickListener {
                startActivity(Intent(this, PrivacyPolicyActivity::class.java))
            }
        } catch (e: Exception) {
            // Privacy policy link might not be in layout yet
        }
          // Set up knowledge base button
        knowledgeBaseButton = findViewById(R.id.knowledgeBaseButton)
        knowledgeBaseButton.setOnClickListener {
            startActivity(Intent(this, KnowledgeBaseActivity::class.java))
        }
          // Set up API security info button
        apiSecurityInfoButton.setOnClickListener {
            ApiKeySecurityDialog.show(this)
        }
        
        // Set up learn more link
        learnMoreLink.setOnClickListener {
            ApiKeySecurityDialog.show(this)
        }
        
        // Load current settings
        loadSettings()
        
        // Set up save button click listener
        saveButton.setOnClickListener {
            saveSettings()
        }
    }    private fun loadSettings() {
        // Load the settings from preferences
        val serverUrl = PrefsUtil.getServerUrl(this)
        val apiKey = SecureKeyStore.getOpenAIApiKey(this)
        serverUrlEditText.setText(serverUrl)
        secureApiKeyEditText.setText(apiKey)
    }private fun saveSettings() {
        val serverUrl = serverUrlEditText.text.toString().trim()
        val apiKey = secureApiKeyEditText.getText().trim()
        
        // Validate URL (basic validation)
        if (serverUrl.isEmpty()) {
            Toast.makeText(this, getString(R.string.server_url_required), Toast.LENGTH_SHORT).show()
            return
        }
        
        // Validate API key (basic validation)
        if (apiKey.isEmpty()) {
            Toast.makeText(this, getString(R.string.api_key_required), Toast.LENGTH_SHORT).show()
            return
        }
        
        // *** Added: Ensure Topics directory exists ***
        val topicsDir = File(filesDir, "Topics")
        if (!topicsDir.exists()) {
            if (topicsDir.mkdirs()) {
                android.util.Log.i("SettingsActivity", "Topics directory created successfully.")
            } else {
                android.util.Log.e("SettingsActivity", "Failed to create Topics directory.")
                // Optionally show a toast to the user
                Toast.makeText(this, "Could not create Topics directory", Toast.LENGTH_SHORT).show()
            }
        }
        // *** End Added ***
          // Save the settings
        PrefsUtil.setServerUrl(this, serverUrl)
        SecureKeyStore.setOpenAIApiKey(this, apiKey)
        Toast.makeText(this, getString(R.string.settings_saved), Toast.LENGTH_SHORT).show()
        finish()
    }
}
