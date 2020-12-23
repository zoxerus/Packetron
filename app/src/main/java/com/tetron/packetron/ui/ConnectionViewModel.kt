package com.tetron.packetron.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.tetron.packetron.db.conversations.ConversationMessage
import java.net.DatagramSocket
import java.net.ServerSocket
import java.net.Socket

class ConnectionViewModel(application: Application) : AndroidViewModel(application) {
    var udpRemoteAddresses = mutableListOf<String>()
    var udpLocalPort = "33333"
    var localTcpPort = "33333"
    var tcpClientAddress = "127.0.0.1:33333"

    var udpSocket: DatagramSocket? = null
    var tcpServerSocket: ServerSocket? = null
    var tcpClientSocket: Socket? = null


    var udpResponses: ArrayList<ConversationMessage> = ArrayList()

    var tcpServerResponses: ArrayList<ConversationMessage> = ArrayList()
    var tcpClientResponses: ArrayList<ConversationMessage> = ArrayList()
    var tcpClients: ArrayList<Socket> = ArrayList()

    var udpMessageToSend = ""
    var tcpServerMessageToSend = ""
    var tcpClientMessageToSend = ""


    val tcpServerResponsesLive: MutableLiveData<ArrayList<ConversationMessage>> by lazy {
        MutableLiveData<ArrayList<ConversationMessage>>()
    }

    val udpResponsesLive: MutableLiveData<ArrayList<ConversationMessage>> by lazy {
        MutableLiveData<ArrayList<ConversationMessage>>()
    }

    val tcpClientResponsesLive: MutableLiveData<ArrayList<ConversationMessage>> by lazy {
        MutableLiveData<ArrayList<ConversationMessage>>()
    }

    val tcpClientsLive: MutableLiveData<ArrayList<Socket>> by lazy {
        MutableLiveData<ArrayList<Socket>>()
    }

    @Synchronized
    fun addUdpResponse(pm: ConversationMessage) {
        udpResponses.add(pm)
        udpResponsesLive.postValue(udpResponses)
    }

    @Synchronized
    fun addTcpServerResponse(pm: ConversationMessage) {
        tcpServerResponses.add(pm)
        tcpServerResponsesLive.postValue(tcpServerResponses)
    }

    @Synchronized
    fun addTcpClientResponse(pm: ConversationMessage) {
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