package com.postangel.screenshare

import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
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
import androidx.documentfile.provider.DocumentFile
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
    
    // Folder picker launcher
    private val folderPickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri?.let {
            handleSelectedFolder(it)
        }
    }

    private fun handleSelectedFolder(uri: Uri) {
        val docTreeUri = DocumentFile.fromTreeUri(this, uri) ?: return
        var importedCount = 0
        var errorCount = 0

        docTreeUri.listFiles().forEach { file ->
            if (!file.isFile) return@forEach

            val name = file.name ?: return@forEach
            val isTextFile = name.endsWith(".txt", true) || name.endsWith(".md", true)
                    || file.type?.startsWith("text/") == true

            if (isTextFile) {
                try {
                    contentResolver.openInputStream(file.uri)?.use { input ->
                        val destinationFile = File(topicsDir, name)
                        FileOutputStream(destinationFile).use { output ->
                            input.copyTo(output)
                        }
                        importedCount++
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error importing file: ${file.name}", e)
                    errorCount++
                }
            }
        }

        // Show result to user
        val message = when {
            importedCount > 0 && errorCount == 0 -> 
                "Successfully imported $importedCount files"
            importedCount > 0 && errorCount > 0 -> 
                "Imported $importedCount files. Failed to import $errorCount files."
            errorCount > 0 -> 
                "Failed to import $errorCount files."
            else -> 
                "No text files found in the selected folder"
        }
        
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        if (importedCount > 0) {
            loadTopics() // Refresh the list if we imported any files
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
            topicsDir.mkdirs()
            Log.d(TAG, "Created Topics directory: ${topicsDir.absolutePath}")
        }

        // Log existing files before checking emptiness
        val existingFiles = topicsDir.listFiles()
        Log.d(TAG, "Checking Topics directory: ${topicsDir.absolutePath}")
        Log.d(TAG, "Files found: ${existingFiles?.map { it.name }?.joinToString() ?: "None or Error"}")

        // Initialize the adapter BEFORE loadTopics is called
        adapter = TopicAdapter(
            topics,
            onItemClick = { topic -> openTopicEditor(topic) },
            onDeleteClick = { topic -> confirmDeleteTopic(topic) },
            context = this
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Add sample topic if the directory is empty
        if (existingFiles?.isEmpty() == true) {
            Log.d(TAG, "Topics directory IS empty. Calling createSampleTopic().") // Added log
            createSampleTopic()
            loadTopics() // Reload topics after creating samples
        } else {
            Log.d(TAG, "Topics directory IS NOT empty. Skipping createSampleTopic().") // Added log
            // Load topics even if not empty (original behavior)
            loadTopics()
        }

        fabAddTopic.setOnClickListener {
            showNewTopicDialog()
        }
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
            R.id.action_delete_all -> {
                confirmDeleteAllTopics()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun confirmDeleteAllTopics() {
        if (topics.isEmpty()) {
            Toast.makeText(this, "No topics to delete", Toast.LENGTH_SHORT).show()
            return
        }
        
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_all_topics)
            .setMessage("Are you sure you want to delete all topics? This action cannot be undone.")
            .setPositiveButton("Delete All") { _, _ ->
                deleteAllTopics()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun deleteAllTopics() {
        try {
            var deletedCount = 0
            val topicsCopy = topics.toList() // Create a copy to avoid concurrent modification
            
            for (topic in topicsCopy) {
                if (topic.file.delete()) {
                    deletedCount++
                }
            }
            
            // Update UI
            topics.clear()
            adapter.notifyDataSetChanged()
            findViewById<View>(R.id.emptyView).visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            
            Toast.makeText(this, "Deleted $deletedCount topics", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting all topics", e)
            Toast.makeText(this, "Error deleting topics: ${e.message}", Toast.LENGTH_SHORT).show()
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
            
            // Sort files - put markdown files first
            val sortedFiles = files?.sortedWith(compareBy { 
                !it.name.endsWith(".md", ignoreCase = true) 
            })
            
            sortedFiles?.forEach { file ->
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
    Log.d(TAG, "Starting to create sample topics...")
    
    // Create Topics directory if it doesn't exist
    if (!topicsDir.exists()) {
        topicsDir.mkdirs()
        Log.d(TAG, "Created Topics directory: ${topicsDir.absolutePath}")
    }

    var textCreated = false
    var markdownCreated = false

    // Create text sample first
    try {
        val sampleTextFile = File(topicsDir, "Sample Topic.txt")
        sampleTextFile.writeText(
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
        textCreated = sampleTextFile.exists() && sampleTextFile.length() > 0
        Log.d(TAG, "Text sample creation ${if (textCreated) "successful" else "failed"}")
    } catch (e: Exception) {
        Log.e(TAG, "Failed to create text sample", e)
    }

    
    // Try to create markdown sample
    try {
        createMarkdownSample()
        val markdownFile = File(topicsDir, "PostAngel.md")
        markdownCreated = markdownFile.exists() && markdownFile.length() > 0
        Log.d(TAG, "Markdown sample creation ${if (markdownCreated) "successful" else "failed"}")
    } catch (e: Exception) {
        Log.e(TAG, "Failed to create main markdown sample, trying fallback", e)
        try {
            createFallbackMarkdownSample()
            val fallbackFile = File(topicsDir, "Markdown Example.md")
            markdownCreated = fallbackFile.exists() && fallbackFile.length() > 0
            Log.d(TAG, "Fallback markdown creation ${if (markdownCreated) "successful" else "failed"}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create fallback markdown sample", e)
        }
    }

    // Log final results
    Log.d(TAG, "Sample creation complete. Text: $textCreated, Markdown: $markdownCreated")
    
    // Force refresh of topics list if at least one sample was created
    if (textCreated || markdownCreated) {
        loadTopics()
    }
}

private fun createMarkdownSample() {
    val sampleMarkdownFile = File(topicsDir, "PostAngel.md")
    Log.d(TAG, "Attempting to create markdown file at: ${sampleMarkdownFile.absolutePath}")

    try {
        // Check if resource exists first
        val resourceId = R.raw.readme_content
        Log.d(TAG, "Looking for resource with ID: $resourceId")
        
        try {
            resources.getResourceName(resourceId)
        } catch (e: Resources.NotFoundException) {
            Log.e(TAG, "Resource readme_content not found in R.raw", e)
            throw IOException("Resource not found: R.raw.readme_content")
        }

        // If we got here, resource exists, try to read it
        val inputStream = resources.openRawResource(resourceId)
        Log.d(TAG, "Successfully opened resource stream")
        
        val readmeContent = inputStream.bufferedReader().use { 
            Log.d(TAG, "Reading content from resource stream")
            it.readText() 
        }
        
        // Make sure the content is not empty
        if (readmeContent.isBlank()) {
            Log.e(TAG, "README content is empty")
            throw IOException("README content is empty")
        }
        
        Log.d(TAG, "Read content length: ${readmeContent.length} characters")
        
        // Write content to file
        sampleMarkdownFile.writeText(readmeContent)
        Log.d(TAG, "Wrote content to file")
        
        // Verify file was created successfully
        if (sampleMarkdownFile.exists() && sampleMarkdownFile.length() > 0) {
            Log.d(TAG, "Successfully created markdown sample: ${sampleMarkdownFile.length()} bytes")
        } else {
            Log.e(TAG, "File creation verification failed")
            throw IOException("Markdown file was not created properly")
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to create markdown sample from resource", e)
        throw e // Re-throw to trigger fallback
    }
}

      
private fun createFallbackMarkdownSample() {
    val sampleMarkdownFile = File(topicsDir, "Markdown Example.md")
    try {
        sampleMarkdownFile.writeText(
            """
            # Markdown Example
            
            This is a fallback sample topic showing how to use Markdown formatting.
            
            ## Features
            - You can use Markdown for formatting
            - Like **bold** and *italic* text
            - Or create lists like this one
            
            ## Examples
            
            ### Text Formatting
            - **Bold text** using double asterisks
            - *Italic text* using single asterisks
            - `Code snippets` using backticks
            
            ### Lists
            1. Numbered lists
            2. Are easy to create
            3. Just start with numbers
            
            ### Quotes
            > You can create blockquotes
            > For important information
            
            ### Links
            You can add [links to websites](https://example.com)
            
            ## Tips for Usage
            - Keep your content organized with headers
            - Use lists for better readability
            - Include relevant keywords
            - Add examples when helpful
            
            Feel free to edit this file or create new ones!
            """.trimIndent()
        )
        
        if (sampleMarkdownFile.exists() && sampleMarkdownFile.length() > 0) {
            Log.d(TAG, "Successfully created fallback markdown sample: ${sampleMarkdownFile.absolutePath}")
        } else {
            throw IOException("Fallback markdown file was created but is empty")
        }
        
    } catch (e: Exception) {
        Log.e(TAG, "Failed to create fallback markdown sample", e)
        throw e
    }
}
    
    private fun showNewTopicDialog() {
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT
        
        // Add padding to the input field (16dp on all sides)
        val paddingInDp = 16
        val scale = resources.displayMetrics.density
        val paddingInPx = (paddingInDp * scale + 0.5f).toInt()
        input.setPadding(paddingInPx, paddingInPx, paddingInPx, paddingInPx)
        
        // Set initial hint for .txt
        input.hint = "Topic name (will be saved as [name].txt)"
        
        val fileTypes = arrayOf("Text (.txt)", "Markdown (.md)")
        var selectedFileType = 0 // Default to .txt
        
        val dialog = AlertDialog.Builder(this)
            .setTitle("New Topic")
            .setView(input)
            .setSingleChoiceItems(fileTypes, selectedFileType) { _, which ->
                selectedFileType = which
                // Update hint text based on selection
                input.hint = "Topic name (will be saved as [name]${if (which == 0) ".txt" else ".md"})"
            }
            .setPositiveButton("Create") { _, _ ->                
                val topicName = input.text.toString().trim()
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
            
            // Initial content based on file type
            val isMarkdownFile = finalName.endsWith(".md", ignoreCase = true)
            val initialContent = if (isMarkdownFile) {
                """
                # ${finalName.substringBeforeLast(".")}
                
                ## About this topic
                
                Write your topic information here using Markdown formatting.
                
                ## Markdown Tips:
                - **Bold text** is created using `**text**`
                - *Italic text* is created using `*text*`
                - Use `#` symbols for headings
                - Create lists with `-` or `*`
                - [Links](https://example.com) use `[text](url)`
                
                Delete these tips and add your own content.
                """.trimIndent()
            } else {
                ""
            }
            
            // Create file with initial content
            if (newFile.createNewFile()) {
                newFile.writeText(initialContent)
                val newTopic = TopicFile(finalName, initialContent, newFile)
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
        // Show a dialog to choose between different import options
        AlertDialog.Builder(this)
            .setTitle("Import Options")
            .setItems(arrayOf("Select Text Files", "Select Folder", "Select Any File")) { _, which ->
                when (which) {
                    0 -> {
                        // For text files only
                        filePickerLauncher.launch("text/*")
                    }
                    1 -> {
                        // For folder selection - requires ACTION_OPEN_DOCUMENT_TREE
                        folderPickerLauncher.launch(null)
                    }
                    2 -> {
                        // For all files - we'll filter for supported types later
                        filePickerLauncher.launch("*/*")
                    }
                }
            }
            .show()
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
