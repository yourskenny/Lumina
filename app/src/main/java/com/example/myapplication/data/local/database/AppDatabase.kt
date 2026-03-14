package com.example.myapplication.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.myapplication.data.local.dao.MemoryDao
import com.example.myapplication.data.local.entity.ChatMessage
import com.example.myapplication.data.local.entity.SceneLog

@Database(entities = [ChatMessage::class, SceneLog::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lumina_memory_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
