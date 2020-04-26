package com.tetron.packetron.ui

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tetron.packetron.ProtocolMessage
import java.net.DatagramSocket
import java.net.ServerSocket
import java.net.Socket

class ConnectionViewModel : ViewModel() {
    var addresses = mutableListOf<String>()
    var localPort: String? = null
    var localTcpPort: String? = null
    var tcpClientAddress: String? = null

    var udpSocket: DatagramSocket? = null
    var tcpServerSocket: ServerSocket? = null
    var tcpClientSocket: Socket? = null

    var udpResponses: ArrayList<ProtocolMessage> = ArrayList()
    var tcpServerResponses: ArrayList<ProtocolMessage> = ArrayList()
    var tcpClientResponses: ArrayList<ProtocolMessage> = ArrayList()
    var tcpClients: ArrayList<Socket> = ArrayList()

    //lateinit var clientAdapter: ArrayAdapter<Socket>


    val tcpServerResponsesLive: MutableLiveData<ArrayList<ProtocolMessage>> by lazy {
        MutableLiveData<ArrayList<ProtocolMessage>>()
    }

    val udpResponsesLive: MutableLiveData<ArrayList<ProtocolMessage>> by lazy {
        MutableLiveData<ArrayList<ProtocolMessage>>()
    }

    val tcpClientResponsesLive: MutableLiveData<ArrayList<ProtocolMessage>> by lazy {
        MutableLiveData<ArrayList<ProtocolMessage>>()
    }

    val tcpClientsLive: MutableLiveData<ArrayList<Socket>> by lazy {
        MutableLiveData<ArrayList<Socket>>()
    }

    fun loadUdpResponses() {
        udpResponsesLive.postValue(udpResponses)
    }

    fun loadTcpServerResponses() {
        tcpServerResponsesLive.postValue(tcpServerResponses)
    }

    fun loadTcpClientResponses() {
        tcpClientResponsesLive.postValue(tcpClientResponses)
    }

    fun loadTcpClients() {
        tcpClientsLive.postValue(tcpClients)

    }

    override fun onCleared() {
        super.onCleared()
        try {
            udpSocket?.close()
            udpSocket = null
            tcpServerSocket?.close()
            tcpServerSocket = null
        } catch (e: Exception) {

        }

    }
}