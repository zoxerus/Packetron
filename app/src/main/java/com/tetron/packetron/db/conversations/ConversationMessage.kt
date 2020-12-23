package com.tetron.packetron.db.conversations

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.net.Socket
import java.text.SimpleDateFormat

@Entity(tableName = "saved_conversations")
data class ConversationMessage(
    @PrimaryKey val timeId: Long,
    @ColumnInfo(name = "message") val message: String,
    @ColumnInfo(name = "direction") val direction: Int,
    @ColumnInfo(name = "name") var name: String,
    @ColumnInfo(name = "receiver_ip") val remoteIp: String,
    @ColumnInfo(name = "out_port") val localPort: String,
    @ColumnInfo(name = "in_port") val remotePort: String,
    @ColumnInfo(name = "sender_ip") val localIp: String
) {
    @Ignore
    private val sdf = SimpleDateFormat.getTimeInstance()

    @Ignore
    var socket: Socket? = null

    fun addressToString(): String {
        return if (direction == 0) {
            "$localIp:$localPort:\t\t\t\t${sdf.format(timeId)}"
        } else {
            "$remoteIp:$remotePort:\t\t\t\t${sdf.format(timeId)}"
        }
    }

    override fun toString(): String {
        return "$remoteIp:$remotePort: \t$timeId"
    }

    fun print(): String {
        return "\nTime ID: $timeId\nDirection: $direction\nMessage: $message\nLocal IP: " +
                "$localIp\nLocal Port: $localPort\nRemote IP: $remoteIp\nRemote Port: " +
                "$remotePort\nName: ${name}\nSocket: ${socket.toString()}"
    }

}