package com.termproject.sprintyou.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [MainGoal::class, SprintRecord::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SprintDatabase : RoomDatabase() {
    abstract fun sprintRecordDao(): SprintRecordDao
    abstract fun mainGoalDao(): MainGoalDao
}