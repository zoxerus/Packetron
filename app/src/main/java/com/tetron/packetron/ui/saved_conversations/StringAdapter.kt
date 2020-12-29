package com.tetron.packetron.ui.saved_conversations

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tetron.packetron.R
import com.tetron.packetron.db.conversations.ConversationsTable


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
        val msgTxtView = holder.msgItemView.findViewById<TextView>(R.id.tv_message_templates_message)
        val msgCheckBox = holder.msgItemView.findViewById<CheckBox>(R.id.cb_message_templates_checkbox)
        val current = conversations[position]
        msgTxtView.text = current.name
        msgCheckBox.isChecked = false
        msgTxtView.setOnClickListener {
            itemClickListener(current)
        }

        msgCheckBox.setOnCheckedChangeListener { _, isChecked ->
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

