package com.tetron.packetron.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.tetron.packetron.R
import com.tetron.packetron.db.conversations.ConversationMessage
import kotlinx.android.synthetic.main.recycler_view_packet_received.view.*

class ResponseAdapter(
    private var mDataSet: List<ConversationMessage>,
    val messageClickListener: (ConversationMessage) -> Unit
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    class ReceivedViewHolder(val inMsg: LinearLayout) : RecyclerView.ViewHolder(inMsg)
    class SentViewHolder(val outMsg: ConstraintLayout) : RecyclerView.ViewHolder(outMsg)


    override fun getItemCount() = mDataSet.size
    fun getAll(): List<ConversationMessage> {
        return mDataSet
    }

    fun addAll(conversations: List<ConversationMessage>) {
        mDataSet = conversations
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        val item = mDataSet[position]
        when (holder.itemViewType) {
            1 -> {
                val receivedViewHolder = holder as ReceivedViewHolder
                receivedViewHolder.inMsg.address_view.text = item.addressToString()
                receivedViewHolder.inMsg.text_view.text = item.message
                receivedViewHolder.inMsg.setOnClickListener {
                    messageClickListener(item)
                }
            }
            0 -> {
                val sentViewHolder = holder as SentViewHolder
                sentViewHolder.outMsg.address_view.text = item.addressToString()
                sentViewHolder.outMsg.text_view.text = item.message
                sentViewHolder.outMsg.setOnClickListener {
                    messageClickListener(item)
                }
            }

        }

    }

    override fun getItemViewType(position: Int): Int {
        if (mDataSet[position].direction == 1) {
            return 1
        }
        return 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return if (viewType == 1) {
            val view = layoutInflater
                .inflate(R.layout.recycler_view_packet_received, parent, false) as LinearLayout
            ReceivedViewHolder(view)
        } else {
            val view = layoutInflater
                .inflate(R.layout.recycler_view_packet_sent, parent, false) as ConstraintLayout
            SentViewHolder(view)
        }


    }
}
