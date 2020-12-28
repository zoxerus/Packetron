package com.tetron.packetron.db.templates

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface TemplateDao {

    @Query("SELECT * FROM message_templates")
    fun getAll(): LiveData<List<MessageTemplate>>

    @Query("SELECT * FROM message_templates WHERE id IN (:msgIds)")
    fun loadAllByIds(msgIds: IntArray): List<MessageTemplate>

    @Insert
    fun insertAll(vararg messages: MessageTemplate)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(msg: MessageTemplate)

    @Delete
    fun delete(msg: MessageTemplate)

    @Query("DELETE FROM message_templates where id in (:idList)")
    fun deleteMany(idList: List<Long>)

    @Query("DELETE FROM message_templates")
    fun deleteAll()
}