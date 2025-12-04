package com.termproject.sprintyou.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface SprintRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: SprintRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<SprintRecord>)

    @Query("SELECT * FROM sprint_records ORDER BY created_at DESC")
    suspend fun getAllRecords(): List<SprintRecord>

    @Query("SELECT * FROM sprint_records WHERE owner_uid = :ownerId ORDER BY created_at DESC")
    suspend fun getRecordsByOwner(ownerId: String): List<SprintRecord>

    @Query("UPDATE sprint_records SET owner_uid = :ownerUid WHERE owner_uid IS NULL")
    suspend fun claimRecordsWithoutOwner(ownerUid: String)

    @Query(
        """
        SELECT s.sprint_id AS sprint_id,
               s.parent_goal_id AS parent_goal_id,
               s.task_content AS task_content,
               s.target_duration AS target_duration,
               s.actual_duration AS actual_duration,
               s.created_at AS created_at,
               s.firebase_id AS firebase_id,
               s.owner_uid AS owner_uid,
               s.last_modified AS last_modified,
               s.is_synced AS is_synced,
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
               s.firebase_id AS firebase_id,
               s.owner_uid AS owner_uid,
               s.last_modified AS last_modified,
               s.is_synced AS is_synced,
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

    @Query("UPDATE sprint_records SET is_synced = :synced WHERE owner_uid = :ownerUid")
    suspend fun markRecordsSynced(ownerUid: String, synced: Boolean)

    @Query("DELETE FROM sprint_records")
    suspend fun clearAll()

    @Transaction
    suspend fun replaceAll(records: List<SprintRecord>) {
        clearAll()
        if (records.isNotEmpty()) {
            insertAll(records)
        }
    }
}

data class SprintHistoryItem(
    @Embedded
    val record: SprintRecord,
    @ColumnInfo(name = "main_goal_title")
    val mainGoalTitle: String?
)