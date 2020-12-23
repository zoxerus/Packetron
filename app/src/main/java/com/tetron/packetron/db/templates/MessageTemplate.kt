package com.tetron.packetron.db.templates

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "message_templates")
data class MessageTemplate(
    @PrimaryKey val id: Long
) {
    @ColumnInfo(name = "message")
    var message: String? = null
}
