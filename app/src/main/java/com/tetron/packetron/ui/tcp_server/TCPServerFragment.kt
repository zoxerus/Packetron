package com.tetron.packetron.ui.tcp_server

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.widget.ToggleButton
import androidx.fragment.app.Fragment
import com.tetron.packetron.R
import kotlinx.android.synthetic.main.fragment_tcp_server.*
import java.net.ServerSocket

class TCPServerFragment : Fragment() {
    private var tcpServer: ServerSocket? = null

    companion object {
        private var tcpClients: ArrayList<TCPClientHandler> = ArrayList()
        fun newInstance(): TCPServerFragment {
            return TCPServerFragment()
        }

        fun addClient(client: TCPClientHandler) {
            tcpClients.add(client)
        }

        fun removeClient(client: TCPClientHandler) {
            tcpClients.remove(client)
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_tcp_server, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        val connectToggle: ToggleButton = view.findViewById(R.id.tcp_button_connect)
        connectToggle.text = getString(R.string.text_connect)
        connectToggle.textOff = getString(R.string.text_connect)
        connectToggle.textOn = getString(R.string.text_disconnect)


        connectToggle.setOnCheckedChangeListener { _, _ ->
            Log.e("TCP SERVER", " button changed status ")
            connectToggle.isEnabled = false
            if (tcpServer != null && tcpServer!!.isBound) {
                try {
                    tcpServer?.close()
                    tcpServer = null
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                val portNumString = tcp_local_port.text.toString()
                Thread {
                    try {
                        tcpServer = ServerSocket(portNumString.toInt())
                    } catch (e: Exception) {
                        tcpServer = null
                        e.printStackTrace()
                        activity!!.runOnUiThread {
                            connectToggle.isChecked = false
                            tcp_local_port.error = " Cannot bind to port "
                            Toast.makeText(context, "Cannot bind to port", Toast.LENGTH_LONG)
                                .show()
                        }
                    }
                    do {
                        try {
                            val client = tcpServer?.accept()
                            val clientHandler = TCPClientHandler(client!!)
                            clientHandler.run()

                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    } while (tcpServer != null && tcpServer!!.isBound)
                }.start()

            }
            connectToggle.isEnabled = true
        }


    }

}