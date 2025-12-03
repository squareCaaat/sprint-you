package com.termproject.sprintyou.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "main_goals")
data class MainGoal(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "goal_id")
    val goalId: Long = 0,
    val title: String,
    val status: MainGoalStatus = MainGoalStatus.ACTIVE,
    @ColumnInfo(name = "total_sprints")
    val totalSprints: Int? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "completed_at")
    val completedAt: Long? = null
)