package com.tetron.packetron.ui.saved_conversations

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tetron.packetron.R
import com.tetron.packetron.db.conversations.ConversationViewModel


class ConversationListFragment(rf1: (Fragment) -> Unit): Fragment() {

    private lateinit var conversationViewModel: ConversationViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter1: StringAdapter
    private lateinit var viewManager: RecyclerView.LayoutManager
    private val replaceFragment = rf1


    companion object {
        fun newInstance(): ConversationListFragment {
            return ConversationListFragment(){}
        }
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

        return inflater.inflate(R.layout.fragment_conversation_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewManager = LinearLayoutManager(requireContext().applicationContext)
        // specify the behaviour when the user clicks a message from the message list
        adapter1 = StringAdapter(requireContext()) { it, ->

            val conversationFragment =
                ConversationFragment.newInstance()
            conversationFragment.setConversation(it)
            replaceFragment(conversationFragment)

        }


        conversationViewModel.conversationsTable.observe(
            viewLifecycleOwner,
             {
                adapter1.setConversations(it)
            })


        recyclerView = view.findViewById<RecyclerView>(R.id.rv_saved_conversation_list).apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = adapter1
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.mm_message_templates_action_delete -> {
                val deleteMessages = adapter1.checkedMessages.toList()
                conversationViewModel.deleteMany( deleteMessages)
                adapter1.checkedMessages.clear()
            }
            R.id.mm_message_templates_action_delete_all -> {
                conversationViewModel.deleteAll()
            }
        }
        return super.onOptionsItemSelected(item)
    }
}