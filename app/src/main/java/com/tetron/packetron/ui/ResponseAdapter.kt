package com.tetron.packetron.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tetron.packetron.ProtocolMessage
import com.tetron.packetron.R

class ResponseAdapter(
    private val mDataSet: ArrayList<ProtocolMessage>,
    lsnr: (ProtocolMessage) -> Unit
) :
    RecyclerView.Adapter<ResponseAdapter.MyViewHolder>() {
    val listener = lsnr
    class MyViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)


    override fun getItemCount() = mDataSet.size

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val item = mDataSet[position]
        holder.textView.text = item.toString()
        holder.textView.setOnClickListener {
            listener(item)
        }

        if (position % 2 == 1) {
            holder.itemView.setBackgroundColor(Color.parseColor("#EFEFEF"))
            //  holder.imageView.setBackgroundColor(Color.parseColor("#FFFFFF"));
        } else {
            holder.itemView.setBackgroundColor(Color.parseColor("#DEDEDE"))
            //  holder.imageView.setBackgroundColor(Color.parseColor("#FFFAF8FD"));
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = layoutInflater
            .inflate(R.layout.recycler_view_item, parent, false) as TextView



        return MyViewHolder(view)

    }
}
