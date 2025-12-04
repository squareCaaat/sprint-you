package com.termproject.sprintyou.ui.home

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.termproject.sprintyou.R
import com.termproject.sprintyou.auth.AuthManager
import com.termproject.sprintyou.data.GoalWithProgress
import com.termproject.sprintyou.data.MainGoalStatus
import com.termproject.sprintyou.data.SprintDatabaseProvider
import com.termproject.sprintyou.databinding.ActivityHomeBinding
import com.termproject.sprintyou.sync.FirebaseSyncManager
import com.termproject.sprintyou.ui.auth.LoginActivity
import com.termproject.sprintyou.ui.goal.GoalSettingActivity
import com.termproject.sprintyou.ui.history.CalendarActivity
import com.termproject.sprintyou.ui.navigation.IntentKeys
import com.termproject.sprintyou.ui.setup.SetupActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private val database by lazy { SprintDatabaseProvider.getDatabase(this) }
    private var activeGoal: GoalWithProgress? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bindClicks()
        updateLoginHeader()
    }

    override fun onResume() {
        super.onResume()
        updateLoginHeader()
        maybeSyncRemoteThenLoad()
    }

    private fun bindClicks() {
        binding.btnCalendar.setOnClickListener {
            startActivity(Intent(this, CalendarActivity::class.java))
        }
        binding.btnProfile.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
        binding.btnCreateGoal.setOnClickListener {
            startActivity(Intent(this, GoalSettingActivity::class.java))
        }
        binding.btnStartSprint.setOnClickListener {
            activeGoal?.let { goal ->
                val intent = Intent(this, SetupActivity::class.java).apply {
                    putExtra(IntentKeys.EXTRA_MAIN_GOAL_ID, goal.goal.goalId)
                    putExtra(IntentKeys.EXTRA_MAIN_GOAL_TITLE, goal.goal.title)
                }
                startActivity(intent)
            } ?: Toast.makeText(this, R.string.toast_need_active_goal, Toast.LENGTH_SHORT).show()
        }
        binding.btnCompleteGoal.setOnClickListener {
            activeGoal?.goal?.goalId?.let { showCompleteGoalDialog(it) }
        }
    }

    private fun loadActiveGoal() {
        lifecycleScope.launch {
            val goalWithProgress = withContext(Dispatchers.IO) {
                database.mainGoalDao().getActiveGoalWithProgress()
            }
            activeGoal = goalWithProgress
            updateGoalState(goalWithProgress)
        }
    }

    private fun maybeSyncRemoteThenLoad() {
        if (!AuthManager.isFirebaseReady || !AuthManager.isLoggedIn) {
            loadActiveGoal()
            return
        }
        lifecycleScope.launch {
            runCatching {
                FirebaseSyncManager.pullRemoteData(applicationContext)
            }
            loadActiveGoal()
        }
    }

    private fun updateGoalState(goalWithProgress: GoalWithProgress?) {
        val hasGoal = goalWithProgress != null
        binding.layoutEmptyGoal.isVisible = !hasGoal
        binding.layoutActiveGoal.isVisible = hasGoal
        binding.btnStartSprint.isEnabled = hasGoal
        updateLoginHeader()

        goalWithProgress?.let { data ->
            val goal = data.goal
            val completed = data.completedSprints
            binding.tvActiveGoalTitle.text = goal.title

            val total = goal.totalSprints
            if (total != null && total > 0) {
                binding.progressGoal.isIndeterminate = false
                binding.progressGoal.max = total
                binding.progressGoal.setProgressCompat(
                    completed.coerceAtMost(total),
                    false
                )
                binding.tvProgressSummary.text =
                    getString(R.string.home_progress_with_total, completed, total)
                binding.tvProgressDetail.text =
                    getString(R.string.home_progress_detail_with_total, completed, total)
            } else {
                val displayMax = (completed + 1).coerceAtLeast(1)
                binding.progressGoal.isIndeterminate = false
                binding.progressGoal.max = displayMax
                binding.progressGoal.setProgressCompat(completed.coerceAtMost(displayMax), false)
                binding.tvProgressSummary.text =
                    getString(R.string.home_progress_without_total, completed)
                binding.tvProgressDetail.text =
                    getString(R.string.home_progress_detail_without_total, completed)
            }
        }
    }

    private fun showCompleteGoalDialog(goalId: Long) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_complete_goal_title)
            .setMessage(R.string.dialog_complete_goal_message)
            .setPositiveButton(R.string.dialog_complete_goal_positive) { _, _ ->
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        val now = System.currentTimeMillis()
                        database.mainGoalDao().updateGoalStatus(
                            goalId = goalId,
                            status = MainGoalStatus.COMPLETED,
                            completedAt = now,
                            lastModified = now
                        )
                    }
                    Toast.makeText(this@HomeActivity, R.string.toast_goal_completed, Toast.LENGTH_SHORT)
                        .show()
                    if (AuthManager.isLoggedIn) {
                        runCatching { FirebaseSyncManager.pushLocalData(applicationContext) }
                    }
                    loadActiveGoal()
                }
            }
            .setNegativeButton(R.string.dialog_complete_goal_negative, null)
            .show()
    }

    private fun updateLoginHeader() {
        val headerText = if (AuthManager.isLoggedIn) {
            val email = AuthManager.currentUserEmail ?: getString(R.string.login_unknown_user)
            getString(R.string.home_header_signed_in, email.substringBefore("@"))
        } else {
            getString(R.string.home_header)
        }
        binding.tvHomeHeader.text = headerText
    }
}