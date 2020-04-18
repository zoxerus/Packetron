package com.tetron.packetron.ui.udp_send_receive

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.net.DatagramSocket

class UDPViewModel : ViewModel() {
    var remoteIp: String? = null
    var remotePort: String? = null
    var localPort: String? = null
    var udpSocket: DatagramSocket? = null
    var responses: ArrayList<String> = ArrayList()

    val responsesLive: MutableLiveData<ArrayList<String>> by lazy {
        MutableLiveData<ArrayList<String>>()
    }

    fun loadResponses() {
        responsesLive.postValue(responses)
    }
}