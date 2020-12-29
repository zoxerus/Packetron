package com.tetron.packetron

import android.content.Context
import android.net.wifi.WifiManager
import java.net.InetAddress
import java.net.UnknownHostException

class ConnectionUtils {

    fun charToHex(chars: CharArray): String {
        val HEX_ARRAY = "0123456789ABCDEF".toCharArray()
        val hexChars = CharArray(chars.size * 2 )
        for (i: Int in chars.indices ){
            val v: Int = chars[i].toInt() and 0xFF
            hexChars[i*2] = HEX_ARRAY[v shr 4]
            hexChars[ i*2+1 ] = HEX_ARRAY[v and 0x0F]
        }
        return String(hexChars)
    }

    fun getInetAddress(context: Context): InetAddress {
        val wifiManager = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as WifiManager
        return intToInetAddress(wifiManager.dhcpInfo.ipAddress)
    }

    private fun intToInetAddress(hostAddress: Int): InetAddress {
        val addressBytes = byteArrayOf(
            (0xff and hostAddress).toByte(),
            (0xff and (hostAddress shr 8)).toByte(),
            (0xff and (hostAddress shr 16)).toByte(),
            (0xff and (hostAddress shr 24)).toByte()
        )
        return try {
            InetAddress.getByAddress(addressBytes)
        } catch (e: UnknownHostException) {
            throw AssertionError()
        }
    }
}