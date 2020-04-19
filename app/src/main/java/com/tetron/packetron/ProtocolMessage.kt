package com.tetron.packetron

class ProtocolMessage(ip: String, prt: String, msg: String) {
    var messageText: String = msg
    var messagePort: String = prt
    var messageIp: String = ip


    override fun toString(): String {
        return "$messageIp:$messagePort\t$messageText"
    }
}