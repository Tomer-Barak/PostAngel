package com.postmuse.screenshare

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

class ShareReceiverActivity : AppCompatActivity() {
    
    private lateinit var statusTextView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var closeButton: Button    
    private lateinit var copyButton: Button // Added copy button
    
    private val TAG = "ShareReceiverActivity"
    
    companion object {
        const val TOPICS_DIR_NAME = "Topics" // Directory for knowledge base
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_share_receiver)
        
        statusTextView = findViewById(R.id.statusTextView)
        progressBar = findViewById(R.id.progressBar)
        closeButton = findViewById(R.id.closeButton)
        copyButton = findViewById(R.id.copyButton) // Initialize copy button
        
        copyButton.visibility = View.GONE // Initially hide copy button
        
        closeButton.setOnClickListener {
            finish()
        }
        
        copyButton.setOnClickListener {
            // Copy the text from statusTextView to clipboard
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("PostMuse Response", statusTextView.text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Response copied to clipboard", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this, "Please set your OpenAI API key in settings", Toast.LENGTH_LONG).show()
            // Open settings so user can set API key
            val settingsIntent = Intent(this, SettingsActivity::class.java)
            startActivity(settingsIntent)
            return
        }
        
        (intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java))?.let { imageUri ->
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
            
        withContext(Dispatchers.Main) {
            statusTextView.text = "Extracting post content..." // Update status
            progressBar.visibility = View.VISIBLE
            copyButton.visibility = View.GONE // Hide copy button during processing
        }
            
        // Read file as base64
        val fileContent = file.readBytes()
        val base64Image = android.util.Base64.encodeToString(fileContent, android.util.Base64.NO_WRAP)
        
        try {
            // Stage 1: Extract content from image
            val extractedContent = processImageWithVisionAPI(client, apiKey, base64Image)
            
            withContext(Dispatchers.Main) {
                statusTextView.text = "Evaluating opportunity..." // Update status
            }            // Stage 2: Evaluate opportunity based on knowledge base
            withContext(Dispatchers.Main) {
                statusTextView.text = "Evaluating for potential response opportunities..." // Update status
            }
              val knowledgeBase = readKnowledgeBase()
            if (knowledgeBase.isBlank()) {
                withContext(Dispatchers.Main) {
                    statusTextView.text = "No topics found in your knowledge base. Please add topic files (.txt or .md) to the \"${TOPICS_DIR_NAME}\" folder."
                    progressBar.visibility = View.GONE
                    copyButton.visibility = View.GONE
                }
                return
            }
            
            val relevantContext = evaluateOpportunity(client, apiKey, extractedContent, knowledgeBase)

            if (relevantContext != null) {
                withContext(Dispatchers.Main) {
                    statusTextView.text = "Found relevant topic! Generating response..." // Update status
                }
                // Stage 3: Generate response if opportunity exists
                val suggestedResponse = generateResponse(client, apiKey, extractedContent, relevantContext)
                
                // Extract topic name for UI display
                val topicPattern = "Topic: ([^\\n]+)".toRegex()
                val topicMatch = topicPattern.find(relevantContext)
                val topicName = topicMatch?.groupValues?.get(1) ?: "a relevant topic"
                
                withContext(Dispatchers.Main) {
                    // Format the final output with some context
                    statusTextView.text = "Response suggestion based on \"$topicName\":\n\n$suggestedResponse" 
                    progressBar.visibility = View.GONE
                    copyButton.visibility = View.VISIBLE // Show copy button
                }
            } else {
                // No opportunity found
                withContext(Dispatchers.Main) {
                    statusTextView.text = "No relevant response opportunity found based on your topics."
                    progressBar.visibility = View.GONE
                    copyButton.visibility = View.GONE
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
    }
    
    private suspend fun processImageWithVisionAPI(client: OkHttpClient, apiKey: String, base64Image: String): String {
        val visionUrl = "https://api.openai.com/v1/chat/completions"
        val jsonPayload = JSONObject()
        jsonPayload.put("model", "gpt-4.1-mini")
        
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
            .url(visionUrl)
            .post(requestBody)
            .addHeader("Authorization", "Bearer $apiKey")
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
        val analysisUrl = "https://api.openai.com/v1/chat/completions"
        
        val prompt = constructAnalysisPrompt(postContent, topic.name, topic.content)
        
        val jsonPayload = JSONObject().apply {
            put("model", "gpt-4o-mini")
            put("messages", org.json.JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", "You are a social media assistant analyzing posts for promotional opportunities.")
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
            .url(analysisUrl)
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
        val instructions = """
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
            - The response should feel natural and relevant to the original post
            - The promotion should be subtle and not forced
            - The response should be respectful and professional
        """.trimIndent()
        
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
        val topicName = topicMatch?.groupValues?.get(1) ?: "unknown topic"
        
        // If we don't have a ready-made response idea, generate one using the LLM
        val generationUrl = "https://api.openai.com/v1/chat/completions"
        val jsonPayload = JSONObject().apply {
            put("model", "gpt-4o-mini")
            
            put("messages", org.json.JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", "You are a helpful assistant that drafts concise and relevant social media replies. Use the provided context to respond to the original post content.")
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
            .url(generationUrl)
            .post(requestBody)
            .addHeader("Authorization", "Bearer $apiKey")
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
    }


    // This function must be inside the class, just before the final closing brace
    private suspend fun showError(message: String) {
        withContext(Dispatchers.Main) {
            statusTextView.text = message // Show error message
            progressBar.visibility = View.GONE            
            copyButton.visibility = View.GONE // Hide copy button on error
            Toast.makeText(this@ShareReceiverActivity, 
                "Error processing image", 
                Toast.LENGTH_LONG).show()
        }
    }
}
