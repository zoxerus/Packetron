package com.tetron.packetron

import java.net.Socket
import java.text.SimpleDateFormat
import java.util.*

class ProtocolMessage(msg: String, s: Socket? = null) {
    private val sdf = SimpleDateFormat.getTimeInstance()
    val socket: Socket? = s
    var messageText: String = msg
    var messagePort: String = s?.port.toString()
    var messageIp: String = s?.inetAddress.toString()
    var messageTime: Date = Calendar.getInstance().time

    fun addressToString(): String {
        if (messageIp.startsWith("//")) {
            return "${messageIp.removePrefix("/")}:$messagePort:\t\t\t\t${sdf.format(messageTime)}"
        }
        return "$messageIp:$messagePort:\t\t\t\t${sdf.format(messageTime)}"
    }

    override fun toString(): String {
        return "$messageIp:$messagePort: \t$messageText"
    }
}