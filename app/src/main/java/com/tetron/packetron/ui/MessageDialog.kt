package com.tetron.packetron.ui

import android.app.Dialog
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.tetron.packetron.R
import kotlinx.android.synthetic.main.message_dialog.view.*


class MessageDialog(
    var dialogueTitle: String,
    var positiveButtonText: String,
    val onPositiveButtonListener: (String) -> Unit
) : DialogFragment() {
    private var dialogView: View? = null
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            dialogView = View.inflate(activity, R.layout.message_dialog, null)
            val builder = AlertDialog.Builder(it)
            builder.setTitle(dialogueTitle)
            builder.setView(dialogView)
            builder.setNegativeButton("Cancel", null)
            builder.setPositiveButton(positiveButtonText) { _, _ ->
                onPositiveButtonListener(dialogView!!.replay_message.text.toString())
            }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}

