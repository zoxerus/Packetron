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
import com.tetron.packetron.ui.MessageDialog
import com.tetron.packetron.ui.ResponseAdapter
import kotlinx.android.synthetic.main.fragment_udp_send_receive.*
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.SocketTimeoutException

const val LOG_TAG: String = "UDP_SEND_RECEIVE"

class UDPSendReceiveFragment(vm: UDPViewModel) : Fragment() {

    private val udpViewModel = vm

    private var responseRecyclerView: RecyclerView? = null
    private var ipPref: SharedPreferences? = null

    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager


    private lateinit var addressAdapter: ArrayAdapter<String>
    private lateinit var remoteHost: AutoCompleteTextView

    constructor() : this(UDPViewModel())

    companion object {
        fun newInstance(vm: UDPViewModel): UDPSendReceiveFragment {
            return UDPSendReceiveFragment(vm)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        retainInstance = true
        setHasOptionsMenu(true)
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
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
        responseRecyclerView = view.findViewById(R.id.response_recycler_view)
        remoteHost = view.findViewById(R.id.remote_address_and_port)



        viewManager = LinearLayoutManager(requireContext().applicationContext)
        viewAdapter = ResponseAdapter(udpViewModel.responses) { itpm ->
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

        udpViewModel.responsesLive.observe(
            viewLifecycleOwner,
            Observer<List<ProtocolMessage>> { _ ->
                viewAdapter.notifyDataSetChanged()
            })

        Log.e(LOG_TAG, "View Created")

        recyclerView = view.findViewById<RecyclerView>(R.id.response_recycler_view).apply {
            // use this setting to improve performance if you know that changes
            // in content do not change the layout size of the RecyclerView
            setHasFixedSize(true)

            // use a linear layout manager
            layoutManager = viewManager

            // specify an viewAdapter (see also next example)
            adapter = viewAdapter
        }
        send_button.setOnClickListener {
            val remoteIp = remote_address_and_port.text
                .toString()
                .split(":", ignoreCase = true, limit = 0)
                .first()
            val remotePort = remote_address_and_port.text
                .toString()
                .split(":", ignoreCase = true, limit = 0)
                .last()
            val message = message_to_send.text.toString()
            SendPacketAsyncTask().execute(remoteIp, remotePort, message)

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
                val udpConnectionDialog = UDPConnectionDialog(udpViewModel)
                udpConnectionDialog.showNow(
                    requireActivity().supportFragmentManager,
                    "Connection Dialog"
                )
            }
            R.id.action_clear_responses -> {
                udpViewModel.responses.clear()
                udpViewModel.loadResponses()
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