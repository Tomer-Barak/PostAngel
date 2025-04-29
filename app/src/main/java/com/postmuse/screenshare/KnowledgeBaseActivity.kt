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
        private const val PERMISSION_REQUEST_READ_MEDIA = 102
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // READ_MEDIA_DOCUMENTS doesn't exist, use the correct permission for Android 13+
            val permission = android.Manifest.permission.READ_MEDIA_IMAGES
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(permission),
                    PERMISSION_REQUEST_READ_MEDIA
                )
            } else {
                openFilePicker()
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
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
            openFilePicker()
        }
    }    private fun openFilePicker() {
        // Show a dialog to choose between content provider or local storage
        AlertDialog.Builder(this)
            .setTitle("Select File Source")
            .setItems(arrayOf("Document Provider", "Local Storage")) { _, which ->
                when (which) {
                    0 -> {
                        // Use the standard document provider (Content URI)
                        filePickerLauncher.launch("text/*")
                    }
                    1 -> {
                        // Launch a custom file picker that can access direct file paths
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            // For Android 11+, we need to use the Storage Access Framework
                            Toast.makeText(this, "For Android 11+, please select files through the document provider", Toast.LENGTH_LONG).show()
                            filePickerLauncher.launch("text/*")
                        } else {
                            // For older Android versions, we could use a custom file picker
                            // or just fallback to the standard one
                            filePickerLauncher.launch("text/*")
                        }
                    }
                }
            }
            .show()
    }
    
    private fun handleSelectedFile(uri: Uri) {
        try {
            val fileDetails = getFileDetailsFromUri(uri)
            val fileName = fileDetails.first
              // Check if file has a supported extension
            if (!fileName.endsWith(".txt", ignoreCase = true) && !fileName.endsWith(".md", ignoreCase = true)) {
                // Check if it's a text file by MIME type and ask user what extension to use
                val mimeType = fileDetails.second
                if (mimeType == "text/plain" || mimeType == "text/markdown") {
                    showFileExtensionDialog(uri, fileName)
                } else {
                    Toast.makeText(this, "Only .txt and .md files are supported", Toast.LENGTH_SHORT).show()
                    return
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
    }
      override fun onRequestPermissionsResult(
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
                }
            }
            PERMISSION_REQUEST_READ_MEDIA -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openFilePicker()
                } else {
                    Toast.makeText(this, "Permission denied to access media files", Toast.LENGTH_SHORT).show()
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
