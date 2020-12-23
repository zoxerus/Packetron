package com.tetron.packetron.ui.saved_conversations

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.tetron.packetron.R
import kotlinx.android.synthetic.main.message_template_recycleview_item.view.*


class StringAdapter internal constructor(
    private val context: Context,
    private val itemClickListener: (String) -> Unit,
    private val contextMenuItemListener: (String, Int) -> Unit
) : RecyclerView.Adapter<StringAdapter.MessageViewHolder>() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private var messages = emptyList<String>()
    val checkedMessages = ArrayList<String>()


    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val msgItemView = itemView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val itemView = inflater.inflate(R.layout.message_template_recycleview_item, parent, false)
        return MessageViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val current = messages[position]
        holder.msgItemView.tv_message_templates_message.text = current
        holder.msgItemView.cb_message_templates_checkbox.isChecked = false
        holder.msgItemView.tv_message_templates_message.setOnClickListener {
            itemClickListener(current)
        }
        holder.msgItemView.tv_message_templates_message.setOnLongClickListener { view ->
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
        holder.msgItemView.cb_message_templates_checkbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                checkedMessages.add(current)
            } else {
                checkedMessages.remove(current)
            }
        }
    }

    override fun getItemCount() = messages.size
}

