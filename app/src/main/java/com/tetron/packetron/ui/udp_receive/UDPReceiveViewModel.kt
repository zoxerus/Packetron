package com.tetron.packetron.ui.udp_receive

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class UDPReceiveViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is udp_receive Fragment"
    }
    val text: LiveData<String> = _text
}