package com.tetron.packetron.ui.udp_send_receive

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tetron.packetron.ProtocolMessage
import java.net.DatagramSocket

class UDPViewModel : ViewModel() {
    var addresses = mutableListOf<String>()

    var localPort: String? = null
    var udpSocket: DatagramSocket? = null
    var responses: ArrayList<ProtocolMessage> = ArrayList()

    val responsesLive: MutableLiveData<ArrayList<ProtocolMessage>> by lazy {
        MutableLiveData<ArrayList<ProtocolMessage>>()
    }

    fun loadResponses() {
        responsesLive.postValue(responses)
    }

    override fun onCleared() {
        super.onCleared()
        try {
            udpSocket?.close()
            udpSocket = null
        } catch (e: Exception) {

        }

    }
}