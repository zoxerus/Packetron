package com.tetron.packetron.ui.tcp_client

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
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
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
import com.tetron.packetron.ui.udp_send_receive.LOG_TAG
import java.io.InputStreamReader
import java.net.Socket


class TCPClientFragment(vm: ConnectionViewModel) : Fragment(), View.OnClickListener,
    View.OnFocusChangeListener {
    private val tcpClientViewModel = vm
    private lateinit var conversationViewModel: ConversationViewModel
    private var ipPref: SharedPreferences? = null
    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var recyclerView: RecyclerView
    private lateinit var responseAdapter: ResponseAdapter
    private lateinit var viewManager: RecyclerView.LayoutManager

    /*/* Global Variable */*/
    private var stopResends = true
    private val utils = ConnectionUtils()

    /*/*    Views    */*/
    private lateinit var hexInputET: EditText
    private lateinit var sendBTN: Button
    private lateinit var stopResendsBTN: Button
    private lateinit var outMessageET: EditText
    private lateinit var resendMsET: EditText

    // private lateinit var showAdvancedCB: CheckBox
    private lateinit var showHexCB: CheckBox
    private lateinit var advancedControlsLayout: ConstraintLayout


    constructor() : this(ConnectionViewModel(Application()))

    companion object {
        fun newInstance(vm: ConnectionViewModel): TCPClientFragment {
            return TCPClientFragment(vm)
        }
    }

    private val startForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                val intent = result.data
                tcpClientViewModel.tcpClientMessageToSend =
                    intent?.getStringExtra(getString(R.string.selected_message_template))!!
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        conversationViewModel = ViewModelProvider(this).get(ConversationViewModel::class.java)
        ipPref = activity?.getSharedPreferences(
            "ip_preferences", Context.MODE_PRIVATE
        )
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        responseAdapter = ResponseAdapter(tcpClientViewModel.tcpClientResponses) { pm ->
            MessageDialog("Enter a Reply", "Send")
            {
                if (pm.socket != null && pm.socket!!.isBound) {
                    Thread {
                        try {
                            pm.socket!!.getOutputStream()
                                .write(it.toByteArray())
                            pm.socket!!.getOutputStream().flush()
                            if (sharedPreferences.getBoolean(
                                    getString(R.string.tcp_client_show_sent),
                                    false
                                )
                            ) {
                                val newPm = ConversationMessage(
                                    timeId = System.currentTimeMillis(),
                                    message = it, direction = 0,
                                    localIp = pm.socket!!.localAddress.toString().removePrefix("/"),
                                    localPort = pm.socket!!.localPort.toString(),
                                    remoteIp = pm.socket!!.remoteSocketAddress.toString()
                                        .removePrefix("/"),
                                    remotePort = pm.socket!!.port.toString()
                                )
                                newPm.socket = pm.socket
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
                .showNow(requireActivity().supportFragmentManager, "Replay ConversationMessage")
        }
    }

    override fun onResume() {
        super.onResume()
        requireActivity().title = "TCP Client"
        tcpClientViewModel.tcpClientAddress =
            ipPref!!.getString("client_address", "127.0.0.1:33333")!!
        outMessageET.setText(tcpClientViewModel.tcpClientMessageToSend)
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
        outMessageET = view.findViewById(R.id.message_to_send)
        sendBTN = view.findViewById(R.id.send_button)
        stopResendsBTN = view.findViewById(R.id.stop_resends)
        resendMsET = view.findViewById(R.id.repeat_ms)
        // showAdvancedCB = view.findViewById(R.id.cb_show_advanced_controls)
        showHexCB = view.findViewById(R.id.cb_show_hex)
        advancedControlsLayout = view.findViewById(R.id.advanced_controls_layout)
        hexInputET = view.findViewById(R.id.hex_input)

/*
        showAdvancedCB.setOnCheckedChangeListener(){ _, isChecked ->
            if (isChecked){
                advancedControlsLayout.visibility = View.VISIBLE
            } else {
                advancedControlsLayout.visibility = View.GONE
            }
        }
*/
        showHexCB.setOnCheckedChangeListener() { _, isChecked ->
            if (isChecked) {
                responseAdapter.useHex(true)
            } else {
                responseAdapter.useHex(false)
            }
            responseAdapter.setResponses(tcpClientViewModel.tcpClientResponses)
        }


        sendBTN.setOnClickListener(this)
        stopResendsBTN.setOnClickListener(this)
        hexInputET.onFocusChangeListener = this
        outMessageET.onFocusChangeListener = this


        viewManager = LinearLayoutManager(requireContext().applicationContext)


        tcpClientViewModel.tcpClientResponsesLive.observe(
            viewLifecycleOwner,
            {
                recyclerView.scrollToPosition(tcpClientViewModel.tcpClientResponses.size - 1)
            })

        Log.e(LOG_TAG, "View Created")

        recyclerView = view.findViewById<RecyclerView>(R.id.response_recycler_view).apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = responseAdapter
        }
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_connect -> {
                val udpConnectionDialog =
                    ConnectionDialog("Enter Remote IP and address separated by a colon ':' ",
                        tcpClientViewModel.tcpClientAddress,
                        tcpClientViewModel,
                        { vm, button ->
                            if (vm.tcpClientSocket != null && vm.tcpClientSocket!!.isBound) {
                                button.isChecked = true
                            }
                        })
                        /// toggle button OnCheckedChange
                        { vm, address, toggle, editText ->
                            // set the last used address in the address input of the dialogue
                            if (sharedPreferences.getBoolean("tcp_client_remember_hosts", true)) {
                                tcpClientViewModel.tcpClientAddress = address
                            }
                            // disable the toggle button until the connection is established
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
                                        vm.tcpClientSocket!!.soTimeout = 2000
                                    } catch (e: Exception) {
                                        vm.tcpClientSocket = null
                                        e.printStackTrace()
                                        requireActivity().runOnUiThread {
                                            toggle.isChecked = false
                                            editText.error = "check IP and Port"
                                        }
                                    }
                                    val message = ByteArray(
                                        sharedPreferences.getString(
                                            getString(R.string.tcp_client_in_buffer), "255"
                                        )!!.toInt()
                                    )
                                    var length = 0
                                    try {
                                        val reader = vm.tcpClientSocket!!.getInputStream()
                                        do {
                                            if (length > 0) {
                                                val cm = ConversationMessage(
                                                    timeId = System.currentTimeMillis(),
                                                    message = String(message, 0, length),
                                                    direction = 1,
                                                    localPort = vm.tcpClientSocket!!.localPort.toString(),
                                                    localIp = vm.tcpClientSocket!!.localAddress.toString()
                                                        .removePrefix("/"),
                                                    remotePort = vm.tcpClientSocket!!.port.toString(),
                                                    remoteIp = vm.tcpClientSocket!!.remoteSocketAddress
                                                        .toString().removePrefix("/").split(":")[0]
                                                )
                                                cm.socket = vm.tcpClientSocket
                                                vm.addTcpClientResponse(cm)
                                            }
                                            length = try {
                                                reader.read(message, 0, message.size)
                                            } catch (e: Exception) {
                                                0
                                            }
                                        } while (vm.tcpClientSocket != null && length != -1)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                    try {
                                        vm.tcpClientSocket?.close()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                    vm.tcpClientSocket = null
                                }.start()
                            }
                            toggle.isEnabled = true
                        }
                udpConnectionDialog.showNow(
                    requireActivity().supportFragmentManager,
                    "Connection Dialog"
                )
            }
            R.id.action_clear_responses -> {
                tcpClientViewModel.tcpClientResponses.clear()
                responseAdapter.notifyDataSetChanged()
            }
            R.id.message_templates -> {
                startForResult.launch(Intent(activity, SavedMessageActivity::class.java))
            }

            R.id.mm_action_save_conversation -> {
                // specify the behaviour when user clicks the positive button of the reply dialogue
                MessageDialog("Enter a Name", "Save") {
                    if (tcpClientViewModel.tcpClientResponsesLive.value != null && tcpClientViewModel.tcpClientResponsesLive.value!!.isNotEmpty()) {
                        val msgs = ConversationsTable(
                            name = it,
                            fromTime = tcpClientViewModel.tcpClientResponsesLive.value!![0].timeId,
                            toTime = tcpClientViewModel.tcpClientResponsesLive.value!![tcpClientViewModel.tcpClientResponsesLive.value!!.size - 1].timeId
                        )
                        conversationViewModel.insertMany(responseAdapter.getAll())
                        conversationViewModel.insertConversation(msgs)
                    }
                }
                    .showNow(requireActivity().supportFragmentManager, "Conversation Name")
            }


        }
        return super.onOptionsItemSelected(item)
    }

    override fun onFocusChange(v: View?, hasFocus: Boolean) {
        when (v!!.id) {
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
                    if (hex.length % 2 != 0) {
                        hex = "0$hex"
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

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.send_button -> {
                val msg = outMessageET.text.toString()
                val resendDelay = resendMsET.text.toString()
                if (msg.isNotEmpty() &&
                    tcpClientViewModel.tcpClientSocket != null &&
                    tcpClientViewModel.tcpClientSocket!!.isConnected
                ) {
                    if (resendDelay.isNotEmpty() && resendDelay.toInt() > 0) {
                        if (resendDelay.toInt() < 100) {
                            resendMsET.error = "must be >= 100"
                            return
                        }
                        outMessageET.text = null
                        stopResends = false
                        Thread {
                            var lastCall = 0L
                            while (!stopResends) {
                                if (System.currentTimeMillis() - lastCall >= resendDelay.toLong()) {
                                    lastCall = System.currentTimeMillis()
                                    sendToServer(msg)
                                }
                            }
                        }.start()
                    } else {
                        Thread {
                            sendToServer(msg)
                        }.start()
                    }
                    outMessageET.text = null
                }
            }
            R.id.stop_resends -> {
                stopResends = true
            }
        }
    }

    private fun sendToServer(msg: String) {
        try {
            tcpClientViewModel.tcpClientSocket!!.getOutputStream()
                .write(msg.toByteArray())
            if (sharedPreferences.getBoolean(
                    getString(R.string.tcp_client_show_sent),
                    false
                )
            ) {
                val pm = ConversationMessage(
                    timeId = System.currentTimeMillis(),
                    message = msg, direction = 0,
                    localPort = tcpClientViewModel.tcpClientSocket!!.localPort.toString(),
                    localIp = tcpClientViewModel.tcpClientSocket!!.localAddress.toString()
                        .removePrefix("/"),
                    remotePort = tcpClientViewModel.tcpClientSocket!!.port.toString(),
                    remoteIp = tcpClientViewModel.tcpClientSocket!!.remoteSocketAddress.toString()
                        .removePrefix("/")
                )
                pm.socket = tcpClientViewModel.tcpClientSocket
                tcpClientViewModel.addTcpClientResponse(pm)
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

}