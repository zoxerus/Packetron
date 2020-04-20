package com.tetron.packetron.ui.udp_send_receive

import android.content.Context
import android.content.SharedPreferences
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
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
import kotlinx.android.synthetic.main.fragment_udp_send_receive.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException

const val LOG_TAG: String = "UDP_SEND_RECEIVE"

class UDPSendReceiveFragment(vm: ConnectionViewModel) : Fragment() {

    private val udpViewModel = vm

    private var ipPref: SharedPreferences? = null

    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager


    private lateinit var addressAdapter: ArrayAdapter<String>
    private lateinit var remoteHost: AutoCompleteTextView

    constructor() : this(ConnectionViewModel())

    companion object {
        fun newInstance(vm: ConnectionViewModel): UDPSendReceiveFragment {
            return UDPSendReceiveFragment(vm)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        //retainInstance = true
        setHasOptionsMenu(true)
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        requireActivity().title = "UDP Sender Receiver"
        udpViewModel.localPort = ipPref!!.getString("local_port", "33333")
        if (ipPref!!.getStringSet("addresses", null) != null) {
            udpViewModel.addresses = ipPref!!.getStringSet("addresses", null)!!.toMutableList()
        }
        addressAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line, udpViewModel.addresses.distinct()
        )

        remoteHost.setAdapter(addressAdapter)

        super.onResume()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        ipPref = activity?.getSharedPreferences(
            "ip_preferences", Context.MODE_PRIVATE
        )
        return inflater.inflate(R.layout.fragment_udp_send_receive, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        remoteHost = view.findViewById(R.id.remote_address_and_port)

        viewManager = LinearLayoutManager(requireContext().applicationContext)
        viewAdapter = ResponseAdapter(udpViewModel.udpResponses) { itpm ->
            val d = MessageDialog(itpm)
            {
                SendPacketAsyncTask()
                    .execute(
                        it.messageIp.removePrefix("/"),
                        it.messagePort,
                        it.messageText
                    )
            }
                .showNow(requireActivity().supportFragmentManager, "Replay Message")
        }

        udpViewModel.udpResponsesLive.observe(
            viewLifecycleOwner,
            Observer<List<ProtocolMessage>> { _ ->
                viewAdapter.notifyDataSetChanged()
            })

        Log.e(LOG_TAG, "View Created")

        recyclerView = view.findViewById<RecyclerView>(R.id.response_recycler_view).apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
        }

        send_button.setOnClickListener {
            val message = message_to_send.text.toString()
            if (message.isNotEmpty()) {
                val remoteIp = remote_address_and_port.text
                    .toString()
                    .split(":", ignoreCase = true, limit = 0)
                    .first()
                val remotePort = remote_address_and_port.text
                    .toString()
                    .split(":", ignoreCase = true, limit = 0)
                    .last()

                SendPacketAsyncTask().execute(remoteIp, remotePort, message)
            }

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
                    ConnectionDialog(udpViewModel, { vm, button ->
                        if (vm.udpSocket != null && vm.udpSocket!!.isBound) {
                            button.isChecked = true
                        }
                    }, { vm, port, toggle, editText ->
                        if (vm.udpSocket != null && vm.udpSocket!!.isBound) {
                            vm.udpSocket?.close()
                            vm.udpSocket = null
                        } else {
                            Thread {
                                do {
                                    val msg = ByteArray(2048)
                                    val packet = DatagramPacket(msg, msg.size)
                                    if (vm.udpSocket == null || !vm.udpSocket!!.isBound) {
                                        try {
                                            vm.udpSocket = DatagramSocket(port.toInt())
                                            vm.udpSocket?.soTimeout = 0
                                        } catch (e: Exception) {
                                            requireActivity().runOnUiThread {
                                                toggle.isChecked = false
                                                editText.error = "Port Unavailable"
                                            }
                                            e.printStackTrace()
                                            break
                                        }
                                    } else {

                                        try {
                                            vm.udpSocket?.receive(packet)
                                            val res =
                                                ProtocolMessage(
                                                    String(msg)
                                                )
                                            res.messageIp = packet.address.toString()
                                            res.messagePort = packet.port.toString()

                                            vm.udpResponses.add(0, res)
                                            vm.loadUdpResponses()
                                        } catch (e: SocketTimeoutException) {
                                            e.printStackTrace()
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                } while (vm.udpSocket != null && vm.udpSocket!!.isBound)
                                vm.udpSocket?.close()
                            }.start()

                        }
                    })



                udpConnectionDialog.showNow(
                    requireActivity().supportFragmentManager,
                    "Connection Dialog"
                )
            }
            R.id.action_clear_responses -> {
                udpViewModel.udpResponses.clear()
                udpViewModel.loadUdpResponses()
            }

        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPause() {
        super.onPause()

        with(ipPref!!.edit()) {
            putString("local_port", udpViewModel.localPort)
            putStringSet("addresses", udpViewModel.addresses.distinct().toSet())
            apply()
        }

        Log.e(LOG_TAG, "Paused")
    }


    inner class SendPacketAsyncTask : AsyncTask<String, Void, Boolean>() {

        override fun doInBackground(vararg params: String?): Boolean? {
            try {
                val msg = params[2]?.toByteArray()
                val port = params[1]!!.toInt()
                val receiverAddress = InetAddress.getByName(params[0])
                val packet = DatagramPacket(msg, msg!!.size, receiverAddress, port)
                if (udpViewModel.udpSocket != null && udpViewModel.udpSocket!!.isBound) {
                    udpViewModel.udpSocket?.send(packet)
                } else {
                    activity!!.runOnUiThread {
                        Toast.makeText(
                            activity,
                            " Not Connected, Connect First",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    return false
                }

            } catch (e: SocketTimeoutException) {
                e.printStackTrace()
                return false
            } catch (e: Exception) {
                activity!!.runOnUiThread {
                    Toast.makeText(
                        activity,
                        " Incorrect IP Address or Port unavailable ",
                        Toast.LENGTH_LONG
                    ).show()
                }

                e.printStackTrace()
                return false
            }
            return true
        }

        override fun onPostExecute(result: Boolean?) {
            if (result!!) {
                val address = remoteHost.text.toString()
                if (addressAdapter.getPosition(address) == -1) {
                    udpViewModel.addresses.add(address)
                    addressAdapter.add(address)
                }
                message_to_send.text = null
            }
        }

    }

/*    inner class UDPSocketAsyncTask : AsyncTask<String, Void, Void>() {


        override fun doInBackground(vararg params: String?): Void? {
            setThreadPriority(THREAD_PRIORITY_BACKGROUND + THREAD_PRIORITY_MORE_FAVORABLE)
            val msg = ByteArray(255)
            val packet = DatagramPacket(msg, msg.size)
            val port = params[0]!!.toInt()

            while (true) {
                try {
                    if (udpSocket == null || !udpSocket!!.isBound) {
                        udpSocket = DatagramSocket(port)
                        udpSocket?.soTimeout = 0
                        activity!!.runOnUiThread {
                            button_connect.text = "Disconnect"
                        }

                    } else {
                        udpSocket?.receive(packet)
                        activity!!.runOnUiThread {

                        }
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    udpSocket?.close()
                    udpSocket = null
                    return null
                }
            }
        }
    }*/


}