package com.tetron.packetron.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.tetron.packetron.db.conversations.ConversationDao
import com.tetron.packetron.db.conversations.ConversationMessage
import com.tetron.packetron.db.conversations.ConversationsTable
import com.tetron.packetron.db.templates.MessageTemplate
import com.tetron.packetron.db.templates.TemplateDao

@Database(entities = [MessageTemplate::class,ConversationsTable::class, ConversationMessage::class], version = 3)
abstract class AppDatabase : RoomDatabase() {
    abstract fun msgDao(): TemplateDao
    abstract fun conversationDao(): ConversationDao


    companion object {
        // Singleton prevents multiple instances of database opening at the
        // same time.
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "Packetron_Database"
                ).build()
                INSTANCE = instance
                return instance
            }
        }
    }
}