package com.tetron.packetron.db.conversations

import androidx.lifecycle.LiveData


class ConversationRepository(private val conversationDao: ConversationDao) {

    val allConversations: LiveData<List<ConversationMessage>> = conversationDao.getAll()

    suspend fun insert(msg: ConversationMessage) {
        conversationDao.insert(msg)
    }

    suspend fun insertMany(msgList: List<ConversationMessage>) {
        conversationDao.insertMany(msgList)
    }

    fun getAll(): LiveData<List<ConversationMessage>> {
        return allConversations
    }


}