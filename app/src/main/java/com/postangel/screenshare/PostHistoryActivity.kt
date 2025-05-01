package com.postangel.screenshare

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class PostHistoryActivity : AppCompatActivity() {
    
    private lateinit var postsRecyclerView: RecyclerView
    private lateinit var emptyHistoryTextView: TextView
    private lateinit var clearHistoryButton: Button
    
    private lateinit var adapter: PostHistoryAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_history)
        
        // Set up the action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.post_history)
        
        // Initialize views
        postsRecyclerView = findViewById(R.id.postsRecyclerView)
        emptyHistoryTextView = findViewById(R.id.emptyHistoryTextView)
        clearHistoryButton = findViewById(R.id.clearHistoryButton)
        
        // Set up recycler view
        postsRecyclerView.layoutManager = LinearLayoutManager(this)
        
        // Initialize adapter with empty list
        adapter = PostHistoryAdapter(
            this,
            mutableListOf(),
            onDeletePost = { postId -> deletePost(postId) }
        )
        postsRecyclerView.adapter = adapter
        
        // Set up clear history button
        clearHistoryButton.setOnClickListener {
            showClearHistoryConfirmation()
        }
        
        // Load posts
        loadPosts()
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
    
    /**
     * Load all posts from the history manager
     */
    private fun loadPosts() {
        val posts = PostHistoryManager.getPosts(this)
        
        if (posts.isEmpty()) {
            emptyHistoryTextView.visibility = View.VISIBLE
            postsRecyclerView.visibility = View.GONE
        } else {
            emptyHistoryTextView.visibility = View.GONE
            postsRecyclerView.visibility = View.VISIBLE
            adapter.updatePosts(posts)
        }
    }
    
    /**
     * Delete a post from history
     */
    private fun deletePost(postId: String) {
        if (PostHistoryManager.deletePost(this, postId)) {
            // Remove from adapter
            if (adapter.removePost(postId)) {
                Toast.makeText(this, R.string.post_deleted, Toast.LENGTH_SHORT).show()
                
                // If no posts left, show empty view
                if (adapter.itemCount == 0) {
                    emptyHistoryTextView.visibility = View.VISIBLE
                    postsRecyclerView.visibility = View.GONE
                }
            }
        }
    }
    
    /**
     * Show confirmation dialog before clearing history
     */
    private fun showClearHistoryConfirmation() {
        AlertDialog.Builder(this)
            .setTitle(R.string.clear_all_history)
            .setMessage("Are you sure you want to clear all post history?")
            .setPositiveButton("Clear") { _, _ -> 
                clearAllHistory()
            }
            .setNegativeButton("Cancel", null)
            .create()
            .show()
    }
    
    /**
     * Clear all post history
     */
    private fun clearAllHistory() {
        PostHistoryManager.clearHistory(this)
        adapter.updatePosts(emptyList())
        emptyHistoryTextView.visibility = View.VISIBLE
        postsRecyclerView.visibility = View.GONE
        Toast.makeText(this, R.string.history_cleared, Toast.LENGTH_SHORT).show()
    }
}
