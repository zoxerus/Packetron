package com.tetron.packetron.ui

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.tetron.packetron.ProtocolMessage
import com.tetron.packetron.R
import kotlinx.android.synthetic.main.message_dialog.view.*


class MessageDialog(pm: ProtocolMessage, lsnr: (ProtocolMessage) -> Unit) : DialogFragment() {
    private val protocolMessage = pm
    private var dialogView: View? = null
    private val listener = lsnr
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        retainInstance = true
        return activity?.let {
            dialogView = View.inflate(activity, R.layout.message_dialog, null)
            val builder = AlertDialog.Builder(it)
            builder.setTitle("UDP Connection")
            builder.setView(dialogView)
            builder.setNegativeButton("Cancel", null)
            builder.setPositiveButton("Send") { _, _ ->
                listener(
                    ProtocolMessage(
                        protocolMessage.messageIp,
                        protocolMessage.messagePort,
                        dialogView!!.replay_message.text.toString()
                    )
                )
            }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}

