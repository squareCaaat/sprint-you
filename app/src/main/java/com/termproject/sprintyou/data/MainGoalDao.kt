package com.termproject.sprintyou.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MainGoalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: MainGoal): Long

    @Query(
        """
        SELECT g.goal_id AS goal_id,
               g.title AS title,
               g.status AS status,
               g.total_sprints AS total_sprints,
               g.created_at AS created_at,
               g.completed_at AS completed_at,
               COUNT(s.sprint_id) AS completed_sprints
        FROM main_goals g
        LEFT JOIN sprint_records s ON g.goal_id = s.parent_goal_id
        WHERE g.status = :status
        GROUP BY g.goal_id
        ORDER BY g.created_at DESC
        LIMIT 1
        """
    )
    suspend fun getActiveGoalWithProgress(status: MainGoalStatus = MainGoalStatus.ACTIVE): GoalWithProgress?

    @Query("SELECT * FROM main_goals WHERE status = :status ORDER BY created_at DESC LIMIT 1")
    suspend fun getGoalByStatus(status: MainGoalStatus): MainGoal?

    @Query("UPDATE main_goals SET status = :status, completed_at = :completedAt WHERE goal_id = :goalId")
    suspend fun updateGoalStatus(goalId: Long, status: MainGoalStatus, completedAt: Long?)

    @Query("UPDATE main_goals SET status = :newStatus WHERE status = :currentStatus")
    suspend fun updateStatusFor(currentStatus: MainGoalStatus, newStatus: MainGoalStatus)
}

data class GoalWithProgress(
    @Embedded
    val goal: MainGoal,
    @ColumnInfo(name = "completed_sprints")
    val completedSprints: Int
)