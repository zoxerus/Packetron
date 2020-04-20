package com.tetron.packetron.ui.tcp_server

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
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
import java.net.ServerSocket
import java.net.Socket

class TCPServerFragment(vm: ConnectionViewModel) : Fragment() {
    private val tcpViewModel = vm

    private lateinit var recyclerView: RecyclerView
    private lateinit var responseAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager

    private lateinit var clientAdapter: ArrayAdapter<Socket>

    companion object {
        fun newInstance(vm: ConnectionViewModel): TCPServerFragment {
            return TCPServerFragment(vm)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onResume() {
        super.onResume()
        requireActivity().title = "TCP Server"
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
        clientAdapter = ArrayAdapter(requireActivity(), android.R.layout.simple_dropdown_item_1line)
        tcp_address_spinner.adapter = clientAdapter
        send_button.setOnClickListener {
            val pos = tcp_address_spinner.selectedItemPosition
            val soc = clientAdapter.getItem(pos)
            val msg = message_to_send.text.toString().toByteArray()
            if (msg.isNotEmpty() && soc != null && soc.isConnected) {
                Thread {
                    try {
                        soc.getOutputStream().write(msg)
                    } catch (e: java.lang.Exception) {
                        e.printStackTrace()
                    }
                }.start()
                message_to_send.text = null
            }
        }

        viewManager = LinearLayoutManager(requireContext().applicationContext)
        responseAdapter = ResponseAdapter(tcpViewModel.tcpResponses) { pm ->
            MessageDialog(pm)
            {
                if (pm.socket != null && pm.socket.isBound) {
                    Thread {
                        try {
                            pm.socket.getOutputStream()
                                .write(it.toString().toByteArray())
                            pm.socket.getOutputStream().flush()
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

        tcpViewModel.tcpResponsesLive.observe(
            viewLifecycleOwner,
            Observer<List<ProtocolMessage>> { _ ->
                responseAdapter.notifyDataSetChanged()
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
                    ConnectionDialog(tcpViewModel, { vm, button ->
                        if (vm.tcpSocket != null && vm.tcpSocket!!.isBound) {
                            button.isChecked = true
                        }
                    },
                        { vm, port, toggle, editText ->

                            Log.e("TCP SERVER", " button changed status ")
                            toggle.isEnabled = false
                            if (vm.tcpSocket != null && vm.tcpSocket!!.isBound) {
                                try {
                                    vm.tcpSocket?.close()
                                    vm.tcpSocket = null
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            } else {
                                Thread {
                                    try {
                                        vm.tcpSocket = ServerSocket(port.toInt())
                                    } catch (e: Exception) {
                                        vm.tcpSocket = null
                                        e.printStackTrace()
                                        requireActivity().runOnUiThread {
                                            toggle.isChecked = false
                                            editText.error = " Cannot bind to port "
                                            Toast.makeText(
                                                context,
                                                "Cannot bind to port",
                                                Toast.LENGTH_LONG
                                            )
                                                .show()
                                        }
                                    }
                                    do {
                                        try {
                                            val client: Socket? = vm.tcpSocket?.accept()
                                            val clientHandler = TCPClientHandler(client!!) {
                                                requireActivity().runOnUiThread {
                                                    vm.tcpResponses.add(0, it)
                                                    vm.loadTcpResponses()
                                                    if (clientAdapter.getPosition(client) == -1) {
                                                        clientAdapter.add(client)
                                                    }
                                                }
                                            }
                                            clientHandler.run()


                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    } while (vm.tcpSocket != null && vm.tcpSocket!!.isBound)
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
                tcpViewModel.udpResponses.clear()
                tcpViewModel.loadUdpResponses()
            }

        }
        return super.onOptionsItemSelected(item)
    }

}