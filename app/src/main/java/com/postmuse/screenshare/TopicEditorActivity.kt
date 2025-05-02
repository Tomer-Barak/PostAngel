package com.postangel.screenshare

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import io.noties.markwon.Markwon
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
    private lateinit var markdownPreviewScrollView: ScrollView
    private lateinit var markdownPreviewTextView: TextView
    private lateinit var topicFile: File
    private lateinit var markwon: Markwon
    private var originalContent: String = ""
    private var hasChanges: Boolean = false
    private var isMarkdownFile: Boolean = false
    private var inPreviewMode: Boolean = false
      override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_topic_editor)
        
        // Get file information from intent
        val topicFileName = intent.getStringExtra(EXTRA_TOPIC_FILE_NAME) ?: "Unknown Topic"
        val topicFilePath = intent.getStringExtra(EXTRA_TOPIC_FILE_PATH) 
            ?: throw IllegalArgumentException("Topic file path must be provided")
        
        // Check if this is a markdown file
        isMarkdownFile = topicFileName.endsWith(".md", ignoreCase = true)
        
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
        markdownPreviewScrollView = findViewById(R.id.markdownPreviewScrollView)
        markdownPreviewTextView = findViewById(R.id.markdownPreviewTextView)
          // Initialize Markwon for markdown rendering with basic styling
        markwon = Markwon.create(this)
          // Load topic file
        topicFile = File(topicFilePath)
        loadTopicContent()
        
        // If it's a markdown file, automatically show preview
        if (isMarkdownFile) {
            // Initialize preview content first
            updatePreview()
            
            // Auto-switch to preview mode for markdown files
            inPreviewMode = true
            editTextContent.visibility = View.GONE
            markdownPreviewScrollView.visibility = View.VISIBLE
            invalidateOptionsMenu()
        }
    }
    
    private fun loadTopicContent() {
        try {
            originalContent = topicFile.readText()
            editTextContent.setText(originalContent)
        } catch (e: IOException) {
            Toast.makeText(this, "Error reading topic: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_topic_editor, menu)
        
        // Show the preview toggle only for markdown files
        menu.findItem(R.id.action_toggle_preview).isVisible = isMarkdownFile
        
        return true
    }
    
    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        if (isMarkdownFile) {
            val toggleItem = menu.findItem(R.id.action_toggle_preview)
            if (inPreviewMode) {
                toggleItem.title = "Edit"
                toggleItem.setIcon(android.R.drawable.ic_menu_edit)
            } else {
                toggleItem.title = "Preview"
                toggleItem.setIcon(android.R.drawable.ic_menu_view)
            }
        }
        return super.onPrepareOptionsMenu(menu)
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
            R.id.action_toggle_preview -> {
                togglePreviewMode()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun togglePreviewMode() {
        inPreviewMode = !inPreviewMode
        
        if (inPreviewMode) {
            // Update preview before showing
            updatePreview()
            
            // Switch to preview mode
            editTextContent.visibility = View.GONE
            markdownPreviewScrollView.visibility = View.VISIBLE
            
            // Change menu icon/title
            invalidateOptionsMenu()
        } else {
            // Switch to edit mode
            markdownPreviewScrollView.visibility = View.GONE
            editTextContent.visibility = View.VISIBLE
            
            // Change menu icon/title
            invalidateOptionsMenu()
        }
    }
    
    private fun updatePreview() {
        val content = editTextContent.text.toString()
        markwon.setMarkdown(markdownPreviewTextView, content)
    }
      private fun saveTopicContent() {
        val newContent = editTextContent.text.toString()
        
        try {
            topicFile.writeText(newContent)
            originalContent = newContent
            hasChanges = false
            
            // Update preview if we're in preview mode
            if (inPreviewMode) {
                updatePreview()
            }
            
            Toast.makeText(this, getString(R.string.topic_saved), Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            Toast.makeText(this, "Error saving topic: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
      override fun onBackPressed() {
        // If we're in preview mode and there are unsaved changes,
        // first switch back to edit mode before checking 
        if (inPreviewMode) {
            val currentContent = editTextContent.text.toString()
            if (currentContent != originalContent) {
                // Switch back to edit mode first
                togglePreviewMode()
                // Now we're in edit mode, check for unsaved changes
                checkForUnsavedChanges()
            } else {
                // No changes, can simply finish
                finish()
            }
        } else {
            // Normal back button behavior when in edit mode
            checkForUnsavedChanges()
        }
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
