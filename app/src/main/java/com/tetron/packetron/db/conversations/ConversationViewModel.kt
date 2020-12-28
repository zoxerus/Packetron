package com.tetron.packetron.db.conversations

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.tetron.packetron.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ConversationViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ConversationRepository

    // Using LiveData and caching what getAlphabetizedWords returns has several benefits:
    // - We can put an observer on the data (instead of polling for changes) and only update the
    //   the UI when the data actually changes.
    // - Repository is completely separated from the UI through the ViewModel.
    val allConversations: LiveData<List<ConversationMessage>>
    val conversationsTable: LiveData<List<ConversationsTable>>

    private val selectedConversations = emptyList<Long>()

    init {
        val conversationDao = AppDatabase.getDatabase(application).conversationDao()
        repository = ConversationRepository(conversationDao)
        allConversations = repository.allConversations
        conversationsTable = repository.conversationList
    }

    /**
     * Launching a new coroutine to insert the data in a non-blocking way
     */
    fun insert(msg: ConversationMessage) = viewModelScope.launch(Dispatchers.IO) {
        repository.insert(msg)
    }

    fun insertConversation(msg: ConversationsTable) = viewModelScope.launch(Dispatchers.IO) {
        repository.insertConversation(msg)
    }

    fun insertMany(msgList: List<ConversationMessage>) = viewModelScope.launch(Dispatchers.IO) {
        repository.insertMany(msgList)
    }

    fun getAll(): List<ConversationMessage>? {
        return repository.getAll().value
    }

    fun getConversationByIdRange(from:Long, to:Long): LiveData<List<ConversationMessage>> {
        return repository.getConversationByIdRange(from,to)
    }

    fun getConversationList(): LiveData<List<ConversationsTable>> {
        return repository.conversationList
    }

    fun deleteMany(messages: List<Int>) = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteMany(messages)
    }

    fun deleteAll() = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteAll()
    }


}