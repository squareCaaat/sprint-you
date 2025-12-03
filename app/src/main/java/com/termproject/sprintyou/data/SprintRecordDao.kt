package com.termproject.sprintyou.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SprintRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: SprintRecord)

    @Query(
        """
        SELECT s.sprint_id AS sprint_id,
               s.parent_goal_id AS parent_goal_id,
               s.task_content AS task_content,
               s.target_duration AS target_duration,
               s.actual_duration AS actual_duration,
               s.created_at AS created_at,
               g.title AS main_goal_title
        FROM sprint_records s
        LEFT JOIN main_goals g ON s.parent_goal_id = g.goal_id
        ORDER BY s.created_at DESC
        """
    )
    suspend fun getAllWithGoals(): List<SprintHistoryItem>

    @Query(
        """
        SELECT s.sprint_id AS sprint_id,
               s.parent_goal_id AS parent_goal_id,
               s.task_content AS task_content,
               s.target_duration AS target_duration,
               s.actual_duration AS actual_duration,
               s.created_at AS created_at,
               g.title AS main_goal_title
        FROM sprint_records s
        LEFT JOIN main_goals g ON s.parent_goal_id = g.goal_id
        WHERE s.created_at BETWEEN :startMillis AND :endMillis
        ORDER BY s.created_at DESC
        """
    )
    suspend fun getBetween(startMillis: Long, endMillis: Long): List<SprintHistoryItem>

    @Query("SELECT COUNT(*) FROM sprint_records WHERE parent_goal_id = :goalId")
    suspend fun countForGoal(goalId: Long): Int
}

data class SprintHistoryItem(
    @Embedded
    val record: SprintRecord,
    @ColumnInfo(name = "main_goal_title")
    val mainGoalTitle: String?
)