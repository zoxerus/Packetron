package com.tetron.packetron.ui.tcp_server

import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.nio.charset.Charset

class TCPClientHandler(client: Socket) : Thread() {
    private val clientSocket: Socket = client
    private val writer: OutputStream = clientSocket.getOutputStream()
    private val reader: InputStream = clientSocket.getInputStream()

    override fun run() {
        super.run()
        writer.write("hello bitches\n".toByteArray(Charset.defaultCharset()))
        writer.flush()
    }
}