package com.termproject.sprintyou.sync

import android.content.Context
import androidx.room.withTransaction
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.termproject.sprintyou.data.MainGoal
import com.termproject.sprintyou.data.SprintDatabaseProvider
import com.termproject.sprintyou.data.SprintRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object SnapshotManager {

    private const val SNAPSHOT_FILE_NAME = "pre_login_snapshot.json"
    private val gson = Gson()

    private fun snapshotFile(context: Context): File =
        File(context.filesDir, SNAPSHOT_FILE_NAME)

    suspend fun saveSnapshot(context: Context) = withContext(Dispatchers.IO) {
        val db = SprintDatabaseProvider.getDatabase(context)
        val goals = db.mainGoalDao().getAllGoals()
        val sprints = db.sprintRecordDao().getAllRecords()
        val payload = SnapshotPayload(goals, sprints)
        snapshotFile(context).writeText(gson.toJson(payload))
    }

    suspend fun restoreSnapshot(context: Context) = withContext(Dispatchers.IO) {
        val file = snapshotFile(context)
        if (!file.exists()) return@withContext
        val json = file.readText()
        val payload: SnapshotPayload = gson.fromJson(
            json,
            object : TypeToken<SnapshotPayload>() {}.type
        )
        val db = SprintDatabaseProvider.getDatabase(context)
        db.withTransaction {
            db.mainGoalDao().replaceAll(payload.goals)
            db.sprintRecordDao().replaceAll(payload.sprints)
        }
    }

    fun hasSnapshot(context: Context): Boolean = snapshotFile(context).exists()

    fun clearSnapshot(context: Context) {
        snapshotFile(context).delete()
    }

    private data class SnapshotPayload(
        val goals: List<MainGoal>,
        val sprints: List<SprintRecord>
    )
}

