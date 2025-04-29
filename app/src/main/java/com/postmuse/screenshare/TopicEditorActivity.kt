package com.postmuse.screenshare

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.IOException

class TopicEditorActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "TopicEditorActivity"
        private const val EXTRA_TOPIC_FILE_NAME = "extra_topic_file_name"
        private const val EXTRA_TOPIC_FILE_PATH = "extra_topic_file_path"
        
        fun createIntent(context: Context, topicFileName: String, topicFilePath: String): Intent {
            return Intent(context, TopicEditorActivity::class.java).apply {
                putExtra(EXTRA_TOPIC_FILE_NAME, topicFileName)
                putExtra(EXTRA_TOPIC_FILE_PATH, topicFilePath)
            }
        }
    }
    
    private lateinit var editTextContent: EditText
    private lateinit var topicFile: File
    private var originalContent: String = ""
    private var hasChanges: Boolean = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_topic_editor)
        
        // Get file information from intent
        val topicFileName = intent.getStringExtra(EXTRA_TOPIC_FILE_NAME) ?: "Unknown Topic"
        val topicFilePath = intent.getStringExtra(EXTRA_TOPIC_FILE_PATH) 
            ?: throw IllegalArgumentException("Topic file path must be provided")
        
        // Set up action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
          // Display topic name without extension (.txt or .md)
        val displayName = when {
            topicFileName.endsWith(".txt", ignoreCase = true) -> {
                topicFileName.substring(0, topicFileName.length - 4)
            }
            topicFileName.endsWith(".md", ignoreCase = true) -> {
                topicFileName.substring(0, topicFileName.length - 3)
            }
            else -> {
                topicFileName
            }
        }
        supportActionBar?.title = displayName
        
        // Initialize UI components
        editTextContent = findViewById(R.id.editTextTopicContent)
        
        // Load topic file
        topicFile = File(topicFilePath)
        loadTopicContent()
    }
    
    private fun loadTopicContent() {
        try {
            originalContent = topicFile.readText()
            editTextContent.setText(originalContent)
        } catch (e: IOException) {
            Toast.makeText(this, "Error reading topic: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_topic_editor, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                checkForUnsavedChanges()
                true
            }
            R.id.action_save -> {
                saveTopicContent()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun saveTopicContent() {
        val newContent = editTextContent.text.toString()
        
        try {
            topicFile.writeText(newContent)
            originalContent = newContent
            hasChanges = false
            Toast.makeText(this, getString(R.string.topic_saved), Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            Toast.makeText(this, "Error saving topic: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onBackPressed() {
        checkForUnsavedChanges()
    }
    
    private fun checkForUnsavedChanges() {
        val currentContent = editTextContent.text.toString()
        hasChanges = currentContent != originalContent
        
        if (hasChanges) {
            AlertDialog.Builder(this)
                .setTitle("Unsaved Changes")
                .setMessage("You have unsaved changes. Would you like to save before leaving?")
                .setPositiveButton("Save") { _, _ ->
                    saveTopicContent()
                    finish()
                }
                .setNegativeButton("Discard") { _, _ ->
                    finish()
                }
                .setNeutralButton("Cancel", null)
                .show()
        } else {
            finish()
        }
    }
}
