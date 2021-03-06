package com.tetron.packetron.ui

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.tetron.packetron.R


class MessageDialog(
    var dialogueTitle: String,
    var positiveButtonText: String,
    val onPositiveButtonListener: (String) -> Unit
) : DialogFragment() {
    private lateinit var replayMessageET: EditText

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val dialogView = View.inflate(activity, R.layout.message_dialog, null)
            replayMessageET = dialogView.findViewById(R.id.replay_message)
            val builder = AlertDialog.Builder(it)
            builder.setTitle(dialogueTitle)
            builder.setView(dialogView)
            builder.setNegativeButton("Cancel", null)
            builder.setPositiveButton(positiveButtonText) { _, _ ->
                onPositiveButtonListener( replayMessageET.text.toString() )
            }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}

