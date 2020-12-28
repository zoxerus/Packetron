package com.tetron.packetron.ui.saved_conversations

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.tetron.packetron.R
import com.tetron.packetron.db.conversations.ConversationsTable
import kotlinx.android.synthetic.main.message_template_recycleview_item.view.*


class StringAdapter internal constructor(
    private val context: Context,
    private val itemClickListener: (ConversationsTable) -> Unit
) : RecyclerView.Adapter<StringAdapter.MessageViewHolder>() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private var conversations = emptyList<ConversationsTable>()
    val checkedMessages = ArrayList<Int>()


    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val msgItemView = itemView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val itemView = inflater.inflate(R.layout.message_template_recycleview_item, parent, false)
        return MessageViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val current = conversations[position]
        holder.msgItemView.tv_message_templates_message.text = current.name
        holder.msgItemView.cb_message_templates_checkbox.isChecked = false
        holder.msgItemView.tv_message_templates_message.setOnClickListener {
            itemClickListener(current)
        }

        holder.msgItemView.cb_message_templates_checkbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                checkedMessages.add(current.id)
            } else {
                checkedMessages.remove(current.id)
            }
        }
    }

    fun setConversations(list:List<ConversationsTable>){
        conversations = list
        notifyDataSetChanged()
    }

    override fun getItemCount() = conversations.size

}

