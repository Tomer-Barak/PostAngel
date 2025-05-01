package com.postangel.screenshare

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TopicAdapter(
    private val topics: List<TopicFile>,
    private val onItemClick: (TopicFile) -> Unit,
    private val onDeleteClick: (TopicFile) -> Unit
) : RecyclerView.Adapter<TopicAdapter.TopicViewHolder>() {

    class TopicViewHolder(view: View) : RecyclerView.ViewHolder(view) {
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
        
        // Remove .txt extension for display
        val displayName = if (topic.name.endsWith(".txt", ignoreCase = true)) {
            topic.name.substring(0, topic.name.length - 4)
        } else {
            topic.name
        }
        
        holder.textViewTopicName.text = displayName
        
        // Generate a preview of content (first 100 chars)
        val preview = if (topic.content.length > 100) {
            topic.content.substring(0, 100) + "..."
        } else {
            topic.content
        }
        holder.textViewTopicPreview.text = preview
        
        // Set click listeners
        holder.itemView.setOnClickListener { onItemClick(topic) }
        holder.buttonDelete.setOnClickListener { onDeleteClick(topic) }
    }

    override fun getItemCount() = topics.size
}
