package com.tetron.packetron.db.conversations

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface ConversationDao {

    @Query("SELECT * FROM saved_conversations")
    fun getAll(): LiveData<List<ConversationMessage>>

    @Query("SELECT * FROM conversation_list")
    fun getConversationList(): LiveData<List<ConversationsTable>>

    @Query("SELECT * FROM saved_conversations WHERE timeId >= :fromTime AND timeId <= :toTime;")
    fun getMessagesByIdRange(fromTime:Long, toTime:Long):LiveData<List<ConversationMessage>>


    @Transaction
    suspend fun insertMany(objects: List<ConversationMessage>) = objects.forEach { insert(it) }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(msg: ConversationMessage)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertConversation(msg: ConversationsTable)

    @Query("DELETE FROM conversation_list where id in (:idList)")
    fun deleteMany(idList: List<Int>)

    @Query("DELETE FROM conversation_list")
    fun deleteAll()

}