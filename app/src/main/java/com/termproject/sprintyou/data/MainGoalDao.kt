package com.termproject.sprintyou.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface MainGoalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: MainGoal): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(goals: List<MainGoal>)

    @Query(
        """
        SELECT g.*,
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

    @Query("SELECT * FROM main_goals ORDER BY created_at DESC")
    suspend fun getAllGoals(): List<MainGoal>

    @Query("SELECT * FROM main_goals WHERE owner_uid = :ownerId ORDER BY created_at DESC")
    suspend fun getGoalsByOwner(ownerId: String): List<MainGoal>

    @Query("UPDATE main_goals SET owner_uid = :ownerUid WHERE owner_uid IS NULL")
    suspend fun claimGoalsWithoutOwner(ownerUid: String)

    @Query("UPDATE main_goals SET status = :status, completed_at = :completedAt, last_modified = :lastModified, is_synced = 0 WHERE goal_id = :goalId")
    suspend fun updateGoalStatus(goalId: Long, status: MainGoalStatus, completedAt: Long?, lastModified: Long)

    @Query("UPDATE main_goals SET status = :newStatus, last_modified = :lastModified, is_synced = 0 WHERE status = :currentStatus")
    suspend fun updateStatusFor(currentStatus: MainGoalStatus, newStatus: MainGoalStatus, lastModified: Long)

    @Query("UPDATE main_goals SET is_synced = :synced WHERE owner_uid = :ownerUid")
    suspend fun markGoalsSynced(ownerUid: String, synced: Boolean)

    @Query("DELETE FROM main_goals")
    suspend fun clearAll()

    @Transaction
    suspend fun replaceAll(goals: List<MainGoal>) {
        clearAll()
        if (goals.isNotEmpty()) {
            insertAll(goals)
        }
    }
}

data class GoalWithProgress(
    @Embedded
    val goal: MainGoal,
    @ColumnInfo(name = "completed_sprints")
    val completedSprints: Int
)