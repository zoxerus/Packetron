package com.tetron.packetron.ui

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.tetron.packetron.ConnectionUtils
import com.tetron.packetron.R
import com.tetron.packetron.db.conversations.ConversationMessage

class ResponseAdapter(
    private var mDataSet: List<ConversationMessage>,
    val messageClickListener: (ConversationMessage) -> Unit
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    class ReceivedViewHolder(val inMsg: LinearLayout) : RecyclerView.ViewHolder(inMsg)
    class SentViewHolder(val outMsg: ConstraintLayout) : RecyclerView.ViewHolder(outMsg)


    private val utils = ConnectionUtils()
    private var useHex = false

    override fun getItemCount() = mDataSet.size

    fun getAll(): List<ConversationMessage> {
        return mDataSet
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        val item = mDataSet[position]
        when (holder.itemViewType) {
            1 -> {
                val receivedViewHolder = holder as ReceivedViewHolder
                receivedViewHolder.inMsg.findViewById<TextView>(R.id.address_view).text = item.addressToString()
                if (useHex){
                    receivedViewHolder.inMsg.findViewById<TextView>(R.id.text_view).text = utils.charToHex( item.message.toCharArray() )
                } else{
                    receivedViewHolder.inMsg.findViewById<TextView>(R.id.text_view).text = item.message
                }
                receivedViewHolder.inMsg.setOnClickListener {
                    messageClickListener(item)
                }
            }
            0 -> {
                val sentViewHolder = holder as SentViewHolder
                sentViewHolder.outMsg.findViewById<TextView>(R.id.address_view).text = item.addressToString()
                if (useHex){
                    sentViewHolder.outMsg.findViewById<TextView>(R.id.text_view).text = utils.charToHex( item.message.toCharArray() )
                } else{
                    sentViewHolder.outMsg.findViewById<TextView>(R.id.text_view).text = item.message
                }
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

    fun setResponses(r:List<ConversationMessage>){
        mDataSet = r
        notifyDataSetChanged()
    }

    fun useHex(b: Boolean ){
        useHex = b
        notifyDataSetChanged()
    }
}
