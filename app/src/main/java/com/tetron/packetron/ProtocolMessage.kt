package com.tetron.packetron

import java.net.Socket

class ProtocolMessage(msg: String, s: Socket? = null) {
    val socket: Socket? = s
    var messageText: String = msg
    var messagePort: String = s?.port.toString()
    var messageIp: String = s?.inetAddress.toString()

    override fun toString(): String {
        return "$messageIp:$messagePort\t$messageText"
    }
}