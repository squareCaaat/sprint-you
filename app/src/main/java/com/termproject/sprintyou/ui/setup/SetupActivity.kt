package com.termproject.sprintyou.ui.setup

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import com.termproject.sprintyou.R
import com.termproject.sprintyou.databinding.ActivitySetupBinding
import com.termproject.sprintyou.ui.navigation.IntentKeys
import com.termproject.sprintyou.ui.timer.TimerActivity

class SetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetupBinding
    private var mainGoalId: Long = -1L
    private var mainGoalTitle: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mainGoalId = intent.getLongExtra(IntentKeys.EXTRA_MAIN_GOAL_ID, -1L)
        mainGoalTitle = intent.getStringExtra(IntentKeys.EXTRA_MAIN_GOAL_TITLE).orEmpty()

        if (mainGoalId <= 0 || mainGoalTitle.isEmpty()) {
            finish()
            return
        }

        val prefillTask = intent.getStringExtra(IntentKeys.EXTRA_SPRINT_TASK).orEmpty()
        binding.tvGoalPreview.text =
            getString(R.string.setup_active_goal_format, mainGoalTitle)

        if (prefillTask.isNotEmpty()) {
            binding.etSprintTask.setText(prefillTask)
            binding.etSprintTask.setSelection(prefillTask.length)
            setStartEnabled(true)
        } else {
            setStartEnabled(false)
        }

        updateTimeDisplay(MINUTES_DEFAULT)
        binding.sliderTime.value = MINUTES_DEFAULT.toFloat()
        binding.sliderTime.addOnChangeListener { _, value, _ ->
            updateTimeDisplay(value.toInt())
        }

        binding.etSprintTask.doOnTextChanged { text, _, _, _ ->
            setStartEnabled(!text.isNullOrBlank())
        }

        binding.btnStart.setOnClickListener {
            val sprintTask = binding.etSprintTask.text?.toString()?.trim().orEmpty()
            if (sprintTask.isEmpty()) {
                Toast.makeText(this, R.string.toast_sprint_task_required, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val minutes = binding.sliderTime.value.toInt().coerceIn(MINUTES_MIN, MINUTES_MAX)
            val targetDurationSeconds = minutes * 60L
            val intent = Intent(this, TimerActivity::class.java).apply {
                putExtra(IntentKeys.EXTRA_SPRINT_TASK, sprintTask)
                putExtra(IntentKeys.EXTRA_TARGET_DURATION_SECONDS, targetDurationSeconds)
                putExtra(IntentKeys.EXTRA_MAIN_GOAL_ID, mainGoalId)
                putExtra(IntentKeys.EXTRA_MAIN_GOAL_TITLE, mainGoalTitle)
            }
            startActivity(intent)
            finish()
        }
    }

    private fun updateTimeDisplay(minutes: Int) {
        binding.tvTimeDisplay.text = getString(R.string.setup_time_format, minutes)
    }

    private fun setStartEnabled(enabled: Boolean) {
        binding.btnStart.isEnabled = enabled
        binding.btnStart.alpha = if (enabled) 1f else 0.5f
    }

    companion object {
        private const val MINUTES_MIN = 1
        private const val MINUTES_MAX = 10
        private const val MINUTES_DEFAULT = 5
    }
}