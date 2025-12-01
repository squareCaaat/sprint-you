package com.termproject.sprintyou.ui.timer

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.os.SystemClock
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.termproject.sprintyou.R
import com.termproject.sprintyou.data.SprintDatabaseProvider
import com.termproject.sprintyou.data.SprintRecord
import com.termproject.sprintyou.databinding.ActivityTimerBinding
import com.termproject.sprintyou.ui.goal.GoalSettingActivity
import com.termproject.sprintyou.ui.history.HistoryActivity
import com.termproject.sprintyou.ui.navigation.IntentKeys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TimerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTimerBinding
    private var goalContent: String = ""
    private var targetDurationSeconds: Long = 0L
    private var targetDurationMillis: Long = 0L
    private var remainingMillis: Long = 0L
    private var timer: CountDownTimer? = null
    private var pausedAt: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTimerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        goalContent = intent.getStringExtra(IntentKeys.EXTRA_GOAL_CONTENT).orEmpty()
        targetDurationSeconds = intent.getLongExtra(IntentKeys.EXTRA_TARGET_DURATION_SECONDS, 0L)

        if (goalContent.isEmpty() || targetDurationSeconds <= 0L) {
            finish()
            return
        }

        targetDurationMillis = targetDurationSeconds * 1000L
        remainingMillis = savedInstanceState?.getLong(KEY_REMAINING_MILLIS) ?: targetDurationMillis

        setupViews()
        bindClicks()
        updateCountdownText()
        updateProgress()
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
        binding.tvGoalTitle.text = goalContent
        binding.progressTimer.max = targetDurationSeconds.toInt()
        binding.progressTimer.setProgressCompat(0, false)
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
            }

            override fun onFinish() {
                remainingMillis = 0L
                updateCountdownText()
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
        val elapsedSeconds =
            ((targetDurationMillis - remainingMillis + 999) / 1000).toInt().coerceAtLeast(0)
        val maxProgress = binding.progressTimer.max.coerceAtLeast(1)
        binding.progressTimer.setProgressCompat(
            elapsedSeconds.coerceIn(0, maxProgress),
            false
        )
    }

    private fun completeSprint() {
        timer?.cancel()
        val actualSeconds =
            ((targetDurationMillis - remainingMillis + 999) / 1000).coerceIn(1, targetDurationSeconds)
        lifecycleScope.launch {
            val record = SprintRecord(
                goalContent = goalContent,
                targetDurationSeconds = targetDurationSeconds,
                actualDurationSeconds = actualSeconds,
                createdAt = System.currentTimeMillis()
            )
            withContext(Dispatchers.IO) {
                SprintDatabaseProvider.getDatabase(this@TimerActivity)
                    .sprintRecordDao()
                    .insert(record)
            }
            Toast.makeText(this@TimerActivity, R.string.toast_success_saved, Toast.LENGTH_SHORT)
                .show()
            startActivity(Intent(this@TimerActivity, HistoryActivity::class.java))
            finish()
        }
    }

    private fun handleTimeout() {
        navigateToGoal(withPrefill = true)
    }

    private fun showExitDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_quit_title)
            .setMessage(R.string.dialog_quit_message)
            .setPositiveButton(R.string.dialog_quit_positive) { _, _ ->
                navigateToGoal(withPrefill = true)
            }
            .setNegativeButton(R.string.dialog_quit_negative, null)
            .show()
    }

    private fun navigateToGoal(withPrefill: Boolean) {
        val intent = Intent(this, GoalSettingActivity::class.java).apply {
            if (withPrefill) {
                putExtra(IntentKeys.EXTRA_GOAL_CONTENT, goalContent)
            }
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
        finish()
    }

    companion object {
        private const val KEY_REMAINING_MILLIS = "key_remaining_millis"
        private const val TICK_INTERVAL = 1000L
    }
}