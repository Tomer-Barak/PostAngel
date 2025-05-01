package com.postangel.screenshare

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.util.Date

/**
 * Adapter for displaying post history items in a RecyclerView
 */
class PostHistoryAdapter(
    private val context: Context,
    private var posts: MutableList<PostHistoryEntry>,
    private val onDeletePost: (String) -> Unit
) : RecyclerView.Adapter<PostHistoryAdapter.PostViewHolder>() {

    class PostViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val postItemLayout: LinearLayout = view.findViewById(R.id.postItemLayout)
        val postContentTextView: TextView = view.findViewById(R.id.postContentTextView)
        val postDateTextView: TextView = view.findViewById(R.id.postDateTextView)
        val postSourceTextView: TextView = view.findViewById(R.id.postSourceTextView)
        val copyPostButton: Button = view.findViewById(R.id.copyPostButton)
        val deletePostButton: Button = view.findViewById(R.id.deletePostButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post_history, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]
        
        // Set post content
        holder.postContentTextView.text = post.content
        
        // Set formatted date
        holder.postDateTextView.text = getRelativeTime(post.timestamp)
        
        // Set source text
        holder.postSourceTextView.text = getSourceText(post)
        
        // Apply background color based on mode (dark/light)
        val backgroundColor = if (post.isDarkMode) {
            ContextCompat.getColor(context, R.color.background_dark)
        } else {
            ContextCompat.getColor(context, R.color.background_light)
        }
        holder.postItemLayout.setBackgroundColor(backgroundColor)
        
        // Set text color based on mode
        val textColor = if (post.isDarkMode) {
            ContextCompat.getColor(context, R.color.on_background_dark)
        } else {
            ContextCompat.getColor(context, R.color.on_background_light)
        }
        holder.postContentTextView.setTextColor(textColor)
        
        // Set up button listeners
        holder.copyPostButton.setOnClickListener {
            copyToClipboard(post.content)
            Toast.makeText(context, R.string.post_copied, Toast.LENGTH_SHORT).show()
        }
        
        holder.deletePostButton.setOnClickListener {
            onDeletePost(post.id)
        }
        
        // Set button text color
        val buttonTextColor = if (post.isDarkMode) {
            ContextCompat.getColor(context, R.color.button_text_dark)
        } else {
            ContextCompat.getColor(context, R.color.button_text_light)
        }
        holder.copyPostButton.setTextColor(buttonTextColor)
        holder.deletePostButton.setTextColor(buttonTextColor)
    }
    
    override fun getItemCount() = posts.size
    
    /**
     * Update the data in the adapter
     */
    fun updatePosts(newPosts: List<PostHistoryEntry>) {
        posts.clear()
        posts.addAll(newPosts)
        notifyDataSetChanged()
    }
    
    /**
     * Remove a post from the adapter
     */
    fun removePost(postId: String): Boolean {
        val index = posts.indexOfFirst { it.id == postId }
        if (index != -1) {
            posts.removeAt(index)
            notifyItemRemoved(index)
            return true
        }
        return false
    }
    
    /**
     * Get relative time string (e.g. "5 minutes ago")
     */
    private fun getRelativeTime(date: Date): String {
        val now = System.currentTimeMillis()
        val time = date.time
        
        return DateUtils.getRelativeTimeSpanString(
            time, now, DateUtils.MINUTE_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE
        ).toString()
    }
    
    /**
     * Get source text based on source field and mode
     */
    private fun getSourceText(post: PostHistoryEntry): String {
        val appName = if (post.isDarkMode) "PostDemon" else "PostAngel"
        
        val sourceText = when (post.source) {
            PostHistoryEntry.SOURCE_SHARE -> context.getString(R.string.source_share)
            PostHistoryEntry.SOURCE_CREATE -> context.getString(R.string.source_create)
            else -> context.getString(R.string.source_create)
        }
        
        return "$sourceText • ${context.getString(R.string.created_with, appName)}"
    }
    
    /**
     * Copy text to clipboard
     */
    private fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Post Content", text)
        clipboard.setPrimaryClip(clip)
    }
}
