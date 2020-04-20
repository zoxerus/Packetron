package com.tetron.packetron.ui.tcp_server

import com.tetron.packetron.ProtocolMessage
import java.io.InputStream
import java.net.Socket

class TCPClientHandler(client: Socket, act: (ProtocolMessage) -> Unit) : Thread() {
    private val clientSocket: Socket = client
    private val reader: InputStream = clientSocket.getInputStream()
    private val action = act

    override fun run() {
        super.run()
        val message = ByteArray(255)
        var length: Int
        do {
            length = reader.read(message)
            action(
                ProtocolMessage(
                    String(message, 0, length),
                    clientSocket
                )
            )
        } while (length != -1)
        interrupt()
    }
}