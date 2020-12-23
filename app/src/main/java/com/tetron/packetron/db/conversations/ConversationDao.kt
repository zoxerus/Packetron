package com.tetron.packetron.db.conversations

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface ConversationDao {

    @Query("SELECT * FROM saved_conversations")
    fun getAll(): LiveData<List<ConversationMessage>>

    @Query("SELECT name FROM saved_conversations")
    fun getNames(): List<ConversationMessage>

    @Transaction
    suspend fun insertMany(objects: List<ConversationMessage>) = objects.forEach { insert(it) }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(msg: ConversationMessage)

}