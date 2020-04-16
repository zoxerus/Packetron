package com.tetron.packetron.ui.udp_send_receive

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.widget.ToggleButton
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tetron.packetron.R
import com.tetron.packetron.ui.ResponseAdapter
import kotlinx.android.synthetic.main.fragment_udp_send_receive.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException

const val LOG_TAG: String = "UDP_SEND_RECEIVE"

class UDPSendReceiveFragment : Fragment() {

    //private lateinit var viewModel: UDPSendReceiveViewModel

    private var responseRecyclerView: RecyclerView? = null
    private var ipPref: SharedPreferences? = null

    private var udpSocket: DatagramSocket? = null


    private var responses: ArrayList<String> = ArrayList()

    private lateinit var recyclerView: RecyclerView
    private lateinit var viewAdapter: RecyclerView.Adapter<*>
    private lateinit var viewManager: RecyclerView.LayoutManager

    companion object {
        fun newInstance(): UDPSendReceiveFragment {
            return UDPSendReceiveFragment()

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

        if (savedInstanceState != null) {
            responses = savedInstanceState.getStringArrayList("responses")!!
            Log.e(LOG_TAG, "Instance restored")
        }
        remote_ip.setText(ipPref!!.getString("remote_ip", ""))
        local_port.setText(ipPref!!.getString("local_port", "33333"))
        remote_port.setText(ipPref!!.getString("remote_port", ""))


        if (udpSocket != null && udpSocket!!.isBound) {
            button_connect.isChecked = true
        }

        Log.e(LOG_TAG, "View Created")

        responseRecyclerView = view.findViewById(R.id.response_recycler_view)

        viewManager = LinearLayoutManager(activity!!.applicationContext)
        viewAdapter = ResponseAdapter(responses)

        recyclerView = view.findViewById<RecyclerView>(R.id.response_recycler_view).apply {
            // use this setting to improve performance if you know that changes
            // in content do not change the layout size of the RecyclerView
            setHasFixedSize(true)

            // use a linear layout manager
            layoutManager = viewManager

            // specify an viewAdapter (see also next example)
            adapter = viewAdapter
        }


        val connectToggle: ToggleButton = view.findViewById(R.id.button_connect)
        connectToggle.text = getString(R.string.text_connect)
        connectToggle.textOff = getString(R.string.text_connect)
        connectToggle.textOn = getString(R.string.text_disconnect)


        connectToggle.setOnCheckedChangeListener { _, _ ->
            if (udpSocket != null && udpSocket!!.isBound) {
                udpSocket?.close()
                udpSocket = null
            } else {
                val localPort = local_port.text.toString()
                Thread {
                    do {
                        val msg = ByteArray(2048)
                        val packet = DatagramPacket(msg, msg.size)
                        if (udpSocket == null || !udpSocket!!.isBound) {
                            try {
                                udpSocket = DatagramSocket(localPort.toInt())
                                udpSocket?.soTimeout = 0
                            } catch (e: Exception) {
                                activity!!.runOnUiThread {
                                    connectToggle.isChecked = false
                                    local_port.error = "Port Unavailable"
                                }
                                e.printStackTrace()
                                break
                            }

                        } else {

                            try {
                                udpSocket?.receive(packet)
                                val res: String =
                                    packet.address.toString() + ":" + packet.port.toString() + " " + String(
                                        msg
                                    )
                                activity!!.runOnUiThread {
                                    responses.add(0, res)
                                    viewAdapter.notifyDataSetChanged()
                                }
                            } catch (e: SocketTimeoutException) {
                                e.printStackTrace()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    } while (udpSocket != null && udpSocket!!.isBound)
                    udpSocket?.close()
                }.start()


            }

        }

        send_button.setOnClickListener {
            val ipAddress = remote_ip.text.toString()
            val port = remote_port.text.toString()
            val message = message_to_send.text.toString()

            SendPacketAsyncTask().execute(ipAddress, port, message)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.run {
            putStringArrayList("responses", responses)
            Log.e(LOG_TAG, "instance saved")
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        Log.e(LOG_TAG, "Destroyed")
        try {
            udpSocket?.close()
            udpSocket = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onPause() {
        super.onPause()
        with(ipPref!!.edit()) {
            putString("local_port", local_port.text.toString())
            putString("remote_port", remote_port.text.toString())
            putString("remote_ip", remote_ip.text.toString())
            apply()
        }

        Log.e(LOG_TAG, "Paused")
    }


    class SendPacketAsyncTask : AsyncTask<String, Void, String>() {
        private var udpSocket: DatagramSocket? = null
        private var context: Context? = null
        fun new(s: DatagramSocket, c: Activity) {
            this.udpSocket = s
            this.context = c
        }

        override fun doInBackground(vararg params: String?): String? {
            try {
                val msg = params[2]?.toByteArray()
                val port = params[1]!!.toInt()
                val receiverAddress = InetAddress.getByName(params[0])
                val packet = DatagramPacket(msg, msg!!.size, receiverAddress, port)
                if (this.udpSocket != null && this.udpSocket!!.isBound) {
                    this.udpSocket?.send(packet)
                }
                return ""

            } catch (e: SocketTimeoutException) {
                e.printStackTrace()
            } catch (e: Exception) {


                e.printStackTrace()
            }
            return ""
        }

        override fun onPostExecute(result: String?) {
            Toast.makeText(
                this.context,
                " Incorrect IP Address or Port unavailable ",
                Toast.LENGTH_LONG
            ).show()

            super.onPostExecute(result)
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