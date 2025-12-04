package com.termproject.sprintyou.sync

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.Firebase
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.PropertyName
import com.google.firebase.database.database
import com.termproject.sprintyou.auth.AuthManager
import com.termproject.sprintyou.auth.FirebaseScopeResolver
import com.termproject.sprintyou.data.MainGoal
import com.termproject.sprintyou.data.MainGoalStatus
import com.termproject.sprintyou.data.SprintDatabaseProvider
import com.termproject.sprintyou.data.SprintRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object FirebaseSyncManager {

    private const val TAG = "FirebaseSyncManager"
    private val database: DatabaseReference by lazy {
        Firebase.database("https://sprint-you-db-default-rtdb.asia-southeast1.firebasedatabase.app").reference
    }

    suspend fun pushLocalData(context: Context) {
        if (!AuthManager.isFirebaseReady || !AuthManager.isLoggedIn || AuthManager.currentUserId == null) {
            Log.d(TAG, "pushLocalData skipped: auth not ready")
            return
        }

        val scope = FirebaseScopeResolver.resolve(context)
        val ownerId = scope.scopeId
        Log.d(TAG, "pushLocalData scope=${scope.root}/${scope.scopeId}")
        val localData = withContext(Dispatchers.IO) {
            val db = SprintDatabaseProvider.getDatabase(context)
            db.mainGoalDao().claimGoalsWithoutOwner(ownerId)
            db.sprintRecordDao().claimRecordsWithoutOwner(ownerId)
            val goals = db.mainGoalDao().getGoalsByOwner(ownerId).associateBy(
                { it.goalId.toString() },
                { RemoteMainGoalDto.from(it) }
            )
            val sprints = db.sprintRecordDao().getRecordsByOwner(ownerId).associateBy(
                { it.sprintId.toString() },
                { RemoteSprintRecordDto.from(it) }
            )
            mapOf(
                "goals" to goals,
                "sprints" to sprints
            )
        }

        Log.d(
            TAG,
            "pushLocalData path=${scope.root}/${scope.scopeId} goals=${(localData["goals"] as? Map<*, *>)?.size} sprints=${(localData["sprints"] as? Map<*, *>)?.size}"
        )

        try {
            database
                .child(scope.root)
                .child(scope.scopeId)
                .setValue(localData)
                .await()
            Log.d(TAG, "pushLocalData success for ${scope.scopeId}")
            withContext(Dispatchers.IO) {
                val db = SprintDatabaseProvider.getDatabase(context)
                db.mainGoalDao().markGoalsSynced(ownerId, true)
                db.sprintRecordDao().markRecordsSynced(ownerId, true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "pushLocalData failed", e)
            throw e
        }
    }

    fun scheduleSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork("sync_work", ExistingWorkPolicy.KEEP, syncRequest)
    }

    suspend fun pullRemoteData(context: Context) {
        if (!AuthManager.isFirebaseReady || !AuthManager.isLoggedIn || AuthManager.currentUserId == null) {
            Log.d(TAG, "pullRemoteData skipped: auth not ready")
            return
        }

        val scope = FirebaseScopeResolver.resolve(context)
        Log.d(TAG, "pullRemoteData scope=${scope.root}/${scope.scopeId}")
        val snapshot = database
            .child(scope.root)
            .child(scope.scopeId)
            .get()
            .await()

        if (!snapshot.exists()) {
            Log.d(TAG, "pullRemoteData: no remote snapshot")
            return
        }

        val goalNodes = snapshot.child("goals").children
        val sprintNodes = snapshot.child("sprints").children

        val remoteGoals = goalNodes.mapNotNull { child ->
            child.getValue(RemoteMainGoalDto::class.java)?.toEntity()
        }

        val remoteSprints = sprintNodes.mapNotNull { child ->
            child.getValue(RemoteSprintRecordDto::class.java)?.toEntity()
        }

//        if (remoteGoals.isEmpty() && remoteSprints.isEmpty()) return

        val ownerId = scope.scopeId
        val normalizedGoals = remoteGoals.map {
            it.copy(ownerUid = ownerId, isSynced = true)
        }
        val normalizedSprints = remoteSprints.map {
            it.copy(ownerUid = ownerId, isSynced = true)
        }

        try {
            withContext(Dispatchers.IO) {
                val db = SprintDatabaseProvider.getDatabase(context)
                db.withTransaction {
                    db.mainGoalDao().replaceAll(normalizedGoals)
                    db.sprintRecordDao().replaceAll(normalizedSprints)
                }
            }
            Log.d(TAG, "pullRemoteData success for ${scope.scopeId}")
        } catch (e: Exception) {
            Log.e(TAG, "pullRemoteData failed", e)
        }
    }
}

private data class RemoteMainGoalDto(
    var goalId: Long? = null,
    var title: String? = null,
    var status: String? = null,
    var totalSprints: Int? = null,
    var createdAt: Long? = null,
    var completedAt: Long? = null,
    var firebaseId: String? = null,
    var ownerUid: String? = null,
    var lastModified: Long? = null,
    @get:PropertyName("isSynced") @set:PropertyName("isSynced")
    var isSynced: Boolean? = null
) {
    fun toEntity(): MainGoal? {
        val id = goalId ?: return null
        val titleValue = title ?: return null
        val statusValue = status ?: MainGoalStatus.ACTIVE.name
        return MainGoal(
            goalId = id,
            title = titleValue,
            status = runCatching { MainGoalStatus.valueOf(statusValue) }.getOrDefault(MainGoalStatus.ACTIVE),
            totalSprints = totalSprints,
            createdAt = createdAt ?: System.currentTimeMillis(),
            completedAt = completedAt,
            firebaseId = firebaseId,
            ownerUid = ownerUid,
            lastModified = lastModified ?: System.currentTimeMillis(),
            isSynced = isSynced ?: true
        )
    }

    companion object {
        fun from(goal: MainGoal): RemoteMainGoalDto =
            RemoteMainGoalDto(
                goalId = goal.goalId,
                title = goal.title,
                status = goal.status.name,
                totalSprints = goal.totalSprints,
                createdAt = goal.createdAt,
                completedAt = goal.completedAt,
                firebaseId = goal.firebaseId,
                ownerUid = goal.ownerUid,
                lastModified = goal.lastModified,
                isSynced = goal.isSynced
            )
    }
}

private data class RemoteSprintRecordDto(
    var sprintId: Long? = null,
    var parentGoalId: Long? = null,
    var taskContent: String? = null,
    var targetDurationSeconds: Long? = null,
    var actualDurationSeconds: Long? = null,
    var createdAt: Long? = null,
    var firebaseId: String? = null,
    var ownerUid: String? = null,
    var lastModified: Long? = null,
    @get:PropertyName("isSynced") @set:PropertyName("isSynced")
    var isSynced: Boolean? = null
) {
    fun toEntity(): SprintRecord? {
        val sid = sprintId ?: return null
        val goalId = parentGoalId ?: return null
        val content = taskContent ?: return null
        val target = targetDurationSeconds ?: return null
        val actual = actualDurationSeconds ?: return null
        val created = createdAt ?: return null
        return SprintRecord(
            sprintId = sid,
            parentGoalId = goalId,
            taskContent = content,
            targetDurationSeconds = target,
            actualDurationSeconds = actual,
            createdAt = created,
            firebaseId = firebaseId,
            ownerUid = ownerUid,
            lastModified = lastModified ?: System.currentTimeMillis(),
            isSynced = isSynced ?: true
        )
    }

    companion object {
        fun from(record: SprintRecord): RemoteSprintRecordDto =
            RemoteSprintRecordDto(
                sprintId = record.sprintId,
                parentGoalId = record.parentGoalId,
                taskContent = record.taskContent,
                targetDurationSeconds = record.targetDurationSeconds,
                actualDurationSeconds = record.actualDurationSeconds,
                createdAt = record.createdAt,
                firebaseId = record.firebaseId,
                ownerUid = record.ownerUid,
                lastModified = record.lastModified,
                isSynced = record.isSynced
            )
    }
}