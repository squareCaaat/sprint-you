package com.termproject.sprintyou.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sprint_records",
    foreignKeys = [
        ForeignKey(
            entity = MainGoal::class,
            parentColumns = ["goal_id"],
            childColumns = ["parent_goal_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["parent_goal_id"])
    ]
)
data class SprintRecord(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "sprint_id")
    val sprintId: Long = 0,
    @ColumnInfo(name = "parent_goal_id")
    val parentGoalId: Long,
    @ColumnInfo(name = "task_content")
    val taskContent: String,
    @ColumnInfo(name = "target_duration")
    val targetDurationSeconds: Long,
    @ColumnInfo(name = "actual_duration")
    val actualDurationSeconds: Long,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "firebase_id")
    val firebaseId: String? = null,
    @ColumnInfo(name = "owner_uid")
    val ownerUid: String? = null,
    @ColumnInfo(name = "last_modified")
    val lastModified: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "is_synced")
    val isSynced: Boolean = false
)