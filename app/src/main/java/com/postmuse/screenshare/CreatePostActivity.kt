package com.postangel.screenshare

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.IOException
import com.postangel.screenshare.ModeHelper
import com.postangel.screenshare.PostHistoryEntry
import com.postangel.screenshare.PostHistoryManager
import com.postangel.screenshare.TemporaryModeManager

class CreatePostActivity : AppCompatActivity() {      
    private lateinit var topicSpinner: Spinner
    private lateinit var specialInstructionsEditText: EditText
    private lateinit var generateButton: Button
    private lateinit var generatedPostView: TextView
    private lateinit var copyButton: Button
    private lateinit var toggleModeButton: Button
    private lateinit var toggleButtonsLayout: LinearLayout
    private lateinit var platformRadioGroup: RadioGroup
    private lateinit var radioX: RadioButton
    private lateinit var radioLinkedIn: RadioButton    
    private val TAG = "CreatePostActivity"
    
    // Cache the last topic, instructions and generated post for toggle features
    private var lastSelectedTopic: String? = null
    private var lastSpecialInstructions: String = ""
    private var usingAlternateMode = false

    companion object {
        const val TOPICS_DIR_NAME = "Topics" // Directory for topics
    }    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_post)        // Initialize views
        topicSpinner = findViewById(R.id.topicSpinner)
        specialInstructionsEditText = findViewById(R.id.specialInstructionsEditText)        
        generateButton = findViewById(R.id.generatePostButton)
        generatedPostView = findViewById(R.id.generatedPostTextView)
        copyButton = findViewById(R.id.copyPostButton)
        toggleModeButton = findViewById(R.id.toggleModeButton)
        toggleButtonsLayout = findViewById(R.id.toggleButtonsLayout)
        platformRadioGroup = findViewById(R.id.platformRadioGroup)
        radioX = findViewById(R.id.radioX)
        radioLinkedIn = findViewById(R.id.radioLinkedIn)

        // Initially hide buttons
        copyButton.visibility = View.GONE
        toggleButtonsLayout.visibility = View.GONE
        
        // Set the title based on current mode
        title = if (ModeHelper.isDarkModeActive(this)) {
            getString(R.string.mode_demon)
        } else {
            getString(R.string.mode_angel)
        }
        
        // Set initial platform radio button selection based on saved preference
        setInitialPlatformSelection()
          // Initialize UI for current mode
        updateToggleModeButtonText()

        // Load topics into spinner
        loadTopics()// Set up click listeners        
        generateButton.setOnClickListener {
            val selectedTopic = topicSpinner.selectedItem?.toString()
            if (selectedTopic != null) {
                val specialInstructions = specialInstructionsEditText.text.toString().trim()
                
                // Cache the selections for toggle features
                lastSelectedTopic = selectedTopic
                lastSpecialInstructions = specialInstructions
                
                lifecycleScope.launch {
                    generatePost(selectedTopic, specialInstructions)
                }
            }
        }

        copyButton.setOnClickListener {
            // Copy the text from generatedPostView to clipboard
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Generated Post", generatedPostView.text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Post copied to clipboard", Toast.LENGTH_SHORT).show()
        }
          // Set up toggle mode button
        updateToggleModeButtonText()
        toggleModeButton.setOnClickListener {
            togglePostMode()
        }
        
        // Set up platform radio group listener
        platformRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val platform = when (checkedId) {
                R.id.radioLinkedIn -> PrefsUtil.PLATFORM_LINKEDIN
                else -> PrefsUtil.PLATFORM_X
            }
            PrefsUtil.setSocialMediaPlatform(this, platform)
        }
    }

    // Set initial platform radio button selection based on saved preference
    private fun setInitialPlatformSelection() {
        val savedPlatform = PrefsUtil.getSocialMediaPlatform(this)
        if (savedPlatform == PrefsUtil.PLATFORM_LINKEDIN) {
            radioLinkedIn.isChecked = true
        } else {
            radioX.isChecked = true
        }
    }    // Update toggle mode button text based on current mode
    private fun updateToggleModeButtonText() {
        val currentMode = ModeHelper.isDarkModeActive(this)
        if (currentMode) {
            // Currently in dark mode, show option for light mode
            toggleModeButton.text = getString(R.string.analyze_with_angel)
        } else {
            // Currently in light mode, show option for dark mode
            toggleModeButton.text = getString(R.string.analyze_with_demon)
        }
    }
  
    
    // Toggle between PostAngel and PostDemon modes
    private fun togglePostMode() {
        if (lastSelectedTopic == null) {
            // Can't toggle if no post has been generated
            val errorMsg = if (ModeHelper.isDarkModeActive(this)) {
                "Generate a post first, then try switching modes. Don't be lazy."
            } else {
                "Please generate a post first before switching modes."
            }
            Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show()
            return
        }
        
        // Toggle temporary mode
        val isDarkMode = TemporaryModeManager.toggleTemporaryMode(this)
        usingAlternateMode = true
        
        // Update UI elements
        updateToggleModeButtonText()
        setResponseBackgroundForCurrentMode()
        
        // Show loading state
        generateButton.isEnabled = false
        val modeChangeMsg = if (isDarkMode) {
            "Switching to PostDemon mode for sarcastic content..."
        } else {
            "Switching to PostAngel mode for helpful content..."
        }
        generatedPostView.text = modeChangeMsg
        copyButton.visibility = View.GONE
        
        // Regenerate post with the new mode
        lifecycleScope.launch {
            generatePost(lastSelectedTopic!!, lastSpecialInstructions)
        }
    }

    private fun loadTopics() {
        lifecycleScope.launch(Dispatchers.IO) {
            val topics = getTopics()
            withContext(Dispatchers.Main) {
                val adapter = ArrayAdapter(
                    this@CreatePostActivity,
                    android.R.layout.simple_spinner_item,
                    topics.map { it.name }
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                topicSpinner.adapter = adapter
            }
        }
    }

    data class Topic(val name: String, val description: String)

    private fun getTopics(): List<Topic> {
        val topics = mutableListOf<Topic>()
        val topicsDir = File(filesDir, TOPICS_DIR_NAME)

        if (!topicsDir.exists() || !topicsDir.isDirectory) {
            Log.w(TAG, "Topics directory does not exist: ${topicsDir.absolutePath}")
            return emptyList()
        }

        try {
            topicsDir.listFiles { file -> 
                file.isFile && (file.extension.equals("txt", ignoreCase = true) || 
                              file.extension.equals("md", ignoreCase = true))
            }?.forEach { file ->
                try {
                    val content = file.readText()
                    val name = file.nameWithoutExtension
                    topics.add(Topic(name, content))
                } catch (e: IOException) {
                    Log.e(TAG, "Error reading topic file: ${file.name}", e)
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException accessing topics directory", e)
        }

        return topics
    }    private suspend fun generatePost(topic: String, specialInstructions: String = "") {
        withContext(Dispatchers.Main) {
            // Show loading state
            generateButton.isEnabled = false
            generatedPostView.text = "Generating post..."
            copyButton.visibility = View.GONE
            toggleButtonsLayout.visibility = View.GONE // Hide the entire layout instead of just one button
            
            // Apply appropriate background color for the current mode
            setResponseBackgroundForCurrentMode()
            
            // Show mode-specific loading message
            val isDarkMode = ModeHelper.isDarkModeActive(this@CreatePostActivity)
            generatedPostView.text = if (isDarkMode) {
                "Crafting a sarcastic PostDemon message..."
            } else {
                "Creating a helpful PostAngel message..."
            }
        }

        // Get API key
        val apiKey = SecureKeyStore.getOpenAIApiKey(this)
        if (apiKey.isEmpty()) {
            showError("OpenAI API key not set")
            return
        }

        withContext(Dispatchers.IO) {            try {
                val topicContent = getTopicDescription(topic)
                if (topicContent.isBlank()) {
                    showError("Could not load topic description")
                    return@withContext
                }

                val generatedPost = generatePromotionalTweet(apiKey, topic, topicContent, specialInstructions)
                  withContext(Dispatchers.Main) {                    if (generatedPost != null) {
                        // Display the generated post
                        generatedPostView.text = generatedPost
                        copyButton.visibility = View.VISIBLE
                        toggleButtonsLayout.visibility = View.VISIBLE
                        
                        // Set the background color based on the current mode
                        setResponseBackgroundForCurrentMode()
                        
                        // Save post to history with current mode, platform and source
                        val isDarkMode = ModeHelper.isDarkModeActive(this@CreatePostActivity)
                        val platform = ModeHelper.getCurrentPlatform(this@CreatePostActivity)
                        
                        val extraInfo = JSONObject().apply {
                            put("platform", platform)
                        }.toString()
                        
                        PostHistoryManager.savePost(
                            context = this@CreatePostActivity,
                            content = generatedPost,
                            isDarkMode = isDarkMode,
                            source = PostHistoryEntry.SOURCE_CREATE,
                            extraInfo = extraInfo
                        )
                        
                        // Show a toast confirming post was saved to history
                        Toast.makeText(
                            this@CreatePostActivity, 
                            "Post saved to history", 
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        showError("Failed to generate post")
                    }
                    generateButton.isEnabled = true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error generating post", e)
                showError("Error: ${e.message}")
            }
        }
    }    
    
    private fun setResponseBackgroundForCurrentMode() {
        val isDarkMode = ModeHelper.isDarkModeActive(this)
        
        if (isDarkMode) {
            // Dark mode (PostDemon)
            generatedPostView.setBackgroundColor(resources.getColor(R.color.background_dark, theme))
            generatedPostView.setTextColor(resources.getColor(R.color.on_background_dark, theme))
        } else {
            // Light mode (PostAngel)
            generatedPostView.setBackgroundColor(resources.getColor(R.color.background_light, theme))
            generatedPostView.setTextColor(resources.getColor(R.color.on_background, theme))
        }
    }
    
    private fun getTopicDescription(topic: String): String {
        // Try with .txt extension first
        val txtFile = File(filesDir, "$TOPICS_DIR_NAME/${topic}.txt")
        if (txtFile.exists()) {
            try {
                return txtFile.readText()
            } catch (e: IOException) {
                Log.e(TAG, "Error reading .txt topic file", e)
                // Continue to try .md file if txt file exists but can't be read
            }
        }
        
        // Try with .md extension if .txt doesn't exist or couldn't be read
        val mdFile = File(filesDir, "$TOPICS_DIR_NAME/${topic}.md")
        if (mdFile.exists()) {
            try {
                return mdFile.readText()
            } catch (e: IOException) {
                Log.e(TAG, "Error reading .md topic file", e)
            }
        }
        
        // Log if neither file exists
        if (!txtFile.exists() && !mdFile.exists()) {
            Log.w(TAG, "Topic file does not exist: ${txtFile.absolutePath} or ${mdFile.absolutePath}")
        }
        
        return ""
    }    private suspend fun generatePromotionalTweet(apiKey: String, topic: String, topicDescription: String, specialInstructions: String = ""): String? {
        val client = OkHttpClient()
        val prompt = constructGenerationPrompt(topic, topicDescription, specialInstructions)
        
        // Get post generation API URL and key
        val postGenerationUrl = PrefsUtil.getPostGenerationApiUrl(this)
        val postGenerationApiKey = SecureKeyStore.getPostGenerationApiKey(this, PrefsUtil.isUsingGlobalApiKey(this))
        val effectiveApiKey = if (postGenerationApiKey.isNotEmpty()) postGenerationApiKey else apiKey
        
        // Get appropriate system prompt based on current mode (angel/demon)
        val systemPrompt = ModeHelper.getPostGenerationSystemPrompt(this)
            val jsonPayload = JSONObject().apply {
            put("model", PrefsUtil.getPostGenerationModel(this@CreatePostActivity))
            put("messages", org.json.JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
            put("max_tokens", 300)
            put("temperature", 0.7)
        }

        val requestBody = jsonPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
          val request = Request.Builder()
            .url(postGenerationUrl.toString())
            .post(requestBody)
            .addHeader("Authorization", "Bearer $effectiveApiKey")
            .addHeader("Content-Type", "application/json")
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: ""
                    val jsonResponse = JSONObject(responseBody)
                    val choices = jsonResponse.getJSONArray("choices")
                    if (choices.length() > 0) {
                        val content = choices.getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content")                        // Extract tweet from the response
                        val tweetPattern = "TWEET: (.+)".toRegex(RegexOption.DOT_MATCHES_ALL)
                        val tweetMatch = tweetPattern.find(content)
                        val tweet = tweetMatch?.groupValues?.get(1)?.trim() ?: content.trim()
                        
                        tweet
                    } else {
                        null
                    }
                } else {
                    Log.e(TAG, "API request failed: ${response.code}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating tweet", e)
            null
        }
    }    private fun constructGenerationPrompt(topic: String, topicDescription: String, specialInstructions: String = ""): String {
        val specialInstructionsBlock = if (specialInstructions.isNotBlank()) {
            """
            Special instructions:
            $specialInstructions
            """
        } else {
            ""
        }
          val basePrompt = """
            You are creating a social media post to promote a specific topic.
            
            Your task:
            1. Create an engaging, informative post that promotes the topic.
            2. Make the post feel authentic, not like an advertisement.
            3. Include relevant hashtags if appropriate.
            4. Ensure the post is under 280 characters!
            
            Your response should be formatted as:
            TWEET: [Your post text including any hashtags]
            
            Keep in mind:
            - The post should be engaging and encourage interaction
            - The promotion should be subtle and thoughtful
            - The post should be conversational and personable
            - Feel free to ask thought-provoking questions, share interesting facts, or offer insights
            
            Topic to promote: $topic
            
            Topic description:
            $topicDescription
            ${specialInstructionsBlock}
            Create an engaging promotional post for this topic.
        """.trimIndent()
        
        // Modify the prompt based on current mode
        return ModeHelper.modifyPostGenerationPrompt(this, basePrompt)
    }    private suspend fun showError(message: String) {
        withContext(Dispatchers.Main) {
            // Format error message based on current mode
            val errorPrefix = if (ModeHelper.isDarkModeActive(this@CreatePostActivity)) {
                "Error: "
            } else {
                "Sorry, an error occurred: "
            }
            
            generatedPostView.text = errorPrefix + message
            generateButton.isEnabled = true
            copyButton.visibility = View.GONE
            toggleModeButton.visibility = View.GONE
            
            // Keep background color consistent with current mode
            setResponseBackgroundForCurrentMode()
            
            Toast.makeText(this@CreatePostActivity, message, Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onResume() {
        super.onResume()
        
        // Update the title based on current mode
        title = if (ModeHelper.isDarkModeActive(this)) {
            getString(R.string.mode_demon)
        } else {
            getString(R.string.mode_angel)
        }
        
        // Reset temporary mode when returning to this activity
        if (usingAlternateMode) {
            TemporaryModeManager.clearTemporaryMode()
            usingAlternateMode = false
        }
  
          // Update UI based on current mode
        updateToggleModeButtonText()
        if (generatedPostView.text.isNotBlank() && generatedPostView.text != "Generating post...") {
            setResponseBackgroundForCurrentMode()
        }
    }
}
