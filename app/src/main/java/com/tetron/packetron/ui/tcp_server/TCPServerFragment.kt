package com.tetron.packetron.ui.tcp_server

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ArrayAdapter
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
import kotlinx.android.synthetic.main.fragment_tcp_server.*
import kotlinx.android.synthetic.main.fragment_tcp_server.view.*
import java.net.ServerSocket
import java.net.Socket

class TCPServerFragment(vm: ConnectionViewModel) : Fragment() {
    private val tcpViewModel = vm

    private lateinit var recyclerView: RecyclerView
    private lateinit var responseAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager

    private var ipPref: SharedPreferences? = null
    private var sharedPreferences: SharedPreferences? = null


    private lateinit var tcpClientAdapter: ArrayAdapter<Socket>

    constructor() : this(ConnectionViewModel())

    companion object {
        fun newInstance(vm: ConnectionViewModel): TCPServerFragment {
            return TCPServerFragment(vm)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        retainInstance = true
        ipPref = activity?.getSharedPreferences("ip_preferences", Context.MODE_PRIVATE)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
    }

    override fun onResume() {
        super.onResume()
        requireActivity().title = "TCP Server"
        tcpViewModel.localTcpPort = ipPref!!.getString("local_port", "33333")!!
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

        view.send_button.setOnClickListener {
            val pos = tcp_address_spinner.selectedItemPosition
            Log.e("pos ", pos.toString())
            if (pos == -1) {
                Toast.makeText(requireContext(), "Select Client First", Toast.LENGTH_LONG).show()
            } else {
                val soc = tcpClientAdapter.getItem(pos)
                val msg = message_to_send.text.toString()
                if (msg.isNotEmpty() && soc != null && soc.isConnected) {
                    Thread {

                        try {
                            soc.getOutputStream().write(msg.toByteArray())
                            if (sharedPreferences!!.getBoolean(
                                    getString(R.string.tcp_server_show_sent),
                                    false
                                )
                            ) {
                                val pm = ProtocolMessage(msg)
                                pm.messageIp = "//127.0.0.1"
                                pm.messagePort = tcpViewModel.localTcpPort
                                tcpViewModel.addTcpServerResponse(pm)
                            }

                        } catch (e: java.lang.Exception) {
                            e.printStackTrace()
                        }
                    }.start()
                    message_to_send.text = null
                }
            }
        }
        val tcpClients = ArrayList<Socket>()
        tcpClientAdapter = ArrayAdapter(
            requireActivity(),
            android.R.layout.simple_dropdown_item_1line,
            tcpClients
        )
        tcp_address_spinner.adapter = tcpClientAdapter


        viewManager = LinearLayoutManager(requireContext().applicationContext)
        responseAdapter = ResponseAdapter(tcpViewModel.tcpServerResponses) { pm ->
            MessageDialog(pm)
            {
                if (pm.socket != null && pm.socket.isBound) {
                    Thread {
                        try {
                            pm.socket.getOutputStream()
                                .write(it.messageText.toByteArray())
                            pm.socket.getOutputStream().flush()
                            if (sharedPreferences!!.getBoolean(
                                    getString(R.string.tcp_server_show_sent),
                                    false
                                )
                            ) {
                                val newPm = ProtocolMessage(it.messageText)
                                newPm.messageIp = "//127.0.0.1"
                                newPm.messagePort = tcpViewModel.localTcpPort
                                tcpViewModel.addTcpServerResponse(pm)
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

        tcpViewModel.tcpServerResponsesLive.observe(
            viewLifecycleOwner,
            Observer<List<ProtocolMessage>> {
                recyclerView.scrollToPosition(tcpViewModel.tcpServerResponses.size - 1)
            })
        tcpViewModel.tcpClientsLive.observe(
            viewLifecycleOwner,
            Observer<List<Socket>> {
                tcpClients.clear()
                tcpClients.addAll(tcpViewModel.tcpClients)
                tcpClientAdapter.notifyDataSetChanged()
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
                    ConnectionDialog(
                        "Enter Local Port Number",
                        tcpViewModel.localTcpPort,
                        tcpViewModel,
                        { vm, button ->
                            if (vm.tcpServerSocket != null && vm.tcpServerSocket!!.isBound) {
                                button.isChecked = true
                            }
                        },
                        { vm, port, toggle, editText ->

                            Log.e("TCP SERVER", " button changed status ")
                            toggle.isEnabled = false
                            if (vm.tcpServerSocket != null && vm.tcpServerSocket!!.isBound) {
                                try {
                                    vm.tcpServerSocket!!.close()
                                    vm.tcpServerSocket = null
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            } else {
                                Thread {
                                    try {
                                        vm.tcpServerSocket = ServerSocket(port.toInt())
                                    } catch (e: Exception) {
                                        vm.tcpServerSocket = null
                                        vm.tcpClients.clear()
                                        e.printStackTrace()
                                        requireActivity().runOnUiThread {
                                            toggle.isChecked = false
                                            editText.error = " Cannot bind to port "
                                            Toast.makeText(
                                                context,
                                                "Error",
                                                Toast.LENGTH_LONG
                                            )
                                                .show()
                                        }
                                    }
                                    do {
                                        try {
                                            val client: Socket? = vm.tcpServerSocket?.accept()
                                                if (
                                                    client != null
                                                    && tcpClientAdapter.getPosition(client) == -1
                                                ) {
                                                    vm.updateTcpClients(client, 0)
                                                }

                                            val clientHandler =
                                                TCPClientHandler(
                                                    tcpViewModel,
                                                    sharedPreferences!!.getString(
                                                        getString(R.string.tcp_server_in_buffer),
                                                        "255"
                                                    )!!.toInt(),
                                                    client!!
                                                )
                                            clientHandler.start()


                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    } while (vm.tcpServerSocket != null && vm.tcpServerSocket!!.isBound)
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
                tcpViewModel.tcpServerResponses.clear()
                responseAdapter.notifyDataSetChanged()
            }

        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPause() {
        super.onPause()
        with(ipPref!!.edit()) {
            putString("local_port", tcpViewModel.localTcpPort)
            apply()
        }
    }


}