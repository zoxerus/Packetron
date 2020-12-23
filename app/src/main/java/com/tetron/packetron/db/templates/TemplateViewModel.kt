package com.tetron.packetron.db.templates

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.tetron.packetron.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TemplateViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TemplateRepository

    // Using LiveData and caching what getAlphabetizedWords returns has several benefits:
    // - We can put an observer on the data (instead of polling for changes) and only update the
    //   the UI when the data actually changes.
    // - Repository is completely separated from the UI through the ViewModel.
    val allMessages: LiveData<List<MessageTemplate>>

    private val selectedMessages = emptyList<Long>()

    init {
        val msgDao = AppDatabase.getDatabase(application).msgDao()
        repository = TemplateRepository(msgDao)
        allMessages = repository.allMessages
    }

    /**
     * Launching a new coroutine to insert the data in a non-blocking way
     */
    fun insert(msg: MessageTemplate) = viewModelScope.launch(Dispatchers.IO) {
        repository.insert(msg)
    }

    fun getAll(): List<MessageTemplate>? {
        return repository.getAll().value
    }

    fun delete(msg: MessageTemplate) = viewModelScope.launch(Dispatchers.IO) {
        repository.delete(msg)
    }

    fun deleteMany(messages: List<Long>) = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteMany(messages)
    }

    fun deleteAll() = viewModelScope.launch(Dispatchers.IO) {
        repository.deleteAll()
    }
}