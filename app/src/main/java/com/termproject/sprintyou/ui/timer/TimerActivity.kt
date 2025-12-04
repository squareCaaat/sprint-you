package com.termproject.sprintyou.ui.timer

import android.animation.ArgbEvaluator
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.os.SystemClock
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.termproject.sprintyou.R
import com.termproject.sprintyou.auth.AuthManager
import com.termproject.sprintyou.auth.FirebaseScopeResolver
import com.termproject.sprintyou.data.SprintDatabaseProvider
import com.termproject.sprintyou.data.SprintRecord
import com.termproject.sprintyou.databinding.ActivityTimerBinding
import com.termproject.sprintyou.sync.FirebaseSyncManager
import com.termproject.sprintyou.ui.home.HomeActivity
import com.termproject.sprintyou.ui.navigation.IntentKeys
import com.termproject.sprintyou.ui.setup.SetupActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TimerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTimerBinding
    private var sprintTask: String = ""
    private var targetDurationSeconds: Long = 0L
    private var targetDurationMillis: Long = 0L
    private var remainingMillis: Long = 0L
    private var mainGoalId: Long = -1L
    private var mainGoalTitle: String = ""
    private var timer: CountDownTimer? = null
    private var pausedAt: Long = 0L

    private val argbEvaluator = ArgbEvaluator()
    private var defaultBackgroundColor: Int = 0
    private var urgentBackgroundColor: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTimerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sprintTask = intent.getStringExtra(IntentKeys.EXTRA_SPRINT_TASK).orEmpty()
        targetDurationSeconds = intent.getLongExtra(IntentKeys.EXTRA_TARGET_DURATION_SECONDS, 0L)
        mainGoalId = intent.getLongExtra(IntentKeys.EXTRA_MAIN_GOAL_ID, -1L)
        mainGoalTitle = intent.getStringExtra(IntentKeys.EXTRA_MAIN_GOAL_TITLE).orEmpty()

        if (sprintTask.isEmpty() || targetDurationSeconds <= 0L || mainGoalId <= 0L || mainGoalTitle.isEmpty()) {
            finish()
            return
        }

        targetDurationMillis = targetDurationSeconds * 1000L
        remainingMillis = savedInstanceState?.getLong(KEY_REMAINING_MILLIS) ?: targetDurationMillis

        setupViews()
        bindClicks()
        updateCountdownText()
        updateProgress()
        updateBackgroundColor()
    }

    override fun onResume() {
        super.onResume()
        if (pausedAt > 0L) {
            val delta = SystemClock.elapsedRealtime() - pausedAt
            remainingMillis = (remainingMillis - delta).coerceAtLeast(0L)
            pausedAt = 0L
        }
        startTimer()
    }

    override fun onPause() {
        super.onPause()
        pausedAt = SystemClock.elapsedRealtime()
        timer?.cancel()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(KEY_REMAINING_MILLIS, remainingMillis)
    }

    private fun setupViews() {
        binding.tvGoalTitle.text = sprintTask
        binding.progressTimer.max = targetDurationMillis.toInt()
        binding.progressTimer.setProgressCompat(0, false)

        defaultBackgroundColor = MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorSurface)
        urgentBackgroundColor = ContextCompat.getColor(this, R.color.urgent_red)
    }

    private fun bindClicks() {
        binding.btnComplete.setOnClickListener { completeSprint() }
        binding.btnBack.setOnClickListener { showExitDialog() }
    }

    private fun startTimer() {
        timer?.cancel()
        if (remainingMillis <= 0L) {
            handleTimeout()
            return
        }
        timer = object : CountDownTimer(remainingMillis, TICK_INTERVAL) {
            override fun onTick(millisUntilFinished: Long) {
                remainingMillis = millisUntilFinished
                updateCountdownText()
                updateProgress()
                updateBackgroundColor()
            }

            override fun onFinish() {
                remainingMillis = 0L
                updateCountdownText()
                updateProgress()
                updateBackgroundColor()
                handleTimeout()
            }
        }.start()
    }

    private fun updateCountdownText() {
        val totalSeconds = (remainingMillis / 1000).coerceAtLeast(0)
        val minutes = (totalSeconds / 60).toInt()
        val seconds = (totalSeconds % 60).toInt()
        binding.tvCountdown.text = getString(R.string.timer_remaining_format, minutes, seconds)
    }

    private fun updateProgress() {
        val elapsedMillis = (targetDurationMillis - remainingMillis).coerceAtLeast(0)
        binding.progressTimer.setProgressCompat(elapsedMillis.toInt(), false)
    }

    private fun updateBackgroundColor() {
        if (remainingMillis <= 10000) {
            val fraction = (10000 - remainingMillis) / 10000f
            val color = argbEvaluator.evaluate(fraction, defaultBackgroundColor, urgentBackgroundColor) as Int
            binding.root.setBackgroundColor(color)
        } else {
            binding.root.setBackgroundColor(defaultBackgroundColor)
        }
    }

    private fun completeSprint() {
        timer?.cancel()
        val actualSeconds =
            ((targetDurationMillis - remainingMillis + 999) / 1000).coerceIn(1, targetDurationSeconds)
        lifecycleScope.launch {
            val now = System.currentTimeMillis()
            val ownerId = FirebaseScopeResolver.ownerId(this@TimerActivity)
            val record = SprintRecord(
                parentGoalId = mainGoalId,
                taskContent = sprintTask,
                targetDurationSeconds = targetDurationSeconds,
                actualDurationSeconds = actualSeconds,
                createdAt = now,
                ownerUid = ownerId,
                lastModified = now,
                isSynced = false
            )
            withContext(Dispatchers.IO) {
                SprintDatabaseProvider.getDatabase(this@TimerActivity)
                    .sprintRecordDao()
                    .insert(record)
            }
            Toast.makeText(this@TimerActivity, R.string.toast_success_saved, Toast.LENGTH_SHORT)
                .show()
            if (AuthManager.isLoggedIn) {
                runCatching { FirebaseSyncManager.pushLocalData(applicationContext) }
            }
            val intent = Intent(this@TimerActivity, HomeActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            startActivity(intent)
            finish()
        }
    }

    private fun handleTimeout() {
        navigateToSetup(withPrefill = true)
    }

    private fun showExitDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_quit_title)
            .setMessage(R.string.dialog_quit_message)
            .setPositiveButton(R.string.dialog_quit_positive) { _, _ ->
                navigateToSetup(withPrefill = true)
            }
            .setNegativeButton(R.string.dialog_quit_negative, null)
            .show()
    }

    private fun navigateToSetup(withPrefill: Boolean) {
        val intent = Intent(this, SetupActivity::class.java).apply {
            putExtra(IntentKeys.EXTRA_MAIN_GOAL_ID, mainGoalId)
            putExtra(IntentKeys.EXTRA_MAIN_GOAL_TITLE, mainGoalTitle)
            if (withPrefill) {
                putExtra(IntentKeys.EXTRA_SPRINT_TASK, sprintTask)
            }
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
        finish()
    }

    companion object {
        private const val KEY_REMAINING_MILLIS = "key_remaining_millis"
        private const val TICK_INTERVAL = 20L
    }
}