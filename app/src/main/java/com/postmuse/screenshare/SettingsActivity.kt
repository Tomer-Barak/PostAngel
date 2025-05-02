package com.postangel.screenshare

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import java.io.File

class SettingsActivity : AppCompatActivity() {          private lateinit var saveButton: Button
    private lateinit var privacyPolicyLink: TextView
    private lateinit var apiSecurityInfoButton: ImageButton
    private lateinit var learnMoreLink: TextView
    private lateinit var darkModeSwitch: SwitchCompat
    
    // Global API settings
    private lateinit var globalApiKeyEditText: SecureApiKeyEditText
    private lateinit var globalApiUrlEditText: EditText
    private lateinit var useGlobalApiKeySwitch: SwitchCompat
    
    // Model settings
    private lateinit var visionModelEditText: EditText
    private lateinit var responseModelEditText: EditText
    private lateinit var postGenerationModelEditText: EditText
    
    // Custom API settings - Vision
    private lateinit var visionCustomSettingsLayout: View
    private lateinit var visionApiUrlEditText: EditText
    private lateinit var visionApiKeyEditText: SecureApiKeyEditText
    private lateinit var toggleVisionCustomSettingsButton: Button
    
    // Custom API settings - Response
    private lateinit var responseCustomSettingsLayout: View
    private lateinit var responseApiUrlEditText: EditText
    private lateinit var responseApiKeyEditText: SecureApiKeyEditText
    private lateinit var toggleResponseCustomSettingsButton: Button
    
    // Custom API settings - Post Generation
    private lateinit var postGenerationCustomSettingsLayout: View
    private lateinit var postGenerationApiUrlEditText: EditText
    private lateinit var postGenerationApiKeyEditText: SecureApiKeyEditText
    private lateinit var togglePostGenerationCustomSettingsButton: Button
    
    private lateinit var resetModelsButton: Button
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)          // Get the model settings card view
        val modelSettingsCard = findViewById<View>(R.id.modelSettingsCard)
        
        // Get security info button and learn more link inside the model card
        apiSecurityInfoButton = modelSettingsCard.findViewById(R.id.apiSecurityInfoButton)
        learnMoreLink = modelSettingsCard.findViewById(R.id.learnMoreLink)
          // Model settings card is already initialized above
          // Global API settings
        globalApiKeyEditText = modelSettingsCard.findViewById(R.id.globalApiKeyEditText)
        globalApiUrlEditText = modelSettingsCard.findViewById(R.id.globalApiUrlEditText)
        useGlobalApiKeySwitch = modelSettingsCard.findViewById(R.id.useGlobalApiKeySwitch)
        
        // Initialize models section
        modelsSection = modelSettingsCard.findViewById(R.id.modelsSection)
        
        // Model fields
        visionModelEditText = modelSettingsCard.findViewById(R.id.visionModelEditText)
        responseModelEditText = modelSettingsCard.findViewById(R.id.responseModelEditText)
        postGenerationModelEditText = modelSettingsCard.findViewById(R.id.postGenerationModelEditText)
        
        // Vision custom API settings
        visionCustomSettingsLayout = modelSettingsCard.findViewById(R.id.visionCustomSettingsLayout)
        visionApiUrlEditText = modelSettingsCard.findViewById(R.id.visionApiUrlEditText)
        visionApiKeyEditText = modelSettingsCard.findViewById(R.id.visionApiKeyEditText)
        toggleVisionCustomSettingsButton = modelSettingsCard.findViewById(R.id.toggleVisionCustomSettingsButton)
        
        // Response custom API settings
        responseCustomSettingsLayout = modelSettingsCard.findViewById(R.id.responseCustomSettingsLayout)
        responseApiUrlEditText = modelSettingsCard.findViewById(R.id.responseApiUrlEditText)
        responseApiKeyEditText = modelSettingsCard.findViewById(R.id.responseApiKeyEditText)
        toggleResponseCustomSettingsButton = modelSettingsCard.findViewById(R.id.toggleResponseCustomSettingsButton)
        
        // Post generation custom API settings
        postGenerationCustomSettingsLayout = modelSettingsCard.findViewById(R.id.postGenerationCustomSettingsLayout)
        postGenerationApiUrlEditText = modelSettingsCard.findViewById(R.id.postGenerationApiUrlEditText)
        postGenerationApiKeyEditText = modelSettingsCard.findViewById(R.id.postGenerationApiKeyEditText)
        togglePostGenerationCustomSettingsButton = modelSettingsCard.findViewById(R.id.togglePostGenerationCustomSettingsButton)
        
        resetModelsButton = modelSettingsCard.findViewById(R.id.resetModelsButton)
        
        // Set up toggle buttons for custom API settings
        setupCustomSettingsToggles()
          // Set up custom API keys switch
        useGlobalApiKeySwitch.setOnCheckedChangeListener { _, isChecked ->
            updateApiSettingsVisibility(!isChecked) // Invert logic - false means use global API keys
        }
        
        // Set up reset button click listener
        resetModelsButton.setOnClickListener {
            resetToDefaults()
            Toast.makeText(this, R.string.models_reset, Toast.LENGTH_SHORT).show()
        }
        
        // Initialize dark mode switch
        darkModeSwitch = findViewById(R.id.darkModeSwitch)
        
        saveButton = findViewById(R.id.saveButton)
          // Try to find privacy policy link if it exists
        try {
            privacyPolicyLink = findViewById(R.id.privacyPolicyLink)
            privacyPolicyLink.setOnClickListener {
                startActivity(Intent(this, PrivacyPolicyActivity::class.java))
            }
        } catch (e: Exception) {
            // Privacy policy link might not be in layout yet
        }          // Set up API security info button
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
        val isDarkModeEnabled = PrefsUtil.isDarkModeEnabled(this)
        val useGlobalApiKey = PrefsUtil.isUsingGlobalApiKey(this)
        
        // Load API and model settings
        val globalApiUrl = PrefsUtil.getServerUrl(this)
        val globalApiKey = SecureKeyStore.getGlobalApiKey(this)
        val visionModel = PrefsUtil.getVisionModel(this)
        val responseModel = PrefsUtil.getResponseModel(this)
        val postGenerationModel = PrefsUtil.getPostGenerationModel(this)
        
        // Load custom API settings
        val visionApiUrl = PrefsUtil.getVisionApiUrl(this)
        val visionApiKey = SecureKeyStore.getVisionApiKey(this, false)
        val responseApiUrl = PrefsUtil.getResponseApiUrl(this)
        val responseApiKey = SecureKeyStore.getResponseApiKey(this, false)
        val postGenerationApiUrl = PrefsUtil.getPostGenerationApiUrl(this)
        val postGenerationApiKey = SecureKeyStore.getPostGenerationApiKey(this, false)
        
        // Set texts in edit fields
        darkModeSwitch.isChecked = isDarkModeEnabled
        
        // Global settings
        globalApiUrlEditText.setText(globalApiUrl)
        globalApiKeyEditText.setText(globalApiKey)
        useGlobalApiKeySwitch.isChecked = !useGlobalApiKey // Invert the logic for the switch
        
        // Model names
        visionModelEditText.setText(visionModel)
        responseModelEditText.setText(responseModel)
        postGenerationModelEditText.setText(postGenerationModel)
        
        // Custom API settings
        visionApiUrlEditText.setText(visionApiUrl)
        visionApiKeyEditText.setText(visionApiKey)
        responseApiUrlEditText.setText(responseApiUrl)
        responseApiKeyEditText.setText(responseApiKey)
        postGenerationApiUrlEditText.setText(postGenerationApiUrl)
        postGenerationApiKeyEditText.setText(postGenerationApiKey)
        
        // Update custom settings visibility based on the global API key setting
        updateApiSettingsVisibility(useGlobalApiKey)
        
        // Set up dark mode switch listener
        darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            applyDarkMode(isChecked)
        }
    }
      private fun applyDarkMode(enabled: Boolean) {
        PrefsUtil.setDarkModeEnabled(this, enabled)
        
        // Apply the appropriate night mode
        if (enabled) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            Toast.makeText(this, "PostDemon mode activated", Toast.LENGTH_SHORT).show()
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            Toast.makeText(this, "PostAngel mode activated", Toast.LENGTH_SHORT).show()
        }
        
        // We need to recreate the activity to apply the theme changes fully
        recreate()
    }    private fun saveSettings() {
        val useCustomApiKeys = useGlobalApiKeySwitch.isChecked
        val useGlobalApiKey = !useCustomApiKeys // Invert the logic from the switch
        
        // Get global settings
        val globalApiUrl = globalApiUrlEditText.text.toString().trim()
        val globalApiKey = globalApiKeyEditText.getText().trim()
        
        // Get model names
        val visionModel = visionModelEditText.text.toString().trim()
        val responseModel = responseModelEditText.text.toString().trim()
        val postGenerationModel = postGenerationModelEditText.text.toString().trim()
        
        // Get custom API settings
        val visionApiUrl = visionApiUrlEditText.text.toString().trim()
        val visionApiKey = visionApiKeyEditText.getText().trim()
        val responseApiUrl = responseApiUrlEditText.text.toString().trim()
        val responseApiKey = responseApiKeyEditText.getText().trim()
        val postGenerationApiUrl = postGenerationApiUrlEditText.text.toString().trim()
        val postGenerationApiKey = postGenerationApiKeyEditText.getText().trim()
        
        // Validate API keys (basic validation)
        if (useGlobalApiKey && globalApiKey.isEmpty()) {
            Toast.makeText(this, getString(R.string.global_api_key_required), Toast.LENGTH_SHORT).show()
            return
        }
        
        // Validate global API URL
        if (globalApiUrl.isEmpty()) {
            globalApiUrlEditText.setText(getString(R.string.default_api_url))
        }
        
        // Validate models - set to defaults if empty
        if (visionModel.isEmpty()) {
            visionModelEditText.setText(getString(R.string.default_vision_model))
        }
        
        if (responseModel.isEmpty()) {
            responseModelEditText.setText(getString(R.string.default_response_model))
        }
        
        if (postGenerationModel.isEmpty()) {
            postGenerationModelEditText.setText(getString(R.string.default_post_generation_model))
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
        }        // *** End Added ***        
          // Set global key as the legacy API key for backward compatibility
        SecureKeyStore.setOpenAIApiKey(this, globalApiKey)
        
        // Save dark mode setting
        PrefsUtil.setDarkModeEnabled(this, darkModeSwitch.isChecked)
        
        // Save global API settings
        PrefsUtil.setServerUrl(this, globalApiUrl)
        SecureKeyStore.setGlobalApiKey(this, globalApiKey)
        PrefsUtil.setUseGlobalApiKey(this, useGlobalApiKey)
        
        // Save model names
        PrefsUtil.setVisionModel(this, visionModel)
        PrefsUtil.setResponseModel(this, responseModel)
        PrefsUtil.setPostGenerationModel(this, postGenerationModel)
        
        // Save custom API settings if not using global
        if (!useGlobalApiKey) {
            PrefsUtil.setVisionApiUrl(this, visionApiUrl)
            SecureKeyStore.setVisionApiKey(this, visionApiKey)
            
            PrefsUtil.setResponseApiUrl(this, responseApiUrl)
            SecureKeyStore.setResponseApiKey(this, responseApiKey)
            
            PrefsUtil.setPostGenerationApiUrl(this, postGenerationApiUrl)
            SecureKeyStore.setPostGenerationApiKey(this, postGenerationApiKey)
        }
        
        Toast.makeText(this, getString(R.string.settings_saved), Toast.LENGTH_SHORT).show()
        finish()
    }
    
    private fun setupCustomSettingsToggles() {
        // Vision custom settings toggle
        toggleVisionCustomSettingsButton.setOnClickListener {
            val isVisible = visionCustomSettingsLayout.visibility == View.VISIBLE
            visionCustomSettingsLayout.visibility = if (isVisible) View.GONE else View.VISIBLE
            toggleVisionCustomSettingsButton.text = getString(
                if (isVisible) R.string.customize_api_settings 
                else R.string.hide_custom_settings
            )
        }
        
        // Response custom settings toggle
        toggleResponseCustomSettingsButton.setOnClickListener {
            val isVisible = responseCustomSettingsLayout.visibility == View.VISIBLE
            responseCustomSettingsLayout.visibility = if (isVisible) View.GONE else View.VISIBLE
            toggleResponseCustomSettingsButton.text = getString(
                if (isVisible) R.string.customize_api_settings 
                else R.string.hide_custom_settings
            )
        }
        
        // Post generation custom settings toggle
        togglePostGenerationCustomSettingsButton.setOnClickListener {
            val isVisible = postGenerationCustomSettingsLayout.visibility == View.VISIBLE
            postGenerationCustomSettingsLayout.visibility = if (isVisible) View.GONE else View.VISIBLE
            togglePostGenerationCustomSettingsButton.text = getString(
                if (isVisible) R.string.customize_api_settings 
                else R.string.hide_custom_settings
            )
        }
    }      private lateinit var modelsSection: View
    
    private fun updateApiSettingsVisibility(useGlobalSetting: Boolean) {
        // Get the models section if not already initialized
        if (!::modelsSection.isInitialized) {
            val modelSettingsCard = findViewById<View>(R.id.modelSettingsCard)
            modelsSection = modelSettingsCard.findViewById(R.id.modelsSection)
        }
        
        // Set models section visibility based on custom API keys setting
        modelsSection.visibility = if (useGlobalSetting) View.GONE else View.VISIBLE
        
        // If custom settings are currently visible, hide them first
        if (visionCustomSettingsLayout.visibility == View.VISIBLE) {
            visionCustomSettingsLayout.visibility = View.GONE
            toggleVisionCustomSettingsButton.text = getString(R.string.customize_api_settings)
        }
        
        if (responseCustomSettingsLayout.visibility == View.VISIBLE) {
            responseCustomSettingsLayout.visibility = View.GONE
            toggleResponseCustomSettingsButton.text = getString(R.string.customize_api_settings)
        }
        
        if (postGenerationCustomSettingsLayout.visibility == View.VISIBLE) {
            postGenerationCustomSettingsLayout.visibility = View.GONE
            togglePostGenerationCustomSettingsButton.text = getString(R.string.customize_api_settings)
        }
        
        // Update toggles enabled state
        toggleVisionCustomSettingsButton.isEnabled = !useGlobalSetting
        toggleResponseCustomSettingsButton.isEnabled = !useGlobalSetting
        togglePostGenerationCustomSettingsButton.isEnabled = !useGlobalSetting
          // When using global API key, hide all custom settings
        // but keep the toggle buttons visible
        
        // Update toggle button states
        if (useGlobalSetting) {
            // If using global API key, hide any expanded custom settings
            visionCustomSettingsLayout.visibility = View.GONE
            responseCustomSettingsLayout.visibility = View.GONE
            postGenerationCustomSettingsLayout.visibility = View.GONE
            
            // Reset toggle button texts
            toggleVisionCustomSettingsButton.text = getString(R.string.customize_api_settings)
            toggleResponseCustomSettingsButton.text = getString(R.string.customize_api_settings)
            togglePostGenerationCustomSettingsButton.text = getString(R.string.customize_api_settings)
            
            // When using global API key, disable the toggle buttons
            toggleVisionCustomSettingsButton.isEnabled = false
            toggleResponseCustomSettingsButton.isEnabled = false
            togglePostGenerationCustomSettingsButton.isEnabled = false
        } else {
            // When not using global API key, enable the toggle buttons
            toggleVisionCustomSettingsButton.isEnabled = true
            toggleResponseCustomSettingsButton.isEnabled = true
            togglePostGenerationCustomSettingsButton.isEnabled = true
        }
    }
    
    private fun resetToDefaults() {
        // Reset model names to defaults
        visionModelEditText.setText(getString(R.string.default_vision_model))
        responseModelEditText.setText(getString(R.string.default_response_model))
        postGenerationModelEditText.setText(getString(R.string.default_post_generation_model))
          // Reset global API URL (keep API key as is)
        globalApiUrlEditText.setText(getString(R.string.default_api_url))
        
        // Reset to use global API key (switch OFF means use global keys)
        // First update the UI state, then set the switch without triggering the listener
        updateApiSettingsVisibility(true)
        useGlobalApiKeySwitch.setOnCheckedChangeListener(null)
        useGlobalApiKeySwitch.isChecked = false
        // Restore the listener
        useGlobalApiKeySwitch.setOnCheckedChangeListener { _, isChecked ->
            updateApiSettingsVisibility(!isChecked)
        }
        
        // Clear custom API settings
        visionApiUrlEditText.setText("")
        visionApiKeyEditText.setText("")
        responseApiUrlEditText.setText("")
        responseApiKeyEditText.setText("")
        postGenerationApiUrlEditText.setText("")
        postGenerationApiKeyEditText.setText("")
    }
}
