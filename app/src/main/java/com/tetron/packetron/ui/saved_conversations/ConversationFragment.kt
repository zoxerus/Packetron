package com.tetron.packetron.ui.saved_conversations

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tetron.packetron.R
import com.tetron.packetron.db.conversations.ConversationViewModel
import com.tetron.packetron.db.conversations.ConversationsTable
import com.tetron.packetron.ui.ResponseAdapter


class ConversationFragment: Fragment() {
    private lateinit var conversationTable: ConversationsTable
    private lateinit var conversationViewModel: ConversationViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter1: ResponseAdapter
    private lateinit var viewManager: RecyclerView.LayoutManager

    companion object {
        fun newInstance(): ConversationFragment {
            return ConversationFragment()
        }
    }

    fun setConversation(ct:ConversationsTable ){
        conversationTable = ct
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        conversationViewModel = ViewModelProvider(this).get(ConversationViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(R.layout.fragment_conversations, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewManager = LinearLayoutManager(requireContext().applicationContext)
        // specify the behaviour when the user clicks a message from the message list
        adapter1 = ResponseAdapter( emptyList() ){
        }
        conversationViewModel.getConversationByIdRange(conversationTable.fromTime,conversationTable.toTime).observe(
            viewLifecycleOwner,
            {
                adapter1.setResponses(it)
                Log.e("ddddddd", it[0].toString())
            })


        recyclerView = view.findViewById<RecyclerView>(R.id.rv_saved_conversation).apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = adapter1
        }
    }


}