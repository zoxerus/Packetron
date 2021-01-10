package com.tetron.packetron.ui

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ToggleButton
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.tetron.packetron.R

class ConnectionDialog(
    private val title: String,
    private val address: String?,
    vm: ConnectionViewModel,
    button: (ConnectionViewModel, ToggleButton) -> Unit,
    private val connect: (ConnectionViewModel, String, ToggleButton, EditText) -> Unit
) : DialogFragment() {

    private val connectionViewModel = vm
    private val setButton = button

    private lateinit var localPortET: EditText

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
           val dialogView = View.inflate(activity, R.layout.dialog_connection, null)

            localPortET = dialogView.findViewById(R.id.local_port)

            val builder = AlertDialog.Builder(it)
            builder.setTitle(title)
            builder.setNegativeButton("OK") { _, _ ->
            }
            builder.setView(dialogView)
            localPortET.setText(address)

            val connectToggle: ToggleButton = dialogView!!.findViewById(R.id.button_connect)
            connectToggle.text = getString(R.string.text_connect)
            connectToggle.textOff = getString(R.string.text_connect)
            connectToggle.textOn = getString(R.string.text_disconnect)

            setButton(connectionViewModel, connectToggle)

            connectToggle.setOnCheckedChangeListener { _, _ ->
                connect(
                    connectionViewModel,
                    localPortET.text.toString(),
                    connectToggle,
                    localPortET )
            }

            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

}