package com.tetron.packetron.db.conversations

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversation_list")
data class ConversationsTable(
    @ColumnInfo val name:String,
    @ColumnInfo val fromTime:Long,
    @ColumnInfo val toTime:Long
    ){
    @PrimaryKey(autoGenerate = true) var id: Int = 0
}