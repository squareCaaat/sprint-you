package com.termproject.sprintyou.data

import android.content.Context
import androidx.room.Room

object SprintDatabaseProvider {
    @Volatile
    private var INSTANCE: SprintDatabase? = null

    fun getDatabase(context: Context): SprintDatabase {
        return INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                SprintDatabase::class.java,
                "sprint_db"
            ).build().also { INSTANCE = it }
        }
    }
}