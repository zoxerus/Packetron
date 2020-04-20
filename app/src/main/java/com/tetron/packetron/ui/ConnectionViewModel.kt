package com.tetron.packetron.ui

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tetron.packetron.ProtocolMessage
import java.net.DatagramSocket
import java.net.ServerSocket

class ConnectionViewModel : ViewModel() {
    var addresses = mutableListOf<String>()
    var localPort: String? = null
    var udpSocket: DatagramSocket? = null
    var tcpSocket: ServerSocket? = null
    var udpResponses: ArrayList<ProtocolMessage> = ArrayList()
    var tcpResponses: ArrayList<ProtocolMessage> = ArrayList()


    val tcpResponsesLive: MutableLiveData<ArrayList<ProtocolMessage>> by lazy {
        MutableLiveData<ArrayList<ProtocolMessage>>()
    }

    val udpResponsesLive: MutableLiveData<ArrayList<ProtocolMessage>> by lazy {
        MutableLiveData<ArrayList<ProtocolMessage>>()
    }

    fun loadUdpResponses() {
        udpResponsesLive.postValue(udpResponses)
    }

    fun loadTcpResponses() {
        tcpResponsesLive.postValue(tcpResponses)
    }

    override fun onCleared() {
        super.onCleared()
        try {
            udpSocket?.close()
            udpSocket = null
            tcpSocket?.close()
            tcpSocket = null
        } catch (e: Exception) {

        }

    }
}