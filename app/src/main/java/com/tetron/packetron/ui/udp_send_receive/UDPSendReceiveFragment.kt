package com.tetron.packetron.ui.udp_send_receive

import android.content.Context
import android.content.SharedPreferences
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tetron.packetron.R
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

    companion object {
        fun newInstance(vm: UDPViewModel): UDPSendReceiveFragment {
            return UDPSendReceiveFragment(vm)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setHasOptionsMenu(true)
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        udpViewModel.remoteIp = ipPref!!.getString("remote_ip", "")
        udpViewModel.localPort = ipPref!!.getString("local_port", "33333")
        udpViewModel.remotePort = ipPref!!.getString("remote_port", "")
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

        viewManager = LinearLayoutManager(requireContext().applicationContext)
        viewAdapter = ResponseAdapter(udpViewModel.responses)

        if (savedInstanceState != null) {
            udpViewModel.responses = savedInstanceState.getStringArrayList("responses")!!
            Log.e(LOG_TAG, "Instance restored")
        }
        udpViewModel.responsesLive.observe(viewLifecycleOwner, Observer<List<String>> { _ ->
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
            val message = message_to_send.text.toString()
            SendPacketAsyncTask().execute(udpViewModel.remoteIp, udpViewModel.remotePort, message)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.run {
            putStringArrayList("responses", udpViewModel.responses)
            Log.e(LOG_TAG, "instance saved")
        }

    }

    override fun onPause() {
        super.onPause()
        with(ipPref!!.edit()) {
            putString("local_port", udpViewModel.localPort)
            putString("remote_port", udpViewModel.remotePort)
            putString("remote_ip", udpViewModel.remoteIp)
            apply()
        }

        Log.e(LOG_TAG, "Paused")
    }


    inner class SendPacketAsyncTask : AsyncTask<String, Void, String>() {

        override fun doInBackground(vararg params: String?): String? {
            try {
                val msg = params[2]?.toByteArray()
                val port = params[1]!!.toInt()
                val receiverAddress = InetAddress.getByName(params[0])
                val packet = DatagramPacket(msg, msg!!.size, receiverAddress, port)
                if (udpViewModel.udpSocket != null && udpViewModel.udpSocket!!.isBound) {
                    udpViewModel.udpSocket?.send(packet)
                }
                return ""

            } catch (e: SocketTimeoutException) {
                e.printStackTrace()
            } catch (e: Exception) {
                Toast.makeText(
                    activity,
                    " Incorrect IP Address or Port unavailable ",
                    Toast.LENGTH_LONG
                ).show()
                e.printStackTrace()
            }
            return ""
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