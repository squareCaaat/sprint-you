package com.termproject.sprintyou.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sprint_records")
data class SprintRecord(
    @PrimaryKey(autoGenerate = true)
    val uid: Int = 0,
    @ColumnInfo(name = "goal_content")
    val goalContent: String,
    @ColumnInfo(name = "target_duration")
    val targetDurationSeconds: Long,
    @ColumnInfo(name = "actual_duration")
    val actualDurationSeconds: Long,
    @ColumnInfo(name = "created_at")
    val createdAt: Long
)