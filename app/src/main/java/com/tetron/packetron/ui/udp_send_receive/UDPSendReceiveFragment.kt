package com.tetron.packetron.ui.udp_send_receive

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tetron.packetron.ConnectionUtils
import com.tetron.packetron.R
import com.tetron.packetron.db.conversations.ConversationMessage
import com.tetron.packetron.db.conversations.ConversationViewModel
import com.tetron.packetron.db.conversations.ConversationsTable
import com.tetron.packetron.ui.ConnectionDialog
import com.tetron.packetron.ui.ConnectionViewModel
import com.tetron.packetron.ui.MessageDialog
import com.tetron.packetron.ui.ResponseAdapter
import com.tetron.packetron.ui.message_templates.SavedMessageActivity
import com.tetron.packetron.ui.saved_conversations.SavedConversationActivity
import java.io.IOException
import java.net.*

const val LOG_TAG: String = "UDP_SEND_RECEIVE"

class UDPSendReceiveFragment(vm: ConnectionViewModel) : Fragment(),
    SharedPreferences.OnSharedPreferenceChangeListener, View.OnClickListener, View.OnFocusChangeListener {

    /*/* Global Variables */*/
    private var stopResends = false

    /*/* Shared Preferences */*/
    private var ipPref: SharedPreferences? = null
    private var preferences: SharedPreferences? = null

    /*/* Adapters and Managers */*/
    private val utils = ConnectionUtils()
    private val udpViewModel = vm
    private lateinit var conversationViewModel: ConversationViewModel
    private lateinit var responseAdapter: ResponseAdapter
    private lateinit var viewManager: RecyclerView.LayoutManager
    private lateinit var addressAdapter: ArrayAdapter<String>

    /*/* Views Definition */*/
    private lateinit var outPortNumET: EditText
    private lateinit var outMessageET: EditText
    private lateinit var sendBTN: Button
    private lateinit var stopResendsBTN: Button
    private lateinit var waitMsET: EditText
    private lateinit var resendDelayMsET: EditText
    private lateinit var remoteHost: AutoCompleteTextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var hexInputET: EditText
    private lateinit var showAdvancedControlsCB: CheckBox
    private lateinit var showHexCB: CheckBox
    private lateinit var advancedControlsLayout: ConstraintLayout


    constructor() : this(ConnectionViewModel(Application()))

    companion object {
        fun newInstance(vm: ConnectionViewModel): UDPSendReceiveFragment {
            return UDPSendReceiveFragment(vm)
        }
    }

    // to get a message template from MessageTemplates Activity
    private val startForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                val intent = result.data
                udpViewModel.udpMessageToSend =
                    intent?.getStringExtra(getString(R.string.selected_message_template))!!
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        setHasOptionsMenu(true)
        preferences =
            PreferenceManager.getDefaultSharedPreferences(requireContext().applicationContext)
        preferences!!.registerOnSharedPreferenceChangeListener(this)
        conversationViewModel = ViewModelProvider(this).get(ConversationViewModel::class.java)

        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        requireActivity().title = "UDP Sender Receiver"

        udpViewModel.udpLocalPort = ipPref!!.getString("local_port", "33333")!!
        if (preferences!!.getBoolean(getString(R.string.udp_send_from_server), true)) {
            outPortNumET.setText(udpViewModel.udpLocalPort)
            outPortNumET.isEnabled = false
        }
        outMessageET.setText(udpViewModel.udpMessageToSend)
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
        /*/* Assign the Views */*/
        remoteHost = view.findViewById(R.id.remote_address_and_port)
        outPortNumET =  view.findViewById(R.id.out_port_num)
        outMessageET = view.findViewById(R.id.message_to_send)
        sendBTN = view.findViewById(R.id.send_button)
        stopResendsBTN = view.findViewById(R.id.stop_resends)
        waitMsET = view.findViewById(R.id.response_wait_ms)
        resendDelayMsET = view.findViewById(R.id.repeat_ms)
        hexInputET = view.findViewById(R.id.hex_input)
        showAdvancedControlsCB = view.findViewById(R.id.cb_show_advanced_controls)
        advancedControlsLayout = view.findViewById(R.id.advanced_controls_layout)
        showHexCB = view.findViewById(R.id.cb_show_hex)

        /*/* set listeners for the checkboxes */*/
        showAdvancedControlsCB.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                advancedControlsLayout.visibility = View.VISIBLE
            } else {
                advancedControlsLayout.visibility = View.GONE
            }
        }
        showHexCB.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked){
                responseAdapter.useHex(true)
                responseAdapter.setResponses( udpViewModel.udpResponses )

            } else {
                responseAdapter.useHex(false)
                responseAdapter.setResponses( udpViewModel.udpResponses )
            }
        }

        hexInputET.onFocusChangeListener = this
        outMessageET.onFocusChangeListener = this

        if (ipPref!!.getStringSet("addresses", null) != null) {
            udpViewModel.udpRemoteAddresses.clear()
            udpViewModel.udpRemoteAddresses.addAll(
                ipPref!!.getStringSet(
                    "addresses",
                    setOf("127.0.0.1:33333")
                )!!.toMutableList()
            )
        }

        // the spinner for selecting a pre-used address
        addressAdapter = ArrayAdapter(
            requireContext().applicationContext,
            R.layout.spinner_dropdown, udpViewModel.udpRemoteAddresses
        )
        remoteHost.setAdapter(addressAdapter)

        viewManager = LinearLayoutManager(requireContext().applicationContext)
        // specify the behaviour when the user clicks a message from the message list
        responseAdapter = ResponseAdapter( udpViewModel.udpResponses ) { item ->

            // specify the behaviour when user clicks the positive button of the reply dialogue
            MessageDialog("Enter a Reply", "Send") {
                outMessageET.text = null
                Thread {
                    sendUdpPacket(
                        fromServer = true,
                        message = it,
                        ip = item.remoteIp,
                        port = item.remotePort,
                        outPort = udpViewModel.udpLocalPort,
                        responseWait = ""
                    )
                }.start()
            }
                .showNow(requireActivity().supportFragmentManager, "Replay ConversationMessage")
        }

        udpViewModel.udpResponsesLive.observe(
            viewLifecycleOwner,
            {
                recyclerView.scrollToPosition(udpViewModel.udpResponses.size - 1)
            })

        recyclerView = view.findViewById<RecyclerView>(R.id.response_recycler_view).apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = responseAdapter

        }

        /*/* Set onClick Listeners */*/
        sendBTN.setOnClickListener(this)
        stopResendsBTN.setOnClickListener(this)
    }


    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {

        when {
            key.equals("udp_show_sent") -> {

            }
            key.equals("udp_remember_hosts") -> {
                if (!sharedPreferences!!.getBoolean(key, true)) {
                    ipPref!!.edit().remove("addresses").apply()
                    udpViewModel.udpRemoteAddresses.clear()
                    addressAdapter.clear()
                    addressAdapter.notifyDataSetChanged()
                }
            }
            key.equals(getString(R.string.udp_send_from_server)) -> {
                if (!isVisible) {
                    return
                }
                if (sharedPreferences!!.getBoolean(key, true)) {
                    outPortNumET.setText(udpViewModel.udpLocalPort)
                    outPortNumET.isEnabled = false
                } else {
                    outPortNumET.text = null
                    outPortNumET.isEnabled = true
                }
            }
        }
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
                responseAdapter.notifyDataSetChanged()
            }

            R.id.message_templates -> {
                startForResult.launch(Intent(activity, SavedMessageActivity::class.java))
            }

            R.id.mm_action_save_conversation -> {
                // specify the behaviour when user clicks the positive button of the reply dialogue
                MessageDialog("Enter a Name", "Save") {
                    if (udpViewModel.udpResponsesLive.value != null && udpViewModel.udpResponsesLive.value!!.isNotEmpty()){
                        val msgs = ConversationsTable(name = it,
                            fromTime = udpViewModel.udpResponsesLive.value!![0].timeId,
                            toTime = udpViewModel.udpResponsesLive.value!![udpViewModel.udpResponsesLive.value!!.size - 1].timeId
                        )
                        conversationViewModel.insertMany(responseAdapter.getAll())
                        conversationViewModel.insertConversation(msgs)
                    }
                }
                    .showNow(requireActivity().supportFragmentManager, "Conversation Name")
            }

            R.id.mm_action_show_conversation -> {
                val intent = Intent(activity, SavedConversationActivity::class.java)
                startActivity(intent)
            }

        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPause() {
        super.onPause()
        udpViewModel.udpMessageToSend = outMessageET.text.toString()
        with(ipPref!!.edit()) {
            putString("local_port", udpViewModel.udpLocalPort)
            putStringSet("addresses", udpViewModel.udpRemoteAddresses.toSet())
            apply()
        }

    }

    /*/* Handle Click Events */*/
    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.send_button -> {
                val message = outMessageET.text.toString()
                if (message.isNotEmpty()) {
                    val address = remoteHost.text.toString()
                    val remoteIp = address
                        .split(":", ignoreCase = true, limit = 0)
                        .first()
                    val remotePort = address
                        .split(":", ignoreCase = true, limit = 0)
                        .last()
                    outMessageET.text = null
                    val resendDelay = resendDelayMsET.text.toString()
                    val outPort = outPortNumET.text.toString()
                    val responseWait = waitMsET.text.toString()
                    val fromServer =
                        preferences!!.getBoolean(getString(R.string.udp_send_from_server), true)
                    if (resendDelay != "" && resendDelay != "0") {
                        if (responseWait != "" && responseWait != "0") {
                            waitMsET.text = null
                        }
                        resendDelayMsET.text = null
                        stopResends = false
                        stopResendsBTN.visibility = View.VISIBLE
                        Thread {
                            var lastCall: Long = 0
                            while (!stopResends) {
                                if (System.currentTimeMillis() - lastCall >= resendDelay.toLong()) {
                                    lastCall = System.currentTimeMillis()
                                    if (sendUdpPacket(
                                            fromServer,
                                            message,
                                            remoteIp,
                                            remotePort,
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
                                fromServer = fromServer,
                                message = message,
                                ip = remoteIp,
                                port = remotePort,
                                outPort = outPort,
                                responseWait = waitMsET.text.toString()
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

    override fun onFocusChange(v: View?, hasFocus: Boolean) {
        if (v == null) return
        when (v.id) {
            R.id.hex_input -> {
                if (!hasFocus) return
                if (outMessageET.text.toString() != "") {
                    val bytes = outMessageET.text.toString().toCharArray()
                    val hex = utils.charToHex(bytes)
                    hexInputET.setText(hex)
                }

            }
            R.id.message_to_send -> {
                if (!hasFocus) return
                if (hexInputET.text != null) {
                    var hex = hexInputET.text.toString().replace("\\s".toRegex(), "")
                    val output = StringBuilder()
                    var i = 0
                    if (hex.length % 2 !=0) { hex = "0$hex"
                    }
                    while (i < hex.length) {
                        val str: String = hex.substring(i, i + 2)
                        output.append(str.toInt(16).toChar())
                        i += 2
                    }
                    outMessageET.setText(output)
                }

            }
        }
    }

    /*/* show the connection Dialog   */*/
    private fun createConnectionDialog(): ConnectionDialog {
        return ConnectionDialog(
            "Enter Local Port Number",
            udpViewModel.udpLocalPort,
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
                                vm.udpSocket = DatagramSocket(
                                    port.toInt(),
                                    utils.getInetAddress(requireContext().applicationContext)
                                )
                                vm.udpSocket?.soTimeout = 0
                            } catch (e: Exception) {
                                requireActivity().runOnUiThread {
                                    toggle.isChecked = false
                                    editText.error = "Error: Check Connection or Port Number"
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

    /*/* Listen for upd Packets */*/
    private fun udpListen(soc: DatagramSocket?) {
        val msg = ByteArray(
            preferences!!
                .getString("udp_in_buffer", "255")!!.toInt()
        )
        val packet = DatagramPacket(msg, msg.size)
        soc?.receive(packet)

        val res =
            ConversationMessage(
                timeId = System.currentTimeMillis(),
                message = String(msg.sliceArray(0 until packet.length)),
                direction = 1,
                remoteIp = packet.address.toString().removePrefix("/"),
                remotePort = packet.port.toString(),
                localIp = soc!!.localAddress.toString().removePrefix("/"),
                localPort = soc.localPort.toString()
            )
        udpViewModel.addUdpResponse(res)
    }

    /*/* Transmits UDP Packets */*/
    private fun sendUdpPacket(
        fromServer: Boolean, message: String, ip: String,
        port: String, outPort: String, responseWait: String
    ): Int {
        try {
            val remoteIp = InetAddress.getByName(ip)
            val remotePort = port.toInt()
            val msg = message.toByteArray()
            val packet = DatagramPacket(msg, msg.size, remoteIp, remotePort)
            if (fromServer) {
                if (udpViewModel.udpSocket != null && udpViewModel.udpSocket!!.isBound) {
                    val protocolMessage = ConversationMessage(
                        timeId = System.currentTimeMillis(),
                        direction = 0,
                        message = String(msg), remoteIp = ip, remotePort = port,
                        localPort = udpViewModel.udpSocket!!.localPort.toString(),
                        localIp = udpViewModel.udpSocket!!.localAddress.toString()
                            .removePrefix("/")
                    )

                    udpViewModel.udpSocket!!.send(packet)
                    if (preferences!!.getBoolean("udp_show_sent", false)) {
                        udpViewModel.addUdpResponse(protocolMessage)
                    }
                } else {
                    requireActivity().runOnUiThread {
                        Toast.makeText(
                            requireContext().applicationContext,
                            "UDP Server not Running",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return 1
                }

            } else {
                val socket: DatagramSocket = if (outPort == "" || outPort == "0") {
                    DatagramSocket(0, utils.getInetAddress(requireContext().applicationContext))
                } else {
                    DatagramSocket(
                        outPort.toInt(),
                        utils.getInetAddress(requireContext().applicationContext)
                    )
                }
                if (preferences!!.getBoolean("udp_show_sent", false)) {
                    val protocolMessage = ConversationMessage(
                        timeId = System.currentTimeMillis(),
                        direction = 0,
                        message = String(msg), remoteIp = ip, remotePort = port,
                        localPort = socket.localPort.toString(),
                        localIp = socket.localAddress.toString().removePrefix("/")
                    )
                    udpViewModel.addUdpResponse(protocolMessage)
                }
                socket.send(packet)
                if (responseWait != "" && responseWait != "0") {
                    socket.soTimeout = responseWait.toInt()
                    udpListen(socket)
                }

                socket.close()
            }
            val address = remoteHost.text.toString()
            if (preferences!!.getBoolean("udp_remember_hosts", true)
                && !udpViewModel.udpRemoteAddresses.contains(address)
            ) {
                udpViewModel.udpRemoteAddresses.add(address)

                requireActivity().runOnUiThread {
                    addressAdapter.clear()
                    addressAdapter.addAll(udpViewModel.udpRemoteAddresses)
                    addressAdapter.notifyDataSetChanged()
                }
            }
            return 0
        } catch (e: IllegalArgumentException) {
            Log.e(LOG_TAG, "message", e)
            requireActivity().runOnUiThread {
                Toast.makeText(
                    requireContext().applicationContext,
                    " Incorrect remote IP Address ",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: SocketTimeoutException) {
            Log.e(LOG_TAG, "message", e)
            if (preferences!!.getBoolean("toast_not_received", false)) {
                requireActivity().runOnUiThread {
                    Toast.makeText(
                        requireContext().applicationContext,
                        " Socket Timed Out",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            return 0
        } catch (e: IOException) {
            Log.e(LOG_TAG, "message", e)
            requireActivity().runOnUiThread {
                Toast.makeText(
                    requireContext().applicationContext,
                    "Address Error",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "message", e)
            requireActivity().runOnUiThread {
                Toast.makeText(
                    requireContext().applicationContext,
                    " Error",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        return 1
    }
}




