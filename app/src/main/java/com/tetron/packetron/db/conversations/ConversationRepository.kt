package com.tetron.packetron.db.conversations

import androidx.lifecycle.LiveData


class ConversationRepository(private val conversationDao: ConversationDao) {

    val allConversations: LiveData<List<ConversationMessage>> = conversationDao.getAll()
    val conversationList: LiveData<List<ConversationsTable>> = conversationDao.getConversationList()

    suspend fun insert(msg: ConversationMessage) {
        conversationDao.insert(msg)
    }

    suspend fun insertConversation(msg: ConversationsTable) {
        conversationDao.insertConversation(msg)
    }

    suspend fun insertMany(msgList: List<ConversationMessage>) {
        conversationDao.insertMany(msgList)
    }

    fun getAll(): LiveData<List<ConversationMessage>> {
        return allConversations
    }

    fun getConversationByIdRange(from:Long, to:Long): LiveData<List<ConversationMessage>> {
        return conversationDao.getMessagesByIdRange(from,to)
    }

    fun deleteAll() {
        conversationDao.deleteConversationList()
        conversationDao.deleteSavedConversations()
    }

    fun deleteMany(list:List<Int>){
        conversationDao.deleteMany(list)
    }


}