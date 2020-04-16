package com.tetron.packetron.ui.udp_send_receive

import androidx.lifecycle.ViewModel
import java.util.*

class UDPSendReceiveViewModel : ViewModel() {

    private val _responses = ArrayList<String>()

    var responses: ArrayList<String> = _responses

}