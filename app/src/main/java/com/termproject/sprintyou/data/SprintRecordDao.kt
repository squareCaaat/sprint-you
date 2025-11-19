package com.termproject.sprintyou.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SprintRecordDao {
    @Query("SELECT * FROM sprint_records ORDER BY created_at DESC")
    suspend fun getAll(): List<SprintRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: SprintRecord)
}



