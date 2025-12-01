package com.termproject.sprintyou.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [SprintRecord::class],
    version = 1,
    exportSchema = false
)
abstract class SprintDatabase : RoomDatabase() {
    abstract fun sprintRecordDao(): SprintRecordDao
}