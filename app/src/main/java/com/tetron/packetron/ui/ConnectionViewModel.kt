package com.tetron.packetron.ui

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tetron.packetron.ProtocolMessage
import java.net.DatagramSocket
import java.net.ServerSocket
import java.net.Socket

class ConnectionViewModel : ViewModel() {
    var addresses = mutableListOf<String>()
    var localPort = "33333"
    var localTcpPort = "33333"
    var tcpClientAddress = "127.0.0.1:33333"

    var udpSocket: DatagramSocket? = null
    var tcpServerSocket: ServerSocket? = null
    var tcpClientSocket: Socket? = null


    var udpResponses: ArrayList<ProtocolMessage> = ArrayList()

    var tcpServerResponses: ArrayList<ProtocolMessage> = ArrayList()
    var tcpClientResponses: ArrayList<ProtocolMessage> = ArrayList()
    var tcpClients: ArrayList<Socket> = ArrayList()


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

    @Synchronized
    fun addUdpResponse(pm: ProtocolMessage) {
        udpResponses.add(pm)
        udpResponsesLive.postValue(udpResponses)
    }

    @Synchronized
    fun addTcpServerResponse(pm: ProtocolMessage) {
        tcpServerResponses.add(pm)
        tcpServerResponsesLive.postValue(tcpServerResponses)
    }

    @Synchronized
    fun addTcpClientResponse(pm: ProtocolMessage) {
        tcpClientResponses.add(pm)
        tcpClientResponsesLive.postValue(tcpClientResponses)
    }

    @Synchronized
    fun updateTcpClients(s: Socket?, op: Int) {
        if (op == 0) {
            tcpClients.add(s!!)
        }
        if (op == 1) {
            tcpClients.remove(s!!)
        }
        tcpClientsLive.postValue(tcpClients)
    }

    override fun onCleared() {
        super.onCleared()
        try {
            udpSocket?.close()
            udpSocket = null
            tcpServerSocket?.close()
            tcpServerSocket = null
            tcpClientSocket?.close()
            tcpClientSocket = null
        } catch (e: Exception) {

        }

    }
}