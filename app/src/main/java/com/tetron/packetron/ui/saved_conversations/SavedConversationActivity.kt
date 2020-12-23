package com.tetron.packetron.ui.saved_conversations

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tetron.packetron.R
import com.tetron.packetron.db.conversations.ConversationViewModel
import com.tetron.packetron.ui.message_templates.MessageAdapter

class SavedConversationActivity : AppCompatActivity() {

    private lateinit var conversationViewModel: ConversationViewModel
    private lateinit var adapter: MessageAdapter
    private lateinit var recyclerView: RecyclerView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_saved_conversation)

        recyclerView = findViewById(R.id.rv_saved_conversation)
        adapter = MessageAdapter(this, {}, { _, _ ->

        })
        recyclerView.layoutManager = LinearLayoutManager(this)
        conversationViewModel = ViewModelProvider(this).get(ConversationViewModel::class.java)
        conversationViewModel.allConversations.observe(this, { conversations ->
            conversations?.let {
                adapter.setMessages(emptyList())
            }
        })

        recyclerView.adapter = adapter
    }
}