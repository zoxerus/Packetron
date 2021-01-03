package com.tetron.packetron.ui.saved_conversations

import android.os.Bundle
import android.util.Log
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.tetron.packetron.R

class SavedConversationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_saved_conversation)

        val conversationListFragment = ConversationListFragment(){
            replaceFragment(it)
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.saved_conversation_fragment, conversationListFragment, "")
            .commit()
    }

    private fun replaceFragment(fc: Fragment){
        supportFragmentManager.beginTransaction()
            .replace(R.id.saved_conversation_fragment, fc, "")
            .commit()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.message_template_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }
}