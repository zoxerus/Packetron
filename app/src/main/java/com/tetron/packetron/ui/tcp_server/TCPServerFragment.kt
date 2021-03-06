package com.tetron.packetron.ui.tcp_server

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
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
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
import java.net.ServerSocket
import java.net.Socket

class TCPServerFragment(vm: ConnectionViewModel) : Fragment(), View.OnClickListener, View.OnFocusChangeListener {
    private val tcpViewModel = vm

    private lateinit var recyclerView: RecyclerView
    private lateinit var responseAdapter: ResponseAdapter
    private lateinit var viewManager: RecyclerView.LayoutManager

    private var ipPref: SharedPreferences? = null
    private var sharedPreferences: SharedPreferences? = null


    private lateinit var tcpClientAdapter: ArrayAdapter<Socket>
    private lateinit var conversationViewModel: ConversationViewModel



    /*/* Global Variables */*/
    private var stopResends = true
    private val utils = ConnectionUtils()

    /*/* Views   */*/
    private lateinit var hexInputET: EditText
    private lateinit var sendBTN: Button
    private lateinit var stopResendsBTN: Button
    private lateinit var outMessageET: EditText
    private lateinit var addressSpinner: Spinner
    private lateinit var resendMsET: EditText
    private lateinit var showAdvancedCB: CheckBox
    private lateinit var showHexCB: CheckBox
    private lateinit var advancedControlsLayout: ConstraintLayout

    constructor() : this(ConnectionViewModel(Application()))

    companion object {
        fun newInstance(vm: ConnectionViewModel): TCPServerFragment {
            return TCPServerFragment(vm)
        }
    }

    private val startForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                val intent = result.data
                tcpViewModel.tcpServerMessageToSend =
                    intent?.getStringExtra(getString(R.string.selected_message_template))!!
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        ipPref = activity?.getSharedPreferences("ip_preferences", Context.MODE_PRIVATE)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        conversationViewModel = ViewModelProvider(this).get(ConversationViewModel::class.java)
        responseAdapter = ResponseAdapter(tcpViewModel.tcpServerResponses) { pm ->
            MessageDialog("Enter a Reply", "Send")
            {
                if (pm.socket != null && pm.socket!!.isBound) {
                    Thread {
                        try {
                            pm.socket!!.getOutputStream()
                                .write(it.toByteArray())
                            pm.socket!!.getOutputStream().flush()
                            if (sharedPreferences!!.getBoolean(
                                    getString(R.string.tcp_server_show_sent), false
                                )
                            ) {
                                val newPm = ConversationMessage(
                                    timeId = System.currentTimeMillis(),
                                    message = it, direction = 0,
                                    localPort = pm.socket!!.localPort.toString(),
                                    localIp = pm.socket!!.localAddress.toString(),
                                    remotePort = pm.socket!!.port.toString(),
                                    remoteIp = pm.socket!!.remoteSocketAddress.toString()
                                )
                                newPm.socket = pm.socket
                                tcpViewModel.addTcpServerResponse(newPm)
                            }
                        } catch (e: Exception) {
                            requireActivity().runOnUiThread {
                                Toast.makeText(
                                    context,
                                    "Client Disconnected",
                                    Toast.LENGTH_LONG
                                ).show()
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
        requireActivity().title = "TCP Server"
        tcpViewModel.localTcpPort = ipPref!!.getString("local_port", "33333")!!
        outMessageET.setText(tcpViewModel.tcpServerMessageToSend)
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
        addressSpinner = view.findViewById(R.id.tcp_address_spinner)
        outMessageET = view.findViewById(R.id.message_to_send)
        sendBTN = view.findViewById(R.id.send_button)
        resendMsET = view.findViewById(R.id.repeat_ms)
        showAdvancedCB = view.findViewById(R.id.cb_show_advanced_controls)
        showHexCB = view.findViewById(R.id.cb_show_hex)
        advancedControlsLayout = view.findViewById(R.id.advanced_controls_layout)
        stopResendsBTN = view.findViewById(R.id.stop_resends)
        hexInputET = view.findViewById(R.id.hex_input)
        sendBTN.setOnClickListener(this)
        showAdvancedCB.setOnCheckedChangeListener(){ _, isChecked ->
            if (isChecked){
                advancedControlsLayout.visibility = View.VISIBLE
            } else {
                advancedControlsLayout.visibility = View.GONE
            }
        }
        showHexCB.setOnCheckedChangeListener(){_,isChecked ->
            if (isChecked){
                responseAdapter.useHex(true)
            } else {
                responseAdapter.useHex(false)
            }
            responseAdapter.setResponses(tcpViewModel.tcpServerResponses)
        }

        hexInputET.onFocusChangeListener = this
        outMessageET.onFocusChangeListener = this
        stopResendsBTN.setOnClickListener(this)

        val tcpClients = ArrayList<Socket>()
        tcpClientAdapter = ArrayAdapter(
            requireActivity(),
            android.R.layout.simple_dropdown_item_1line,
            tcpClients
        )
        addressSpinner.adapter = tcpClientAdapter


        viewManager = LinearLayoutManager(requireContext().applicationContext)


        tcpViewModel.tcpServerResponsesLive.observe(
            viewLifecycleOwner,
            {
                recyclerView.scrollToPosition(tcpViewModel.tcpServerResponses.size - 1)
            })
        tcpViewModel.tcpClientsLive.observe(
            viewLifecycleOwner,
            {
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_connect -> {
                val udpConnectionDialog =
                    ConnectionDialog(
                        "Enter Local Port Number",
                        tcpViewModel.localTcpPort,
                        tcpViewModel,
                        { vm, button ->
                            button.isChecked = vm.tcpServerSocket != null && vm.tcpServerSocket!!.isBound
                        } )
                    // on button checkedChangeListener
                    { vm, port, toggle, editText ->
                        if (toggle.isChecked){
                            Thread {
                                try {
                                    vm.tcpServerSocket = ServerSocket(port.toInt())
                                } catch (e: Exception) {
                                    vm.tcpServerSocket = null
                                    vm.clearTcpClients()
                                    e.printStackTrace()
                                    requireActivity().runOnUiThread {
                                        toggle.isChecked = false
                                        editText.error = " Cannot bind to port "
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
                                } while (vm.tcpServerSocket != null && vm.tcpServerSocket!!.isBound )
                            }.start()
                        } else {
                            try {
                                vm.tcpServerSocket!!.close()
                                vm.tcpServerSocket = null
                            } catch (e: Exception) {
                                e.printStackTrace()
                                //vm.clearTcpClients()
                                vm.tcpServerSocket = null
                            }
                        }
                    }
                udpConnectionDialog.showNow(
                    requireActivity().supportFragmentManager,
                    "Connection Dialog"
                )
            }
            R.id.action_clear_responses -> {
                tcpViewModel.tcpServerResponses.clear()
                responseAdapter.notifyDataSetChanged()
            }
            R.id.message_templates -> {
                startForResult.launch(Intent(activity, SavedMessageActivity::class.java))
            }
            R.id.mm_action_save_conversation -> {
                // specify the behaviour when user clicks the positive button of the reply dialogue
                MessageDialog("Enter a Name", "Save") {
                    if (tcpViewModel.tcpServerResponsesLive.value != null && tcpViewModel.tcpServerResponsesLive.value!!.isNotEmpty()){
                        val msgs = ConversationsTable(name = it,
                            fromTime = tcpViewModel.tcpServerResponsesLive.value!![0].timeId,
                            toTime = tcpViewModel.tcpServerResponsesLive.value!![tcpViewModel.tcpServerResponsesLive.value!!.size - 1].timeId
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
        with(ipPref!!.edit()) {
            putString("local_port", tcpViewModel.localTcpPort)
            apply()
        }
    }

    private fun sendToClient(soc: Socket?, msg: String) {
        if (msg.isNotEmpty() && soc != null && soc.isConnected) {
            try {
                soc.getOutputStream().write(msg.toByteArray())
                if (sharedPreferences!!.getBoolean(
                        getString(R.string.tcp_server_show_sent), false
                    )
                ) {
                    val pm = ConversationMessage(
                        timeId = System.currentTimeMillis(),
                        message = msg, direction = 0,
                        localIp = soc.localAddress.toString().removePrefix("/"),
                        localPort = soc.localPort.toString(),
                        remoteIp = soc.remoteSocketAddress.toString()
                            .removePrefix("/"),
                        remotePort = soc.port.toString()
                    )
                    pm.socket = soc
                    tcpViewModel.addTcpServerResponse(pm)
                }

            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
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

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.send_button -> {
                val pos = addressSpinner.selectedItemPosition
                val resendDelay = resendMsET.text.toString()
                if (pos == -1) {
                    Toast.makeText(requireContext(), "Select Client First", Toast.LENGTH_LONG)
                        .show()
                }
                /* Send the Message */
                else {
                    val soc = tcpClientAdapter.getItem(pos)
                    val msg = outMessageET.text.toString()
                    outMessageET.text = null
                    if (resendDelay.isNotEmpty() && resendDelay.toInt() > 0) {
                        if (resendDelay.toInt() < 100 ){
                            resendMsET.error = "must be >= 100"
                            return
                        }
                        stopResends = false
                        Thread {
                            var lastCall = 0L
                            while (!stopResends) {
                                if (System.currentTimeMillis() - lastCall >= resendDelay.toLong()) {
                                    lastCall = System.currentTimeMillis()
                                    sendToClient(soc, msg)
                                }
                            }
                        }.start()
                    } else {
                        Thread {
                            sendToClient(soc, msg)
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