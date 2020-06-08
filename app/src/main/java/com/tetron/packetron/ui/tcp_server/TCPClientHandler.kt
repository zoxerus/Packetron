package com.tetron.packetron.ui.tcp_server

import com.tetron.packetron.ProtocolMessage
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
            if (length != 0) {
                val pm = ProtocolMessage(
                    String(message, 0, length),
                    clientSocket
                )
                vm.addTcpServerResponse(pm)

            }
            length = reader.read(message)
        } while (length != -1 && vm.tcpServerSocket != null)

        vm.updateTcpClients(clientSocket, 1)
        interrupt()
    }
}