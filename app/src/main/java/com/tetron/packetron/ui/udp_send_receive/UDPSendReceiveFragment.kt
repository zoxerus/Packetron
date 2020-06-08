package com.tetron.packetron.ui.udp_send_receive

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
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
import kotlinx.android.synthetic.main.fragment_udp_send_receive.*
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException

const val LOG_TAG: String = "UDP_SEND_RECEIVE"

class UDPSendReceiveFragment(vm: ConnectionViewModel) : Fragment(),
    SharedPreferences.OnSharedPreferenceChangeListener, View.OnClickListener {

    private val udpViewModel = vm

    private var stopResends = false

    private var ipPref: SharedPreferences? = null

    private var preferences: SharedPreferences? = null

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
        setHasOptionsMenu(true)
        retainInstance = true
        preferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        preferences!!.registerOnSharedPreferenceChangeListener(this)
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        requireActivity().title = "UDP Sender Receiver"
        udpViewModel.localPort = ipPref!!.getString("local_port", "33333")!!
        if (preferences!!.getBoolean(getString(R.string.udp_send_from_server), true)) {
            out_port_num.setText(udpViewModel.localPort)
            out_port_num.isEnabled = false
        }
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
        if (ipPref!!.getStringSet("addresses", null) != null) {
            udpViewModel.addresses.clear()
            udpViewModel.addresses.addAll(
                ipPref!!.getStringSet(
                    "addresses",
                    setOf("127.0.0.1:33333")
                )!!.toMutableList()
            )
        }
        addressAdapter = ArrayAdapter(
            requireContext(),
            R.layout.spinner_dropdown, udpViewModel.addresses
        )

        remoteHost.setAdapter(addressAdapter)

        viewManager = LinearLayoutManager(requireContext().applicationContext)
        viewAdapter = ResponseAdapter(udpViewModel.udpResponses) { item ->
            MessageDialog(item)
            {
                message_to_send.text = null
                if (it.messageIp.startsWith("//")) {
                    it.messageIp = it.messageIp.removePrefix("/")
                }
                Thread {
                    sendUdpPacket(
                        true,
                        it.messageIp.removePrefix("/"),
                        it.messagePort,
                        it.messageText
                    )

                }.start()
            }
                .showNow(requireActivity().supportFragmentManager, "Replay Message")
        }

        udpViewModel.udpResponsesLive.observe(
            viewLifecycleOwner,
            Observer<List<ProtocolMessage>> {
                recyclerView.scrollToPosition(udpViewModel.udpResponses.size - 1)
            })

        Log.e(LOG_TAG, "View Created")

        recyclerView = view.findViewById<RecyclerView>(R.id.response_recycler_view).apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter

        }


        send_button.setOnClickListener(this)
        stop_resends.setOnClickListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {

        when {
            key.equals("udp_show_sent") -> {

            }
            key.equals("udp_remember_hosts") -> {
                if (!sharedPreferences!!.getBoolean(key, true)) {
                    ipPref!!.edit().remove("addresses").apply()
                    udpViewModel.addresses.clear()
                    addressAdapter.clear()
                    addressAdapter.notifyDataSetChanged()
                }
            }
            key.equals(getString(R.string.udp_send_from_server)) -> {
                if (!isVisible) {
                    return
                }
                if (sharedPreferences!!.getBoolean(key, true)) {
                    out_port_num.setText(udpViewModel.localPort)
                    out_port_num.isEnabled = false
                } else {
                    out_port_num.text = null
                    out_port_num.isEnabled = true
                }
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
                createConnectionDialog().showNow(
                    requireActivity().supportFragmentManager,
                    "Connection Dialog"
                )
            }
            R.id.action_clear_responses -> {
                udpViewModel.udpResponses.clear()
                viewAdapter.notifyDataSetChanged()
            }

        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPause() {
        super.onPause()

        with(ipPref!!.edit()) {
            putString("local_port", udpViewModel.localPort)
            putStringSet("addresses", udpViewModel.addresses.toSet())
            apply()
        }

    }

    private fun createConnectionDialog(): ConnectionDialog {
        return ConnectionDialog(
            "Enter Local Port Number",
            udpViewModel.localPort,
            udpViewModel,
            { vm, button ->
                if (vm.udpSocket != null && vm.udpSocket!!.isBound) {
                    button.isChecked = true
                }
            },
            { vm, port, toggle, editText ->
                if (vm.udpSocket != null && vm.udpSocket!!.isBound) {
                    vm.udpSocket?.close()
                    vm.udpSocket = null
                } else {
                    Thread {
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
                            }
                            try {
                                do {
                                    udpListen(udpViewModel.udpSocket)
                                } while (vm.udpSocket != null && vm.udpSocket!!.isBound)
                            } catch (e: java.lang.Exception) {

                            }
                        }

                    }.start()

                }
            })
    }


    private fun udpListen(soc: DatagramSocket?) {
        val msg = ByteArray(
            preferences!!
                .getString("udp_in_buffer", "255")!!.toInt()
        )
        val packet = DatagramPacket(msg, msg.size)
        soc?.receive(packet)
        val res =
            ProtocolMessage(
                String(msg)
            )
        res.messageIp = packet.address.toString()
        res.messagePort = packet.port.toString()
        udpViewModel.addUdpResponse(res)
    }


    private fun sendUdpPacket(fromServer: Boolean, vararg params: String?): Int {
        try {
            val msg = params[2]?.toByteArray()!!
            val port = params[1]!!.toInt()
            val receiverAddress = InetAddress.getByName(params[0])
            val packet = DatagramPacket(msg, msg.size, receiverAddress, port)
            if (fromServer) {
                if (udpViewModel.udpSocket != null && udpViewModel.udpSocket!!.isBound) {
                    udpViewModel.udpSocket!!.send(packet)
                    if (preferences!!.getBoolean("udp_show_sent", false)) {
                        val pm = ProtocolMessage(params[2]!!)
                        pm.messageIp = "//127.0.0.1"
                        pm.messagePort = udpViewModel.localPort
                        udpViewModel.addUdpResponse(pm)
                    }
                } else {
                    requireActivity().runOnUiThread {
                        Toast.makeText(
                            requireContext(),
                            "UDP Server not Running",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return 1
                }

            } else {
                val socket: DatagramSocket = if (params[3] == "" || params[3] == "0") {
                    DatagramSocket()
                } else {
                    DatagramSocket(params[3]!!.toInt())
                }
                socket.send(packet)
                if (preferences!!.getBoolean("udp_show_sent", false)) {
                    val pm = ProtocolMessage(params[2]!!)
                    pm.messageIp = "//127.0.0.1"
                    pm.messagePort = socket.localPort.toString()
                    udpViewModel.addUdpResponse(pm)
                }
                if (params[4] != "" && params[4] != "0") {
                    socket.soTimeout = params[4]!!.toInt()
                    udpListen(socket)
                }
                socket.close()
            }
            val address = remoteHost.text.toString()
            if (preferences!!.getBoolean("udp_remember_hosts", true)
                && !udpViewModel.addresses.contains(address)
            ) {
                udpViewModel.addresses.add(address)

                requireActivity().runOnUiThread {
                    addressAdapter.clear()
                    addressAdapter.addAll(udpViewModel.addresses)
                    addressAdapter.notifyDataSetChanged()
                }
            }
            return 0
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
            requireActivity().runOnUiThread {
                Toast.makeText(
                    requireContext(),
                    " Incorrect remote IP Address ",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: SocketTimeoutException) {
            e.printStackTrace()
            if (preferences!!.getBoolean("toast_not_received", false)) {
                requireActivity().runOnUiThread {
                    Toast.makeText(
                        requireContext(),
                        " Socket Timed Out",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            return 0
        } catch (e: IOException) {
            e.printStackTrace()
            requireActivity().runOnUiThread {
                Toast.makeText(
                    requireContext(),
                    "Address Error",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            requireActivity().runOnUiThread {
                Toast.makeText(
                    requireContext(),
                    " Error",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        return 1
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.send_button -> {
                val message = message_to_send.text.toString()
                if (message.isNotEmpty()) {
                    val address = remoteHost.text.toString()
                    val remoteIp = address
                        .split(":", ignoreCase = true, limit = 0)
                        .first()
                    val remotePort = address
                        .split(":", ignoreCase = true, limit = 0)
                        .last()
                    message_to_send.text = null
                    val resendDelay = repeat_ms.text.toString()
                    val outPort = out_port_num.text.toString()
                    val responseWait = response_wait_ms.text.toString()
                    val fromServer =
                        preferences!!.getBoolean(getString(R.string.udp_send_from_server), true)
                    if (resendDelay != "" && resendDelay != "0") {
                        if (responseWait != "" && responseWait != "0") {
                            response_wait_ms.text = null
                        }
                        repeat_ms.text = null
                        stopResends = false
                        stop_resends.visibility = View.VISIBLE
                        Thread {
                            var lastCall: Long = 0
                            while (!stopResends) {
                                if (System.currentTimeMillis() - lastCall >= resendDelay.toLong()) {
                                    lastCall = System.currentTimeMillis()
                                    if (sendUdpPacket(
                                            fromServer,
                                            remoteIp,
                                            remotePort,
                                            message,
                                            outPort,
                                            responseWait
                                        ) != 0
                                    ) {
                                        break
                                    }
                                }
                            }
                        }.start()
                    } else {
                        Thread {
                            sendUdpPacket(
                                fromServer,
                                remoteIp,
                                remotePort,
                                message,
                                out_port_num.text.toString(),
                                response_wait_ms.text.toString()
                            )
                        }.start()
                    }
                }
            }
            R.id.stop_resends -> {
                stopResends = true
            }
        }
    }


}