package com.tetron.packetron.ui.tcp_server

import android.util.Log
import com.tetron.packetron.db.conversations.ConversationMessage
import com.tetron.packetron.ui.ConnectionViewModel
import java.io.InputStream
import java.net.Socket

class TCPClientHandler(
    private val vm: ConnectionViewModel,
    private val inBuffer: Int,
    client: Socket
) : Thread() {
    private val clientSocket: Socket = client
    private val reader: InputStream = clientSocket.getInputStream()


    override fun run() {
        super.run()
        val message = ByteArray(inBuffer)
        var length = 0
        do {
            if (length > 0) {
                val pm = ConversationMessage(
                    timeId = System.currentTimeMillis(),
                    message = String(message, 0, length), direction = 1,
                    localIp = clientSocket.localAddress.toString().removePrefix("/"),
                    localPort = clientSocket.localPort.toString(),
                    remoteIp = clientSocket.remoteSocketAddress.toString().removePrefix("/").split(":")[0],
                    remotePort = clientSocket.port.toString()
                )
                pm.socket = clientSocket
                vm.addTcpServerResponse(pm)
            }
            length = reader.read(message)
        } while (length != -1 && vm.tcpServerSocket != null)

        vm.updateTcpClients(clientSocket, 1)
        interrupt()
    }
}