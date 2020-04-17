package com.tetron.packetron.ui.udp_send_receive

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.ToggleButton
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.tetron.packetron.R
import kotlinx.android.synthetic.main.dialog_udp_connection.view.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketTimeoutException

class UDPConnectionDialog(vm: UDPViewModel) : DialogFragment() {

    private val udpViewModel = vm
    private var dialogView: View? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            dialogView = View.inflate(activity, R.layout.dialog_udp_connection, null)
            val builder = AlertDialog.Builder(it)
            builder.setTitle("UDP Connection")
            builder.setNegativeButton("Save") { _, _ ->
                udpViewModel.localPort = dialogView!!.local_port.text.toString()
                udpViewModel.remotePort = dialogView!!.remote_port.text.toString()
                udpViewModel.remoteIp = dialogView!!.remote_ip.text.toString()
            }
            builder.setView(dialogView)
            dialogView!!.remote_ip.setText(udpViewModel.remoteIp)
            dialogView!!.remote_port.setText(udpViewModel.remotePort)
            dialogView!!.local_port.setText(udpViewModel.localPort)

            val connectToggle: ToggleButton = dialogView!!.findViewById(R.id.button_connect)
            connectToggle.text = getString(R.string.text_connect)
            connectToggle.textOff = getString(R.string.text_connect)
            connectToggle.textOn = getString(R.string.text_disconnect)

            if (udpViewModel.udpSocket != null && udpViewModel.udpSocket!!.isBound) {
                dialogView!!.button_connect.isChecked = true
            }

            connectToggle.setOnCheckedChangeListener { _, _ ->
                if (udpViewModel.udpSocket != null && udpViewModel.udpSocket!!.isBound) {
                    udpViewModel.udpSocket?.close()
                    udpViewModel.udpSocket = null
                } else {
                    val localPort = dialogView!!.local_port.text.toString()
                    Thread {
                        do {
                            val msg = ByteArray(2048)
                            val packet = DatagramPacket(msg, msg.size)
                            if (udpViewModel.udpSocket == null || !udpViewModel.udpSocket!!.isBound) {
                                try {
                                    udpViewModel.udpSocket = DatagramSocket(localPort.toInt())
                                    udpViewModel.udpSocket?.soTimeout = 0
                                } catch (e: Exception) {
                                    requireActivity().runOnUiThread {
                                        connectToggle.isChecked = false
                                        dialogView!!.local_port.error = "Port Unavailable"
                                    }
                                    e.printStackTrace()
                                    break
                                }

                            } else {

                                try {
                                    udpViewModel.udpSocket?.receive(packet)
                                    val res: String =
                                        packet.address.toString() + ":" + packet.port.toString() + " " + String(
                                            msg
                                        )
                                    udpViewModel.responses.add(0, res)
                                    udpViewModel.loadResponses()
                                } catch (e: SocketTimeoutException) {
                                    e.printStackTrace()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        } while (udpViewModel.udpSocket != null && udpViewModel.udpSocket!!.isBound)
                        udpViewModel.udpSocket?.close()
                    }.start()

                }
            }

            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}