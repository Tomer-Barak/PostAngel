package com.postmuse.screenshare

import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

class KnowledgeBaseActivity : AppCompatActivity() {    companion object {
        private const val TAG = "KnowledgeBaseActivity"
        const val TOPICS_DIR_NAME = "Topics" // Same constant used across the app
        private const val PERMISSION_REQUEST_READ_EXTERNAL_STORAGE = 101
    }
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TopicAdapter
    private lateinit var fabAddTopic: FloatingActionButton
    private lateinit var topicsDir: File
    private var topics = mutableListOf<TopicFile>()
    
    // File picker launcher
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            handleSelectedFile(uri)
        }
    }
    
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
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_knowledge_base, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_import -> {
                importFilesFromStorage()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
      private fun loadTopics() {
        topics.clear()
        try {
            // Log supported file types
            Log.d(TAG, "Loading topics from directory: ${topicsDir.absolutePath}")
            Log.d(TAG, "Supporting file extensions: .txt, .md")
            
            val files = topicsDir.listFiles { file -> 
                file.isFile && (file.extension.equals("txt", ignoreCase = true) || 
                              file.extension.equals("md", ignoreCase = true))
            }
            
            files?.forEach { file ->
                try {
                    Log.d(TAG, "Loading topic file: ${file.name}")
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
        
        val fileTypes = arrayOf("Text (.txt)", "Markdown (.md)")
        var selectedFileType = 0 // Default to .txt
        
        AlertDialog.Builder(this)
            .setTitle("New Topic")
            .setView(input)
            .setSingleChoiceItems(fileTypes, selectedFileType) { _, which ->
                selectedFileType = which
            }
            .setPositiveButton("Create") { _, _ ->                val topicName = input.text.toString().trim()
                if (topicName.isNotEmpty()) {
                    // Use selected file type
                    val extension = if (selectedFileType == 0) ".txt" else ".md"
                    createNewTopic(topicName, extension)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
      private fun createNewTopic(topicName: String, extension: String = ".txt") {
        var finalName = topicName
        if (!finalName.endsWith(".txt", ignoreCase = true) && !finalName.endsWith(".md", ignoreCase = true)) {
            finalName = "$finalName$extension"
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
    }    private fun importFilesFromStorage() {
        // For Android 13+ (Tiramisu), we don't need specific permission when using the Storage Access Framework
        // It's designed to handle user-selected files without needing permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            openFilePicker()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // For Android 6.0 - 12, we need READ_EXTERNAL_STORAGE
            val permission = android.Manifest.permission.READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(permission),
                    PERMISSION_REQUEST_READ_EXTERNAL_STORAGE
                )
            } else {
                openFilePicker()
            }
        } else {
            // For Android 5.1 and below, no runtime permissions needed
            openFilePicker()
        }
    }    private fun openFilePicker() {
        // Create a string array of MIME types that can be selected
        val mimeTypes = arrayOf(
            "text/*",  // All text files
            "application/octet-stream", // General files
            "*/*"      // All files (as fallback)
        )
        
        // Show a dialog to choose between different file types
        AlertDialog.Builder(this)
            .setTitle("Select File Type")
            .setItems(arrayOf("Text Files (.txt, .md, etc.)", "All Files")) { _, which ->
                when (which) {
                    0 -> {
                        // For text files only
                        filePickerLauncher.launch("text/*")
                    }
                    1 -> {
                        // For all files - we'll filter for supported types later
                        filePickerLauncher.launch("*/*")
                    }
                }
            }
            .show()
    }
      private fun handleSelectedFile(uri: Uri) {
        try {
            val fileDetails = getFileDetailsFromUri(uri)
            val fileName = fileDetails.first
            val mimeType = fileDetails.second
            
            Log.d(TAG, "Selected file: $fileName, MIME type: $mimeType")
            
            // Check if file has a supported extension
            if (!fileName.endsWith(".txt", ignoreCase = true) && !fileName.endsWith(".md", ignoreCase = true)) {
                // Check the MIME type for text content
                val isTextFile = mimeType?.startsWith("text/") == true
                val isPlainTextFile = mimeType == "text/plain" || mimeType == "text/markdown"
                
                if (isTextFile || isPlainTextFile || checkIfFileIsText(uri)) {
                    // It's likely a text file, ask which extension to use
                    showFileExtensionDialog(uri, fileName)
                } else {
                    // Explain the limitation and provide guidance
                    AlertDialog.Builder(this)
                        .setTitle("Unsupported File Type")
                        .setMessage("Only text files (.txt) and markdown files (.md) are supported in the Knowledge Base. Would you like to try importing this file as text?")
                        .setPositiveButton("Try as Text") { _, _ -> 
                            showFileExtensionDialog(uri, fileName) 
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
            } else {
                // Check if file with same name already exists
                val destinationFile = File(topicsDir, fileName)
                if (destinationFile.exists()) {
                    showFileExistsDialog(uri, fileName, destinationFile)
                } else {
                    copyFileToTopics(uri, fileName)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling selected file", e)
            Toast.makeText(this, "Error processing file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    // Helper function to check if a file seems to be text by examining its contents
    private fun checkIfFileIsText(uri: Uri): Boolean {
        try {
            val inputStream = contentResolver.openInputStream(uri) ?: return false
            val buffer = ByteArray(1024) // Read first 1KB to determine file type
            val bytesRead = inputStream.read(buffer)
            inputStream.close()
            
            if (bytesRead <= 0) return false
            
            // Check if content looks like text (avoids obvious binary files)
            var textCount = 0
            var binaryCount = 0
            
            for (i in 0 until bytesRead) {
                val b = buffer[i].toInt() and 0xFF
                if (b <= 0x08 || (b >= 0x0E && b <= 0x1F) || b >= 0x7F) {
                    // Control characters or extended ASCII are likely binary
                    binaryCount++
                } else {
                    textCount++
                }
            }
            
            // If more than 10% looks binary, it's probably not a text file
            return (binaryCount.toFloat() / (textCount + binaryCount)) <= 0.1f
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if file is text", e)
            return false
        }
    }
    
    private fun getFileDetailsFromUri(uri: Uri): Pair<String, String?> {
        val cursor = contentResolver.query(uri, null, null, null, null)
        
        var displayName = "imported_file_${System.currentTimeMillis()}.txt"
        var mimeType: String? = null
        
        cursor?.use {
            if (it.moveToFirst()) {
                val displayNameIndex = it.getColumnIndex("_display_name")
                val mimeTypeIndex = it.getColumnIndex("mime_type")
                
                if (displayNameIndex != -1) {
                    displayName = it.getString(displayNameIndex)
                }
                if (mimeTypeIndex != -1) {
                    mimeType = it.getString(mimeTypeIndex)
                }
            }
        }
        
        return Pair(displayName, mimeType)
    }
    
    private fun showFileExistsDialog(uri: Uri, fileName: String, destinationFile: File) {
        AlertDialog.Builder(this)
            .setTitle("File Already Exists")
            .setMessage("A file named '$fileName' already exists. Would you like to replace it?")
            .setPositiveButton("Replace") { _, _ ->
                copyFileToTopics(uri, fileName)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showFileExtensionDialog(uri: Uri, originalFileName: String) {
        val fileTypes = arrayOf("Text (.txt)", "Markdown (.md)")
        var selectedFileType = 0 // Default to .txt
        
        AlertDialog.Builder(this)
            .setTitle("Select File Format")
            .setMessage("Please select the format to save this file")
            .setSingleChoiceItems(fileTypes, selectedFileType) { _, which ->
                selectedFileType = which
            }
            .setPositiveButton("OK") { _, _ ->
                // Get base name without any extension
                val baseName = originalFileName.substringBeforeLast(".")
                // Add the appropriate extension
                val newFileName = baseName + if (selectedFileType == 0) ".txt" else ".md"
                
                // Check if file with same name already exists
                val destinationFile = File(topicsDir, newFileName)
                if (destinationFile.exists()) {
                    showFileExistsDialog(uri, newFileName, destinationFile)
                } else {
                    copyFileToTopics(uri, newFileName)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun copyFileToTopics(uri: Uri, fileName: String) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val destinationFile = File(topicsDir, fileName)
            
            inputStream?.use { input ->
                FileOutputStream(destinationFile).use { output ->
                    val buffer = ByteArray(4 * 1024) // 4KB buffer
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                    }
                    output.flush()
                }
            }
            
            Toast.makeText(this, "File imported successfully", Toast.LENGTH_SHORT).show()
            loadTopics() // Refresh the list
        } catch (e: Exception) {
            Log.e(TAG, "Error copying file to topics directory", e)
            Toast.makeText(this, "Error importing file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }      override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            PERMISSION_REQUEST_READ_EXTERNAL_STORAGE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openFilePicker()
                } else {
                    Toast.makeText(this, "Permission denied to read external storage", Toast.LENGTH_SHORT).show()
                    // Even without permission, we can still use the Storage Access Framework
                    AlertDialog.Builder(this)
                        .setTitle("Permission Denied")
                        .setMessage("You can still import files by selecting them directly through the system file picker.")
                        .setPositiveButton("Use File Picker") { _, _ -> openFilePicker() }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
            }
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
