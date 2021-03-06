package com.tetron.packetron.ui.message_templates

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.tetron.packetron.R
import com.tetron.packetron.db.templates.MessageTemplate


class MessageAdapter internal constructor(
    private val context: Context,
    private val itemClickListener: (MessageTemplate) -> Unit,
    private val contextMenuItemListener: (MessageTemplate, Int) -> Unit
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private var messages = emptyList<MessageTemplate>()
    val checkedMessages = ArrayList<Long>()


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
        val current = messages[position]
        msgTxtView.text = current.message
        msgCheckBox.isChecked = false
        holder.msgItemView.findViewById<TextView>(R.id.tv_message_templates_message).setOnClickListener {
            itemClickListener(current)
        }
        msgTxtView.setOnLongClickListener { view ->
            val pop = PopupMenu(context, view)
            pop.inflate(R.menu.message_template_context)
            pop.setOnMenuItemClickListener { item ->

                when (item.itemId) {
                    R.id.cm_message_templates_action_delete -> {
                        contextMenuItemListener(current, 0)
                    }
                    R.id.cm_message_templates_action_edit -> {
                        contextMenuItemListener(current, 1)
                    }
                    R.id.cm_message_templates_action_copy -> {
                        contextMenuItemListener(current, 2)
                    }
                }
                true
            }
            pop.show()
            true
        }
        msgCheckBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                checkedMessages.add(current.id)
            } else {
                checkedMessages.remove(current.id)
            }
        }
    }


    internal fun setMessages(messages: List<MessageTemplate>) {
        this.messages = messages
        notifyDataSetChanged()
    }


    override fun getItemCount() = messages.size
}

