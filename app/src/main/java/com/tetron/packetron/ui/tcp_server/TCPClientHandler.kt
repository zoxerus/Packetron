package com.tetron.packetron.ui.tcp_server

import android.util.Log
import com.tetron.packetron.db.conversations.ConversationMessage
import com.tetron.packetron.ui.ConnectionViewModel
import java.io.InputStream
import java.lang.Exception
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
        clientSocket.soTimeout = 2000
        try {        val message = ByteArray(inBuffer)
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
                length = try{
                    reader.read(message,0,message.size)
                } catch (e: Exception){
                    0
                }
            } while (length != -1 && !vm.tcpServerSocket!!.isClosed )

            vm.updateTcpClients(clientSocket, 1)
            clientSocket.close()
            interrupt()
        }
        catch (e: Exception){
            vm.updateTcpClients(clientSocket, 1)
            clientSocket.close()

            interrupt()
        }
    }


}