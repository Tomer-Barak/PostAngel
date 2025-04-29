package com.postmuse.screenshare

import android.content.DialogInterface
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File
import java.io.IOException

class KnowledgeBaseActivity : AppCompatActivity() {
      companion object {
        private const val TAG = "KnowledgeBaseActivity"
        const val TOPICS_DIR_NAME = "Topics" // Same constant used across the app
    }
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TopicAdapter
    private lateinit var fabAddTopic: FloatingActionButton
    private lateinit var topicsDir: File
    private var topics = mutableListOf<TopicFile>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_knowledge_base)
        
        // Set up the action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.knowledge_base)
        
        // Initialize UI components
        recyclerView = findViewById(R.id.recyclerViewTopics)
        fabAddTopic = findViewById(R.id.fabAddTopic)
        
        // Setup Topics directory
        topicsDir = File(filesDir, TOPICS_DIR_NAME)
        if (!topicsDir.exists()) {
            if (topicsDir.mkdirs()) {
                Log.d(TAG, "Created topics directory at ${topicsDir.absolutePath}")
            } else {
                Log.e(TAG, "Failed to create topics directory")
                Toast.makeText(this, "Failed to create topics directory", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Setup RecyclerView
        adapter = TopicAdapter(topics, 
            onItemClick = { topic -> openTopicEditor(topic) },
            onDeleteClick = { topic -> confirmDeleteTopic(topic) }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        
        // Setup FAB for adding new topics
        fabAddTopic.setOnClickListener {
            showNewTopicDialog()
        }
        
        // Add sample topic if the directory is empty
        if (topicsDir.listFiles()?.isEmpty() == true) {
            createSampleTopic()
        }
        
        // Load topics
        loadTopics()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    
    private fun loadTopics() {
        topics.clear()
        
        try {
            val files = topicsDir.listFiles { file -> 
                file.isFile && file.extension.equals("txt", ignoreCase = true)
            }
            
            files?.forEach { file ->
                try {
                    val content = file.readText()
                    topics.add(TopicFile(file.name, content, file))
                } catch (e: IOException) {
                    Log.e(TAG, "Error reading file: ${file.name}", e)
                }
            }
            
            // Update UI
            if (topics.isEmpty()) {
                findViewById<View>(R.id.emptyView).visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                findViewById<View>(R.id.emptyView).visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
            }
            
            adapter.notifyDataSetChanged()
        } catch (e: Exception) {
            Log.e(TAG, "Error loading topics", e)
            Toast.makeText(this, "Error loading topics: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun createSampleTopic() {
        try {
            val sampleFile = File(topicsDir, "Sample Topic.txt")
            sampleFile.writeText(
                """
                This is a sample topic file.
                
                In your topic files, you should include information about products,
                services, or ideas that you want to promote when relevant opportunities
                arise on social media.
                
                The app will analyze social media posts you share and suggest responses
                based on the content in these topic files when there's a relevant match.
                
                You can edit this file or create new ones to customize the knowledge base.
                """.trimIndent()
            )
            Log.d(TAG, "Created sample topic file")
        } catch (e: Exception) {
            Log.e(TAG, "Error creating sample topic", e)
        }
    }
    
    private fun showNewTopicDialog() {
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT
        input.hint = "Topic name (will be saved as [name].txt)"
        
        AlertDialog.Builder(this)
            .setTitle("New Topic")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                val topicName = input.text.toString().trim()
                if (topicName.isNotEmpty()) {
                    createNewTopic(topicName)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun createNewTopic(topicName: String) {
        var finalName = topicName
        if (!finalName.endsWith(".txt", ignoreCase = true)) {
            finalName = "$finalName.txt"
        }
        
        try {
            val newFile = File(topicsDir, finalName)
            
            // Check if file already exists
            if (newFile.exists()) {
                Toast.makeText(this, "A topic with this name already exists", Toast.LENGTH_SHORT).show()
                return
            }
            
            // Create empty file
            if (newFile.createNewFile()) {
                val newTopic = TopicFile(finalName, "", newFile)
                topics.add(newTopic)
                adapter.notifyItemInserted(topics.size - 1)
                
                // Update empty view visibility
                findViewById<View>(R.id.emptyView).visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                
                // Open editor for the new topic
                openTopicEditor(newTopic)
            } else {
                Toast.makeText(this, "Failed to create new topic file", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating new topic", e)
            Toast.makeText(this, "Error creating topic: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun openTopicEditor(topic: TopicFile) {
        val intent = TopicEditorActivity.createIntent(
            context = this,
            topicFileName = topic.file.name,
            topicFilePath = topic.file.absolutePath
        )
        startActivity(intent)
    }
    
    private fun confirmDeleteTopic(topic: TopicFile) {
        AlertDialog.Builder(this)
            .setTitle("Delete Topic")
            .setMessage("Are you sure you want to delete the topic '${topic.name}'?")
            .setPositiveButton("Delete") { _, _ ->
                deleteTopic(topic)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun deleteTopic(topic: TopicFile) {
        try {
            if (topic.file.delete()) {
                val position = topics.indexOf(topic)
                topics.removeAt(position)
                adapter.notifyItemRemoved(position)
                
                // Update empty view visibility if needed
                if (topics.isEmpty()) {
                    findViewById<View>(R.id.emptyView).visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                }
                
                Toast.makeText(this, "Topic deleted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to delete topic file", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting topic", e)
            Toast.makeText(this, "Error deleting topic: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Reload topics to refresh content after possible edits
        loadTopics()
    }
}

// Data class for topic files
data class TopicFile(
    val name: String,
    val content: String,
    val file: File
)
