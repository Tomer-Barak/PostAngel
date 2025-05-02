package com.postangel.screenshare

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

class TopicAdapter(
    private val topics: List<TopicFile>,
    private val onItemClick: (TopicFile) -> Unit,
    private val onDeleteClick: (TopicFile) -> Unit,
    private val context: android.content.Context
) : RecyclerView.Adapter<TopicAdapter.TopicViewHolder>() {

    class TopicViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardViewTopic: CardView = view.findViewById(R.id.cardViewTopic)
        val textViewTopicName: TextView = view.findViewById(R.id.textViewTopicName)
        val textViewTopicPreview: TextView = view.findViewById(R.id.textViewTopicPreview)
        val buttonDelete: ImageButton = view.findViewById(R.id.buttonDeleteTopic)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopicViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_topic, parent, false)
        return TopicViewHolder(view)
    }    
    
    override fun onBindViewHolder(holder: TopicViewHolder, position: Int) {
        val topic = topics[position]
        val isMarkdown = topic.name.endsWith(".md", ignoreCase = true)
        
        // Remove extension for display (.txt or .md)
        val displayName = when {
            topic.name.endsWith(".txt", ignoreCase = true) -> {
                topic.name.substring(0, topic.name.length - 4)
            }
            isMarkdown -> {
                topic.name.substring(0, topic.name.length - 3)
            }
            else -> {
                topic.name
            }
        }
        
        // Add a markdown indicator for .md files
        if (isMarkdown) {
            holder.textViewTopicName.text = "$displayName (Markdown)"
        } else {
            holder.textViewTopicName.text = displayName
        }
          // Generate a preview of content (first 100 chars)
        val preview = if (topic.content.length > 100) {
            topic.content.substring(0, 100) + "..."
        } else {
            topic.content
        }
        
        // Always use plain text for preview, don't render markdown
        holder.textViewTopicPreview.text = preview
        
        // Set up click listeners
        
        // First, ensure the click events don't conflict
        holder.textViewTopicName.isClickable = false
        holder.textViewTopicPreview.isClickable = false
        
        // Set click listener on the card
        holder.cardViewTopic.setOnClickListener {
            onItemClick(topic)
        }        // Make sure delete button captures its own clicks
        holder.buttonDelete.setOnClickListener { 
            // Call the delete handler
            onDeleteClick(topic)
        }
    }

    override fun getItemCount() = topics.size
}
