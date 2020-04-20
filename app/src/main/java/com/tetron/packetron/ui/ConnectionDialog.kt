package com.tetron.packetron.ui

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ToggleButton
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.tetron.packetron.R
import kotlinx.android.synthetic.main.dialog_udp_connection.view.*

class ConnectionDialog(
    vm: ConnectionViewModel,
    button: (ConnectionViewModel, ToggleButton) -> Unit,
    connect: (ConnectionViewModel, String, ToggleButton, EditText) -> Unit
) : DialogFragment() {

    private val connectionViewModel = vm
    private val setButton = button
    private val connect = connect
    private var dialogView: View? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        retainInstance = true
        return activity?.let {
            dialogView = View.inflate(activity, R.layout.dialog_udp_connection, null)

            val builder = AlertDialog.Builder(it)
            builder.setTitle("Listen on Port")
            builder.setNegativeButton("OK") { _, _ ->
                connectionViewModel.localPort = dialogView!!.local_port.text.toString()
            }
            builder.setView(dialogView)
            dialogView!!.local_port.setText(connectionViewModel.localPort)

            val connectToggle: ToggleButton = dialogView!!.findViewById(R.id.button_connect)
            connectToggle.text = getString(R.string.text_connect)
            connectToggle.textOff = getString(R.string.text_connect)
            connectToggle.textOn = getString(R.string.text_disconnect)

            setButton(connectionViewModel, connectToggle)

            connectToggle.setOnCheckedChangeListener { _, _ ->
                connect(
                    connectionViewModel,
                    dialogView!!.local_port.text.toString(),
                    connectToggle,
                    dialogView!!.local_port
                )
            }

            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

}