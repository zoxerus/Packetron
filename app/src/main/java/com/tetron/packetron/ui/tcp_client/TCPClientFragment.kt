package com.tetron.packetron.ui.tcp_client

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tetron.packetron.ProtocolMessage
import com.tetron.packetron.R
import com.tetron.packetron.ui.ConnectionDialog
import com.tetron.packetron.ui.ConnectionViewModel
import com.tetron.packetron.ui.MessageDialog
import com.tetron.packetron.ui.ResponseAdapter
import com.tetron.packetron.ui.udp_send_receive.LOG_TAG
import kotlinx.android.synthetic.main.fragment_tcp_client.*
import kotlinx.android.synthetic.main.fragment_tcp_client.view.*
import java.net.Socket


class TCPClientFragment(vm: ConnectionViewModel) : Fragment() {
    private val tcpClientViewModel = vm

    private var ipPref: SharedPreferences? = null
    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var recyclerView: RecyclerView
    private lateinit var responseAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager

    constructor() : this(ConnectionViewModel())

    companion object {
        fun newInstance(vm: ConnectionViewModel): TCPClientFragment {
            return TCPClientFragment(vm)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        retainInstance = true
        ipPref = activity?.getSharedPreferences(
            "ip_preferences", Context.MODE_PRIVATE
        )
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
    }

    override fun onResume() {
        super.onResume()
        requireActivity().title = "TCP Client"
        tcpClientViewModel.tcpClientAddress =
            ipPref!!.getString("client_address", "127.0.0.1:33333")!!
    }

    override fun onPause() {
        super.onPause()
        with(ipPref!!.edit()) {
            putString("client_address", tcpClientViewModel.tcpClientAddress)
            apply()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_tcp_client, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.send_button.setOnClickListener {
            val msg = message_to_send.text.toString()
            if (msg.isNotEmpty() &&
                tcpClientViewModel.tcpClientSocket != null &&
                tcpClientViewModel.tcpClientSocket!!.isConnected
            ) {
                Thread {
                    try {
                        tcpClientViewModel.tcpClientSocket!!.getOutputStream()
                            .write(msg.toByteArray())
                        if (sharedPreferences.getBoolean(
                                getString(R.string.tcp_client_show_sent),
                                false
                            )
                        ) {
                            val pm = ProtocolMessage(msg)
                            pm.messageIp = "//127.0.0.1"
                            pm.messagePort = tcpClientViewModel.tcpClientSocket!!.port.toString()
                            tcpClientViewModel.addTcpClientResponse(pm)
                        }
                    } catch (e: java.lang.Exception) {
                        e.printStackTrace()
                    }
                }.start()
                message_to_send.text = null
            }
        }

        viewManager = LinearLayoutManager(requireContext().applicationContext)
        responseAdapter = ResponseAdapter(tcpClientViewModel.tcpClientResponses) { pm ->
            MessageDialog(pm)
            {
                if (pm.socket != null && pm.socket.isBound) {
                    Thread {
                        try {
                            pm.socket.getOutputStream()
                                .write(it.messageText.toByteArray())
                            pm.socket.getOutputStream().flush()
                            if (sharedPreferences.getBoolean(
                                    getString(R.string.tcp_client_show_sent),
                                    false
                                )
                            ) {
                                val newPm = ProtocolMessage(it.messageText)
                                newPm.messageIp = "//127.0.0.1"
                                newPm.messagePort =
                                    tcpClientViewModel.tcpClientSocket!!.port.toString()
                                tcpClientViewModel.addTcpClientResponse(newPm)
                            }
                        } catch (e: Exception) {
                            requireActivity().runOnUiThread {
                                Toast.makeText(
                                    context,
                                    "Client Disconnected",
                                    Toast.LENGTH_LONG
                                )
                                    .show()
                            }
                        }
                    }.start()

                }
            }
                .showNow(requireActivity().supportFragmentManager, "Replay Message")
        }

        tcpClientViewModel.tcpClientResponsesLive.observe(
            viewLifecycleOwner,
            Observer<List<ProtocolMessage>> {
                recyclerView.scrollToPosition(tcpClientViewModel.tcpClientResponses.size - 1)
            })

        Log.e(LOG_TAG, "View Created")

        recyclerView = view.findViewById<RecyclerView>(R.id.response_recycler_view).apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = responseAdapter
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.findItem(R.id.action_connect).isVisible = true
        menu.findItem(R.id.action_clear_responses).isVisible = true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_connect -> {
                val udpConnectionDialog =
                    ConnectionDialog("Enter Remote IP and address separated by a colon ':' ",
                        tcpClientViewModel.tcpClientAddress,
                        tcpClientViewModel, { vm, button ->
                            if (vm.tcpClientSocket != null && vm.tcpClientSocket!!.isBound) {
                                button.isChecked = true
                            }
                        },
                        { vm, address, toggle, editText ->
                            if (sharedPreferences.getBoolean("tcp_client_remember_hosts", true)) {
                                tcpClientViewModel.tcpClientAddress = address
                            }

                            Log.e("TCP Client", " button changed status ")
                            toggle.isEnabled = false
                            if (vm.tcpClientSocket != null && vm.tcpClientSocket!!.isBound) {
                                try {
                                    vm.tcpClientSocket?.close()
                                    vm.tcpClientSocket = null
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            } else {
                                Thread {
                                    try {
                                        val ip = address.split(":").first()
                                        val port = address.split(":").last()
                                        vm.tcpClientSocket = Socket(ip, port.toInt())

                                    } catch (e: Exception) {
                                        vm.tcpClientSocket = null
                                        e.printStackTrace()
                                        requireActivity().runOnUiThread {
                                            toggle.isChecked = false
                                            editText.error = " Cannot connect "
                                            Toast.makeText(
                                                context,
                                                "check IP and Port",
                                                Toast.LENGTH_LONG
                                            )
                                                .show()
                                        }
                                    }
                                    val message = ByteArray(
                                        sharedPreferences.getString(
                                            getString(R.string.tcp_client_in_buffer), "255"
                                        )!!.toInt()
                                    )
                                    try {
                                        while (vm.tcpClientSocket!!.getInputStream()
                                                .read(message) != -1
                                        ) {

                                            vm.addTcpClientResponse(
                                                ProtocolMessage(String(message), vm.tcpClientSocket)
                                            )

                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                    try {
                                        vm.tcpClientSocket?.close()
                                        vm.tcpClientSocket = null
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }

                                }.start()
                            }
                            toggle.isEnabled = true
                        })
                udpConnectionDialog.showNow(
                    requireActivity().supportFragmentManager,
                    "Connection Dialog"
                )
            }
            R.id.action_clear_responses -> {
                tcpClientViewModel.tcpClientResponses.clear()
                responseAdapter.notifyDataSetChanged()
            }

        }
        return super.onOptionsItemSelected(item)
    }

}