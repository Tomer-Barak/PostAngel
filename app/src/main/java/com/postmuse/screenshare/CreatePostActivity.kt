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

class CreatePostActivity : AppCompatActivity() {    
    private lateinit var topicSpinner: Spinner
    private lateinit var specialInstructionsEditText: EditText
    private lateinit var generateButton: Button
    private lateinit var generatedPostView: TextView
    private lateinit var copyButton: Button

    private val TAG = "CreatePostActivity"

    companion object {
        const val TOPICS_DIR_NAME = "Topics" // Directory for topics
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_post)        // Initialize views
        topicSpinner = findViewById(R.id.topicSpinner)
        specialInstructionsEditText = findViewById(R.id.specialInstructionsEditText)
        generateButton = findViewById(R.id.generatePostButton)
        generatedPostView = findViewById(R.id.generatedPostTextView)
        copyButton = findViewById(R.id.copyPostButton)

        // Initially hide copy button
        copyButton.visibility = View.GONE

        // Load topics into spinner
        loadTopics()        // Set up click listeners        
        generateButton.setOnClickListener {
            val selectedTopic = topicSpinner.selectedItem?.toString()
            if (selectedTopic != null) {
                val specialInstructions = specialInstructionsEditText.text.toString().trim()
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
                  withContext(Dispatchers.Main) {
                    if (generatedPost != null) {
                        // Display the generated post
                        generatedPostView.text = generatedPost
                        copyButton.visibility = View.VISIBLE
                        
                        // Save post to history with current mode and source
                        val isDarkMode = ModeHelper.isDarkModeActive(this@CreatePostActivity)
                        PostHistoryManager.savePost(
                            context = this@CreatePostActivity,
                            content = generatedPost,
                            isDarkMode = isDarkMode,
                            source = PostHistoryEntry.SOURCE_CREATE
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
    }    private fun getTopicDescription(topic: String): String {
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
        
        // Get appropriate system prompt based on current mode (angel/demon)
        val systemPrompt = ModeHelper.getPostGenerationSystemPrompt(this)
            val jsonPayload = JSONObject().apply {
            put("model", "gpt-4o-mini")
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
            .url("https://api.openai.com/v1/chat/completions")
            .post(requestBody)
            .addHeader("Authorization", "Bearer $apiKey")
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
    }

    private suspend fun showError(message: String) {
        withContext(Dispatchers.Main) {
            generatedPostView.text = message
            generateButton.isEnabled = true
            copyButton.visibility = View.GONE
            Toast.makeText(this@CreatePostActivity, message, Toast.LENGTH_LONG).show()
        }
    }    // Menu options removed as history is now accessible from the main activity
}
