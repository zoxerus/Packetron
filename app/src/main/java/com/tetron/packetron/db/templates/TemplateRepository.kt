package com.tetron.packetron.db.templates

import androidx.lifecycle.LiveData

// Declares the DAO as a private property in the constructor. Pass in the DAO
// instead of the whole database, because you only need access to the DAO
class TemplateRepository(private val msgDao: TemplateDao) {

    // Room executes all queries on a separate thread.
    // Observed LiveData will notify the observer when the data has changed.
    val allMessages: LiveData<List<MessageTemplate>> = msgDao.getAll()

    suspend fun insert(msg: MessageTemplate) {
        msgDao.insert(msg)
    }

    fun getAll(): LiveData<List<MessageTemplate>> {
        return allMessages
    }

    fun delete(msg: MessageTemplate) {
        msgDao.delete(msg)
    }

    fun deleteMany(messages: List<Long>) {
        msgDao.deleteMany(messages)
    }

    fun deleteAll() {
        msgDao.deleteAll()
    }
}