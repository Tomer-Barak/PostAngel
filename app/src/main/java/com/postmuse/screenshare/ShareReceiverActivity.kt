package com.postangel.screenshare

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import com.postangel.screenshare.PostHistoryEntry
import com.postangel.screenshare.PostHistoryManager

class ShareReceiverActivity : AppCompatActivity() {
    
    private lateinit var statusTextView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var closeButton: Button
    private lateinit var copyButton: Button    
    private lateinit var refreshButton: Button // Added refresh button
    private lateinit var toggleModeButton: Button // Added toggle mode button
    
    private val TAG = "ShareReceiverActivity"
    
    // Cache extracted content for refresh functionality
    private var cachedExtractedContent: String? = null
    private var cachedKnowledgeBase: String? = null
    
    // Flag to track if we're using an alternate mode for the current analysis
    private var usingAlternateMode = false
    
    companion object {
        const val TOPICS_DIR_NAME = "Topics" // Directory for knowledge base
    }
      override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_share_receiver)
        
        // Set activity title based on current mode
        title = if (ModeHelper.isDarkModeActive(this)) {
            getString(R.string.share_receiver_name_demon)
        } else {
            getString(R.string.share_receiver_name)
        }        
        statusTextView = findViewById(R.id.statusTextView)
        progressBar = findViewById(R.id.progressBar)
        closeButton = findViewById(R.id.closeButton)
        copyButton = findViewById(R.id.copyButton)
        refreshButton = findViewById(R.id.refreshButton) // Initialize refresh button
        toggleModeButton = findViewById(R.id.toggleModeButton) // Initialize toggle mode button
        
        copyButton.visibility = View.GONE // Initially hide copy button
        refreshButton.visibility = View.GONE // Initially hide refresh button
        toggleModeButton.visibility = View.GONE // Initially hide toggle mode button
        
        closeButton.setOnClickListener {
            finish()
        }
        copyButton.setOnClickListener {
            // Copy the text from statusTextView to clipboard
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipLabel = ModeHelper.getAppName(this) + " Response"
            val clip = ClipData.newPlainText(clipLabel, statusTextView.text)
            clipboard.setPrimaryClip(clip)
            
            // Show toast message based on current mode
            val toastMessage = if (ModeHelper.isDarkModeActive(this)) {
                "Sarcastic response copied and ready to unleash"
            } else {
                "Response copied to clipboard"
            }
            Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show()
        }
          refreshButton.setOnClickListener {
            refreshResponse()
        }
        
        // Set toggle mode button text and click listener
        updateToggleModeButtonText()
        toggleModeButton.setOnClickListener {
            toggleAnalysisMode()
        }
        
        // Handle the intent
        when {
            intent?.action == Intent.ACTION_SEND -> {
                if (intent.type?.startsWith("image/") == true) {
                    handleSendImage(intent)
                }
            }
            else -> {
                // Handle other intents
                statusTextView.text = "No image received"
                Log.d(TAG, "No valid intent received")
            }
        }
    }
    private fun handleSendImage(intent: Intent) {
        // Show progress
        progressBar.visibility = View.VISIBLE
        statusTextView.text = getString(R.string.processing_image)        // Check if OpenAI API key is set
        val apiKey = SecureKeyStore.getOpenAIApiKey(this)
        if (apiKey.isEmpty()) {
            // Call suspend function from a coroutine scope
            lifecycleScope.launch {
                showError(getString(R.string.api_key_required))
            }
            Toast.makeText(this, "Please set your OpenAI API key in settings", Toast.LENGTH_LONG).show()            // Open settings so user can set API Key
            val settingsIntent = Intent(this, SettingsActivity::class.java)
            startActivity(settingsIntent)
            return
        }
        
        // Handle API level compatibility - use the appropriate getParcelableExtra method
        val imageUri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
        }
        
        imageUri?.let { uri ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Convert URI to file
                    val imageFile = createTempFileFromUri(imageUri)
                    if (imageFile != null) {
                        // Process the image with OpenAI
                        uploadImage(imageFile)
                    } else {
                        lifecycleScope.launch {
                            showError("Could not access image file")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error handling shared image", e)
                    lifecycleScope.launch {
                        showError("Error: ${e.message}")
                    }
                }
            }
        } ?: lifecycleScope.launch {
            showError(getString(R.string.no_image_received))
        }
    }
    
    private fun createTempFileFromUri(uri: Uri): File? {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val fileName = getFileName(uri)
            val tempFile = File(cacheDir, fileName ?: "temp_screenshot.jpg")
            
            inputStream?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            
            return tempFile
        } catch (e: Exception) {
            Log.e(TAG, "Error creating temp file", e)
            return null
        }
    }
    
    private fun getFileName(uri: Uri): String? {
        var fileName: String? = null
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val displayNameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                if (displayNameIndex != -1) {
                    fileName = cursor.getString(displayNameIndex)
                }
            }
        }
        return fileName ?: "screenshot_${System.currentTimeMillis()}.jpg"
    }      private suspend fun uploadImage(file: File) {
        val client = OkHttpClient.Builder()
            .followSslRedirects(true)
            .followRedirects(true)
            .build()
              // Get API key
        val apiKey = SecureKeyStore.getOpenAIApiKey(this@ShareReceiverActivity)
        if (apiKey.isEmpty()) {
            lifecycleScope.launch {
                showError(getString(R.string.api_key_required))
            }
            return
        }
              withContext(Dispatchers.Main) {            // Update status message based on current mode
            val statusMessage = if (ModeHelper.isDarkModeActive(this@ShareReceiverActivity)) {
                "Analyzing post content with PostDemon..." 
            } else {
                "Extracting post content..."
            }
            statusTextView.text = statusMessage
            progressBar.visibility = View.VISIBLE
            copyButton.visibility = View.GONE // Hide copy button during processing
            refreshButton.visibility = View.GONE // Hide refresh button during processing
            toggleModeButton.visibility = View.GONE // Hide toggle mode button during processing
            
            // Set the background color according to the current mode
            setResponseBackgroundForCurrentMode()
        }
            
        // Read file as base64
        val fileContent = file.readBytes()
        val base64Image = android.util.Base64.encodeToString(fileContent, android.util.Base64.NO_WRAP)
        
        try {
            // Stage 1: Extract content from image
            val extractedContent = processImageWithVisionAPI(client, apiKey, base64Image)
            
            // Cache the extracted content
            cachedExtractedContent = extractedContent
            
            withContext(Dispatchers.Main) {
                statusTextView.text = "Evaluating opportunity..." // Update status
            }            // Stage 2: Evaluate opportunity based on knowledge base
            withContext(Dispatchers.Main) {
                // Update status message based on current mode
                val statusMessage = if (ModeHelper.isDarkModeActive(this@ShareReceiverActivity)) {
                    "Looking for ways to be cleverly contradictory..."
                } else {
                    "Evaluating for potential response opportunities..."
                }
                statusTextView.text = statusMessage
            }
              val knowledgeBase = readKnowledgeBase()
            
            // Cache the knowledge base
            cachedKnowledgeBase = knowledgeBase
            
            if (knowledgeBase.isBlank()) {
                withContext(Dispatchers.Main) {                    
                    statusTextView.text = "No topics found in your knowledge base. Please add topic files (.txt or .md) to the \"${TOPICS_DIR_NAME}\" folder."
                    progressBar.visibility = View.GONE
                    copyButton.visibility = View.GONE
                    refreshButton.visibility = View.GONE
                    toggleModeButton.visibility = View.GONE // Hide toggle mode button when no topics
                }
                return
            }
            
            val relevantContext = evaluateOpportunity(client, apiKey, extractedContent, knowledgeBase)
            
            if (relevantContext != null) {
                withContext(Dispatchers.Main) {
                    // Update message based on current mode
                    val statusMessage = if (ModeHelper.isDarkModeActive(this@ShareReceiverActivity)) {
                        "Perfect opportunity found! Crafting sarcastic response..."
                    } else {
                        "Found relevant topic! Generating response..."
                    }
                    statusTextView.text = statusMessage
                }
                // Stage 3: Generate response if opportunity exists
                val suggestedResponse = generateResponse(client, apiKey, extractedContent, relevantContext)
                
                // Extract topic name for UI display
                val topicPattern = "Topic: ([^\\n]+)".toRegex()
                val topicMatch = topicPattern.find(relevantContext)
                val topicName = topicMatch?.groupValues?.get(1) ?: "a relevant topic"                  
                withContext(Dispatchers.Main) {                    // Format the final output with some context
                    val formattedResponse = "Response suggestion based on \"$topicName\":\n\n$suggestedResponse"
                    statusTextView.text = formattedResponse
                    progressBar.visibility = View.GONE
                    copyButton.visibility = View.VISIBLE // Show copy button
                    refreshButton.visibility = View.VISIBLE // Show refresh button when we have a response
                    toggleModeButton.visibility = View.VISIBLE // Show toggle mode button
                    setResponseBackgroundForCurrentMode() // Set background color based on mode
                    
                    // Save response to post history
                    val isDarkMode = ModeHelper.isDarkModeActive(this@ShareReceiverActivity)
                    PostHistoryManager.savePost(
                        context = this@ShareReceiverActivity,
                        content = suggestedResponse,
                        isDarkMode = isDarkMode,
                        source = PostHistoryEntry.SOURCE_SHARE
                    )
                    
                    // Show a toast confirming post was saved to history
                    Toast.makeText(
                        this@ShareReceiverActivity, 
                        "Response saved to history", 
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {                // No opportunity found
                withContext(Dispatchers.Main) {
                    val noOpportunityMessage = if (ModeHelper.isDarkModeActive(this@ShareReceiverActivity)) {
                        "Nothing worth responding to with sarcasm found. Better luck next time."
                    } else {
                        "No relevant response opportunity found based on your topics."
                    }                    
                    statusTextView.text = noOpportunityMessage
                    progressBar.visibility = View.GONE
                    copyButton.visibility = View.GONE
                    refreshButton.visibility = View.GONE
                    toggleModeButton.visibility = View.VISIBLE // Show toggle mode button even if no opportunity found
                    setResponseBackgroundForCurrentMode() // Set background color based on mode
                }
            }
            
            file.delete() // Clean up temp file
            
        } catch (e: IOException) {
            Log.e(TAG, "Network error during pipeline", e)
            lifecycleScope.launch {
                showError("Network error: ${e.message}")
            }
        } catch (e: Exception) {
             Log.e(TAG, "Error during pipeline", e)
            lifecycleScope.launch {
                showError("Error processing image: ${e.message}")
            }
        } finally {
             // Ensure temp file is deleted even if errors occur before deletion point
             if (file.exists()) {
                 file.delete()
             }
        }
    }    private suspend fun processImageWithVisionAPI(client: OkHttpClient, apiKey: String, base64Image: String): String {
        val visionUrl = PrefsUtil.getVisionApiUrl(this)
        val visionApiKey = SecureKeyStore.getVisionApiKey(this, PrefsUtil.isUsingGlobalApiKey(this))
        val effectiveApiKey = if (visionApiKey.isNotEmpty()) visionApiKey else apiKey
        
        val jsonPayload = JSONObject()
        jsonPayload.put("model", PrefsUtil.getVisionModel(this))
        
        val messagesArray = org.json.JSONArray()
        val userMessage = JSONObject()
        userMessage.put("role", "user")
        
        val contentArray = org.json.JSONArray()
        
        val textContent = JSONObject()
        textContent.put("type", "text")
        // *** Changed prompt to be more specific ***
        textContent.put("text", "Extract the primary text content of the social media post shown in this image. If it's not a social media post, describe the image briefly.")
        contentArray.put(textContent)
        
        val imageContent = JSONObject()
        imageContent.put("type", "image_url")
        val imageUrl = JSONObject()
        imageUrl.put("url", "data:image/jpeg;base64,$base64Image")
        imageContent.put("image_url", imageUrl)
        contentArray.put(imageContent)
        
        userMessage.put("content", contentArray)
        messagesArray.put(userMessage)
        jsonPayload.put("messages", messagesArray)
        jsonPayload.put("max_tokens", 500) // Increased tokens slightly for potentially longer text
        
        val requestBody = jsonPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        
        val request = Request.Builder()
            .url(visionUrl.toString())
            .post(requestBody)
            .addHeader("Authorization", "Bearer $effectiveApiKey")
            .addHeader("Content-Type", "application/json")
            .build()
              client.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: ""
                // Use secure logging to avoid exposing sensitive information
                SecureLogUtil.logDebug(TAG, "Vision API Response", responseBody)
                val jsonResponse = JSONObject(responseBody)
                val choices = jsonResponse.getJSONArray("choices")
                if (choices.length() > 0) {
                    val message = choices.getJSONObject(0).getJSONObject("message")
                    return message.getString("content")
                }
                return "Image processed, but no text content extracted." // More specific message
            } else {
                 val errorBody = response.body?.string() ?: ""
                 // Use secure logging for error responses
                 SecureLogUtil.logError(TAG, "Vision API Error: Code=${response.code}", errorBody)
                throw IOException("Vision API request failed with code ${response.code}")
            }
        }
    }

    // *** New Function: Read knowledge base files ***    
    private suspend fun readKnowledgeBase(): String = withContext(Dispatchers.IO) {
        val topicsDir = File(filesDir, TOPICS_DIR_NAME)
        if (!topicsDir.exists() || !topicsDir.isDirectory) {
            Log.w(TAG, "Topics directory does not exist: ${topicsDir.absolutePath}")
            return@withContext "" // Return empty if directory doesn't exist
        }
          // Log all files in the directory to help troubleshoot
        Log.d(TAG, "Scanning knowledge base directory: ${topicsDir.absolutePath}")
        topicsDir.listFiles()?.forEach { file ->
            Log.d(TAG, "Found file: ${file.name}, isFile=${file.isFile}, extension=${file.extension}")
        }
        
        val knowledgeContent = StringBuilder()
        try {
            // Updated to include both .txt and .md files
            topicsDir.listFiles { file -> 
                file.isFile && (file.extension.equals("txt", ignoreCase = true) || file.extension.equals("md", ignoreCase = true)) 
            }?.forEach { file ->
                try {
                    Log.d(TAG, "Reading knowledge base file: ${file.name}")
                    knowledgeContent.append("--- Topic: ${file.name} ---\n")
                    knowledgeContent.append(file.readText())
                    knowledgeContent.append("\n\n")
                } catch (e: IOException) {
                    Log.e(TAG, "Error reading topic file: ${file.name}", e)
                    // Optionally notify user or skip file
                }
            }
        } catch (e: SecurityException) {
             Log.e(TAG, "SecurityException accessing topics directory", e)
             // Handle lack of permissions if necessary (though internal storage should be fine)
        }
        return@withContext knowledgeContent.toString()
    }    // *** New Function: Evaluate Opportunity (Stage 2) ***
    private suspend fun evaluateOpportunity(client: OkHttpClient, apiKey: String, extractedContent: String, knowledgeBase: String): String? {
         if (knowledgeBase.isBlank()) {
             Log.i(TAG, "Knowledge base is empty, skipping opportunity evaluation.")
             return null // No topics to evaluate against
         }

         Log.d(TAG, "Evaluating opportunity. Extracted Content Length: ${extractedContent.length}, Knowledge Base Length: ${knowledgeBase.length}")

        // Parse topics from the knowledge base
        val topics = parseTopics(knowledgeBase).toMutableList()
        if (topics.isEmpty()) {
            Log.i(TAG, "No valid topics found in knowledge base")
            return null
        }
        
        // Randomly shuffle topics like in the Python implementation
        topics.shuffle()
        
        // Analyze each topic for opportunity (like twitter_agent.search_opportunity)
        for (topic in topics) {
            val (hasOpportunity, responseIdea) = analyzePostForTopic(client, apiKey, extractedContent, topic)
            if (hasOpportunity && !responseIdea.isNullOrBlank()) {
                Log.i(TAG, "Found opportunity for topic: ${topic.name}")
                // Return the topic context along with the response idea
                return "Topic: ${topic.name}\n\n${topic.content}\n\nResponse idea: $responseIdea"
            }
        }
        
        Log.i(TAG, "No opportunity found across all topics")
        return null // No opportunity found in any topic
    }

    // *** New Function: Generate Response (Stage 3) ***    // Topic data class to hold parsed topic information
    data class Topic(val name: String, val content: String)
    
    // Parse topics from the knowledge base text
    private fun parseTopics(knowledgeBase: String): List<Topic> {
        val topics = mutableListOf<Topic>()
        val lines = knowledgeBase.lines()
        
        var currentTopicName = ""
        val currentContent = StringBuilder()
        
        for (line in lines) {
            if (line.startsWith("--- Topic:")) {
                // If we were already building a topic, save it before starting a new one
                if (currentTopicName.isNotEmpty() && currentContent.isNotEmpty()) {
                    topics.add(Topic(currentTopicName, currentContent.toString().trim()))
                    currentContent.clear()
                }
                  // Extract new topic name (remove "--- Topic: " and " ---")
                currentTopicName = line.removePrefix("--- Topic:").removeSuffix("---").trim()
                // Remove file extension if present (.txt or .md)
                if (currentTopicName.endsWith(".txt", ignoreCase = true)) {
                    currentTopicName = currentTopicName.substring(0, currentTopicName.length - 4).trim()
                } else if (currentTopicName.endsWith(".md", ignoreCase = true)) {
                    currentTopicName = currentTopicName.substring(0, currentTopicName.length - 3).trim()
                }
            } else if (currentTopicName.isNotEmpty()) {
                // Add content line to current topic
                currentContent.append(line).append("\n")
            }
        }
        
        // Don't forget to add the last topic
        if (currentTopicName.isNotEmpty() && currentContent.isNotEmpty()) {
            topics.add(Topic(currentTopicName, currentContent.toString().trim()))
        }
        
        return topics
    }
    
    // Analyze post for a specific topic (similar to analyze_tweet in twitter_agent.py)
    private suspend fun analyzePostForTopic(
        client: OkHttpClient, 
        apiKey: String, 
        postContent: String, 
        topic: Topic
    ): Pair<Boolean, String?> {
        val analysisUrl = PrefsUtil.getServerUrl(this)
        
        val prompt = constructAnalysisPrompt(postContent, topic.name, topic.content)
        
        val jsonPayload = JSONObject().apply {
            put("model", "gpt-4o-mini")
            put("messages", org.json.JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", ModeHelper.getAnalysisSystemPrompt(this@ShareReceiverActivity))
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
            put("max_tokens", 512)
            put("temperature", 0.7)
        }
        
        val requestBody = jsonPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
          val request = Request.Builder()
            .url(analysisUrl.toString())
            .post(requestBody)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .build()
            
        try {
            client.newCall(request).execute().use { response ->                
            if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: ""
                    // Use secure logging to avoid exposing sensitive data
                    SecureLogUtil.logDebug(TAG, "Analysis API Response", responseBody)
                    
                    val jsonResponse = JSONObject(responseBody)
                    val choices = jsonResponse.getJSONArray("choices")
                    if (choices.length() > 0) {
                        val message = choices.getJSONObject(0).getJSONObject("message")
                        val aiResponse = message.getString("content")
                        // Parse the response for OPPORTUNITY and IDEA tags
                        var hasOpportunity = false
                        var responseIdea: String? = null
                        for (line in aiResponse.split('\n')) {
                            val trimmedLine = line.trim()
                            if (trimmedLine.startsWith("OPPORTUNITY:", ignoreCase = true)) {
                                val opportunityText = trimmedLine.substringAfter("OPPORTUNITY:", "").trim().uppercase()
                                hasOpportunity = opportunityText == "YES"
                            } else if (trimmedLine.startsWith("IDEA:", ignoreCase = true)) {
                                responseIdea = trimmedLine.substringAfter("IDEA:", "").trim()
                                if (responseIdea.isBlank()) {
                                    responseIdea = null
                                }
                            }
                        }
                        return Pair(hasOpportunity, responseIdea)
                    } else {
                        return Pair(false, null)
                    }                } else {
                    val errorBody = response.body?.string() ?: ""
                    // Use secure logging for error responses
                    SecureLogUtil.logError(TAG, "Analysis API Error: Code=${response.code}", errorBody)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing post for topic", e)
        }
        
        return Pair(false, null)
    }
      // Construct prompt for analysis (similar to _construct_analysis_prompt in twitter_agent.py)    
    private fun constructAnalysisPrompt(postContent: String, topic: String, topicDescription: String): String {
        val isDarkMode = ModeHelper.isDarkModeActive(this)
        
        val baseInstructions = if (isDarkMode) {
            """
            You are analyzing a social media post to determine if there's an opportunity to respond with a sarcastic, contradictory tone while subtly relating to a specific topic.
            
            Your task:
            1. Analyze the content of the post.
            2. Determine if there's a clever way to respond to this post with wit and subtle contradiction.
            3. If there is, provide a brief idea for a response that incorporates the topic with sarcasm.
            4. If there isn't a good opportunity, indicate this clearly.
            
            Your response should be formatted as:
            OPPORTUNITY: [YES/NO]
            IDEA: [Your response idea if OPPORTUNITY is YES, otherwise leave blank]
            
            Keep in mind:
            - The response should feel clever and subtly contradictory to the original post
            - The sarcasm should be witty, not mean-spirited
            - The response should be amusing and mildly cynical
            """
        } else {
            """
            You are analyzing a social media post to determine if there's an opportunity to respond in a way that promotes a specific topic.
            
            Your task:
            1. Analyze the content of the post.
            2. Determine if there's a natural way to respond to this post while promoting the topic.
            3. If there is, provide a brief idea for a response that promotes the topic.
            4. If there isn't a natural way to respond, indicate this clearly.
            
            Your response should be formatted as:
            OPPORTUNITY: [YES/NO]
            IDEA: [Your response idea if OPPORTUNITY is YES, otherwise leave blank]
            
            Keep in mind:
            - The response should feel natural and relevant to the original post (don't force it)
            - The promotion should be subtle and not forced
            - The response should be respectful and professional
            """
        }.trimIndent()
        
        // Apply any additional modifications through the helper
        val instructions = ModeHelper.modifyAnalysisInstructions(this, baseInstructions)
        return """$instructions

            ## Topic to promote: $topic
            
            ## Topic description:
            $topicDescription
            -------------------------------------------------------------------
            
            ## Social Media Post:
            $postContent
            
            -------------------------------------------------------------------
            
            Analyze the post and provide your assessment.
        """.trimIndent()
    }
      private suspend fun generateResponse(client: OkHttpClient, apiKey: String, extractedContent: String, relevantContext: String): String {
        Log.d(TAG, "Generating response. Context: $relevantContext")
        
        // Parse the response idea from the relevantContext if available
        val responseIdeaPattern = "Response idea: (.+)".toRegex(RegexOption.DOT_MATCHES_ALL)
        val responseIdeaMatch = responseIdeaPattern.find(relevantContext)
        val extractedResponseIdea = responseIdeaMatch?.groupValues?.get(1)
        
        if (!extractedResponseIdea.isNullOrBlank()) {
            // If we already have a response idea from the analysis step, use it
            return extractedResponseIdea
        }
        
        // Extract the topic name and content for generation
        val topicPattern = "Topic: ([^\\n]+)".toRegex()
        val topicMatch = topicPattern.find(relevantContext)
        val topicName = topicMatch?.groupValues?.get(1) ?: "unknown topic"        // If we don't have a ready-made response idea, generate one using the LLM
        val generationUrl = PrefsUtil.getResponseApiUrl(this)
        val responseApiKey = SecureKeyStore.getResponseApiKey(this, PrefsUtil.isUsingGlobalApiKey(this))
        // Use the response-specific API key if available, otherwise fall back to the legacy key
        val effectiveApiKey = if (responseApiKey.isNotEmpty()) responseApiKey else apiKey
        
        val jsonPayload = JSONObject().apply {
            put("model", PrefsUtil.getResponseModel(this@ShareReceiverActivity))
              put("messages", org.json.JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", ModeHelper.getResponseGenerationSystemPrompt(this@ShareReceiverActivity))
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", """
                        Original Post Content:
                        ${extractedContent}
                        
                        Relevant Context:
                        ${relevantContext}
                        
                        Draft a helpful and concise reply to the original post using the provided context.
                        Keep the response natural, relevant, and under 280 characters if possible.
                    """.trimIndent())
                })
            })
            put("max_tokens", 150) // Adjust as needed for response length
            put("temperature", 0.7) // Some creativity in responses
        }
        
        val requestBody = jsonPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        
        val request = Request.Builder()
            .url(generationUrl.toString())
            .post(requestBody)
            .addHeader("Authorization", "Bearer $effectiveApiKey")
            .addHeader("Content-Type", "application/json")
            .build()
              try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: ""
                    // Use secure logging to avoid showing full API responses in logs
                    SecureLogUtil.logDebug(TAG, "Generation API Response", responseBody)
                    
                    val jsonResponse = JSONObject(responseBody)
                    val choices = jsonResponse.getJSONArray("choices")
                    if (choices.length() > 0) {
                        val message = choices.getJSONObject(0).getJSONObject("message")
                        return message.getString("content").trim() // Return the generated response
                    } else {
                        // If no choices, return a fallback
                        return "Could not generate a response."
                    }
                } else {
                    val errorBody = response.body?.string() ?: ""
                    // Use secure logging for error responses
                    SecureLogUtil.logError(TAG, "Generation API Error: Code=${response.code}", errorBody)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating response", e)
        }
        
    // Fallback response if generation fails
        return "Based on the topic \"$topicName\", you could respond to this post. (Note: Response generation failed; please try again.)"
    }    // This function must be inside the class, just before the final closing brace
    private suspend fun showError(message: String) {
        withContext(Dispatchers.Main) {
            statusTextView.text = message // Show error message
            progressBar.visibility = View.GONE            
            copyButton.visibility = View.GONE // Hide copy button on error
            refreshButton.visibility = View.GONE // Hide refresh button on error
            toggleModeButton.visibility = View.GONE // Hide toggle mode button on error
            
            // Mode-specific error toast
            val errorToast = if (ModeHelper.isDarkModeActive(this@ShareReceiverActivity)) {
                "Well that didn't work. Technical difficulties, obviously."
            } else {
                "Error processing image"
            }
            
            Toast.makeText(this@ShareReceiverActivity, errorToast, Toast.LENGTH_LONG).show()
        }    }

    /**
     * Refreshes the response by looking for another opportunity using the cached extracted content
     * and knowledge base. Will randomly shuffle topics to provide variety in responses.
     */
    private fun refreshResponse() {
        // Check if we have cached content to work with
        if (cachedExtractedContent.isNullOrBlank() || cachedKnowledgeBase.isNullOrBlank()) {
            val errorMsg = if (ModeHelper.isDarkModeActive(this)) {
                "Can't refresh without content. What did you expect?"
            } else {
                "Unable to refresh response. Missing content."
            }
            Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show()
            return
        }

        // Show progress indicators with mode-appropriate messages
        progressBar.visibility = View.VISIBLE
        val refreshMsg = if (ModeHelper.isDarkModeActive(this)) {
            "Finding another way to be cleverly contrary..."
        } else {
            "Finding another response opportunity..."
        }
        statusTextView.text = refreshMsg
        copyButton.visibility = View.GONE        
        refreshButton.visibility = View.GONE
        
        // Get API key
        val apiKey = SecureKeyStore.getOpenAIApiKey(this)
        if (apiKey.isEmpty()) {
            // Show error in main thread for non-suspend context
            lifecycleScope.launch {
                showError("API key required for generating responses")
            }
            return
        }

        // Launch coroutine to generate new response
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val client = OkHttpClient.Builder()
                    .followSslRedirects(true)
                    .followRedirects(true)
                    .build()
                  // Try to find a new opportunity with the same content
                val relevantContext = evaluateOpportunity(
                    client, 
                    apiKey, 
                    cachedExtractedContent!!, 
                    cachedKnowledgeBase!!
                )
                
                if (relevantContext != null) {
                    withContext(Dispatchers.Main) {
                        val msg = if (ModeHelper.isDarkModeActive(this@ShareReceiverActivity)) {
                            "Found the perfect opening for sarcasm! Crafting response..."
                        } else {
                            "Found another opportunity! Generating response..."
                        }
                        statusTextView.text = msg
                    }
                    
                    // Generate new response
                    val suggestedResponse = generateResponse(
                        client, 
                        apiKey, 
                        cachedExtractedContent!!, 
                        relevantContext
                    )
                    
                    // Extract topic name for display
                    val topicPattern = "Topic: ([^\\n]+)".toRegex()
                    val topicMatch = topicPattern.find(relevantContext)
                    val topicName = topicMatch?.groupValues?.get(1) ?: "a relevant topic"
                      // Update UI
                    withContext(Dispatchers.Main) {
                        statusTextView.text = "Response suggestion based on \"$topicName\":\n\n$suggestedResponse"
                        progressBar.visibility = View.GONE
                        copyButton.visibility = View.VISIBLE
                        refreshButton.visibility = View.VISIBLE
                        
                        // Save refreshed response to post history
                        val isDarkMode = ModeHelper.isDarkModeActive(this@ShareReceiverActivity)
                        PostHistoryManager.savePost(
                            context = this@ShareReceiverActivity,
                            content = suggestedResponse,
                            isDarkMode = isDarkMode,
                            source = PostHistoryEntry.SOURCE_SHARE
                        )
                        
                        // Show a toast confirming post was saved to history
                        Toast.makeText(
                            this@ShareReceiverActivity, 
                            "Response saved to history", 
                            Toast.LENGTH_SHORT
                        ).show()
                    }} else {
                    // No new opportunity found
                    withContext(Dispatchers.Main) {
                        val noOpportunityMsg = if (ModeHelper.isDarkModeActive(this@ShareReceiverActivity)) {
                            "Sorry, can't find anything else worth mocking in this content."
                        } else {
                            "No additional response opportunities found."
                        }
                        statusTextView.text = noOpportunityMsg
                        progressBar.visibility = View.GONE
                        copyButton.visibility = View.GONE
                        refreshButton.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing response", e)
                lifecycleScope.launch {
                    showError("Error refreshing: ${e.message}")
                }
            }
        }
    }    override fun onResume() {
        super.onResume()
        
        // Update the title based on current mode when returning to activity
        title = if (ModeHelper.isDarkModeActive(this)) {
            getString(R.string.share_receiver_name_demon)
        } else {
            getString(R.string.share_receiver_name)
        }
    }
    
    /**
     * Updates the toggle mode button text based on the current mode
     */
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
    
    /**
     * Toggles between light (PostAngel) and dark (PostDemon) modes for the current analysis
     * without affecting the app's global setting
     */
    private fun toggleAnalysisMode() {
        if (cachedExtractedContent.isNullOrBlank() || cachedKnowledgeBase.isNullOrBlank()) {
            val errorMsg = if (ModeHelper.isDarkModeActive(this)) {
                "Can't switch modes without content to analyze. Do better next time."
            } else {
                "Unable to switch modes. No content to analyze."
            }
            Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show()
            return
        }
        
        // Toggle to temporary mode
        val isDarkMode = TemporaryModeManager.toggleTemporaryMode(this)
        usingAlternateMode = true
        
        // Update UI elements for the new mode
        updateToggleModeButtonText()
        setResponseBackgroundForCurrentMode()
        
        // Show processing UI
        progressBar.visibility = View.VISIBLE
        val modeChangeMsg = if (isDarkMode) {
            "Switching to PostDemon mode for sarcastic analysis..."
        } else {
            "Switching to PostAngel mode for helpful analysis..."
        }
        statusTextView.text = modeChangeMsg
        copyButton.visibility = View.GONE
        
        // Get API key
        val apiKey = SecureKeyStore.getOpenAIApiKey(this)
        if (apiKey.isEmpty()) {
            lifecycleScope.launch {
                showError("API key required for generating responses")
            }
            return
        }
        
        // Launch coroutine to generate new response with the alternate mode
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val client = OkHttpClient.Builder()
                    .followSslRedirects(true)
                    .followRedirects(true)
                    .build()
                
                // Find an opportunity with the alternate mode
                val relevantContext = evaluateOpportunity(
                    client, 
                    apiKey, 
                    cachedExtractedContent!!, 
                    cachedKnowledgeBase!!
                )
                
                if (relevantContext != null) {
                    withContext(Dispatchers.Main) {
                        val msg = if (isDarkMode) {
                            "Found the perfect opening for sarcasm! Crafting response..."
                        } else {
                            "Found a relevant opportunity! Generating helpful response..."
                        }
                        statusTextView.text = msg
                    }
                    
                    // Generate new response
                    val suggestedResponse = generateResponse(
                        client, 
                        apiKey, 
                        cachedExtractedContent!!, 
                        relevantContext
                    )
                    
                    // Extract topic name for display
                    val topicPattern = "Topic: ([^\\n]+)".toRegex()
                    val topicMatch = topicPattern.find(relevantContext)
                    val topicName = topicMatch?.groupValues?.get(1) ?: "a relevant topic"
                      // Update UI
                    withContext(Dispatchers.Main) {
                        statusTextView.text = "Response suggestion based on \"$topicName\":\n\n$suggestedResponse"
                        progressBar.visibility = View.GONE
                        copyButton.visibility = View.VISIBLE
                        refreshButton.visibility = View.VISIBLE
                        toggleModeButton.visibility = View.VISIBLE
                        setResponseBackgroundForCurrentMode()
                        
                        // Save toggled mode response to post history
                        val isDarkMode = ModeHelper.isDarkModeActive(this@ShareReceiverActivity)
                        PostHistoryManager.savePost(
                            context = this@ShareReceiverActivity,
                            content = suggestedResponse,
                            isDarkMode = isDarkMode,
                            source = PostHistoryEntry.SOURCE_SHARE
                        )
                        
                        // Show a toast confirming post was saved to history
                        Toast.makeText(
                            this@ShareReceiverActivity, 
                            "Response saved to history", 
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    // No opportunity found
                    withContext(Dispatchers.Main) {
                        val noOpportunityMsg = if (isDarkMode) {
                            "Sorry, can't find anything worth mocking in this content."
                        } else {
                            "No relevant response opportunities found."
                        }
                        statusTextView.text = noOpportunityMsg
                        progressBar.visibility = View.GONE
                        copyButton.visibility = View.GONE
                        refreshButton.visibility = View.GONE
                        toggleModeButton.visibility = View.VISIBLE
                        // Reset to original mode if we can't find an opportunity
                        TemporaryModeManager.clearTemporaryMode()
                        updateToggleModeButtonText()
                        setResponseBackgroundForCurrentMode()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error generating response with alternate mode", e)
                lifecycleScope.launch {
                    showError("Error generating response: ${e.message}")
                    // Reset to original mode on error
                    TemporaryModeManager.clearTemporaryMode()
                    updateToggleModeButtonText()
                    setResponseBackgroundForCurrentMode()
                }
            }
        }
    }
    
    /**
     * Sets the background color of the response text view based on the current mode
     */
    private fun setResponseBackgroundForCurrentMode() {
        val scrollView = findViewById<ScrollView>(R.id.scrollView)
        val isDarkMode = ModeHelper.isDarkModeActive(this)
          if (isDarkMode) {
            // Dark mode (PostDemon)
            scrollView.setBackgroundColor(resources.getColor(R.color.background_dark, theme))
            statusTextView.setTextColor(resources.getColor(R.color.on_background_dark, theme))
        } else {
            // Light mode (PostAngel)
            scrollView.setBackgroundColor(resources.getColor(R.color.background_light, theme))
            statusTextView.setTextColor(resources.getColor(R.color.on_background, theme))
        }
    }
}
