package com.termproject.sprintyou.data

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object SprintDatabaseProvider {
    @Volatile
    private var INSTANCE: SprintDatabase? = null

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `main_goals` (
                    `goal_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `title` TEXT NOT NULL,
                    `status` TEXT NOT NULL,
                    `total_sprints` INTEGER,
                    `created_at` INTEGER NOT NULL,
                    `completed_at` INTEGER
                )
                """.trimIndent()
            )

            database.execSQL(
                """
                INSERT INTO main_goals (goal_id, title, status, total_sprints, created_at, completed_at)
                VALUES (
                    1,
                    '이전 기록',
                    'COMPLETED',
                    NULL,
                    strftime('%s','now') * 1000,
                    NULL
                )
                """.trimIndent()
            )

            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `sprint_records_new` (
                    `sprint_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `parent_goal_id` INTEGER NOT NULL,
                    `task_content` TEXT NOT NULL,
                    `target_duration` INTEGER NOT NULL,
                    `actual_duration` INTEGER NOT NULL,
                    `created_at` INTEGER NOT NULL,
                    FOREIGN KEY(`parent_goal_id`) REFERENCES `main_goals`(`goal_id`) ON DELETE CASCADE
                )
                """.trimIndent()
            )

            database.execSQL(
                """
                INSERT INTO sprint_records_new (sprint_id, parent_goal_id, task_content, target_duration, actual_duration, created_at)
                SELECT uid, 1, goal_content, target_duration, actual_duration, created_at
                FROM sprint_records
                """.trimIndent()
            )

            database.execSQL("DROP TABLE sprint_records")
            database.execSQL("ALTER TABLE sprint_records_new RENAME TO sprint_records")
        }
    }
    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE main_goals ADD COLUMN firebase_id TEXT")
            database.execSQL("ALTER TABLE main_goals ADD COLUMN owner_uid TEXT")
            database.execSQL("ALTER TABLE main_goals ADD COLUMN last_modified INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE main_goals ADD COLUMN is_synced INTEGER NOT NULL DEFAULT 0")
            database.execSQL("UPDATE main_goals SET last_modified = strftime('%s','now') * 1000")

            database.execSQL("ALTER TABLE sprint_records ADD COLUMN firebase_id TEXT")
            database.execSQL("ALTER TABLE sprint_records ADD COLUMN owner_uid TEXT")
            database.execSQL("ALTER TABLE sprint_records ADD COLUMN last_modified INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE sprint_records ADD COLUMN is_synced INTEGER NOT NULL DEFAULT 0")
            database.execSQL("UPDATE sprint_records SET last_modified = strftime('%s','now') * 1000")
        }
    }

    fun getDatabase(context: Context): SprintDatabase {
        return INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                SprintDatabase::class.java,
                "sprint_db"
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
                .also { INSTANCE = it }
        }
    }
}