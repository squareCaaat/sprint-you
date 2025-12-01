package com.termproject.sprintyou.ui.setup

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.slider.Slider
import com.termproject.sprintyou.R
import com.termproject.sprintyou.databinding.ActivitySetupBinding
import com.termproject.sprintyou.ui.navigation.IntentKeys
import com.termproject.sprintyou.ui.timer.TimerActivity

class SetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetupBinding
    private var goalContent: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        goalContent = intent.getStringExtra(IntentKeys.EXTRA_GOAL_CONTENT).orEmpty()
        if (goalContent.isEmpty()) {
            finish()
            return
        }

        binding.tvGoalPreview.text = goalContent
        updateTimeDisplay(MINUTES_DEFAULT)

        binding.sliderTime.apply {
            value = MINUTES_DEFAULT.toFloat()
            addOnChangeListener(sliderListener)
        }

        binding.btnStart.setOnClickListener {
            val minutes = binding.sliderTime.value.toInt().coerceIn(MINUTES_MIN, MINUTES_MAX)
            val targetDurationSeconds = minutes * 60L
            val intent = Intent(this, TimerActivity::class.java).apply {
                putExtra(IntentKeys.EXTRA_GOAL_CONTENT, goalContent)
                putExtra(IntentKeys.EXTRA_TARGET_DURATION_SECONDS, targetDurationSeconds)
            }
            startActivity(intent)
            finish()
        }
    }

    private val sliderListener =
        Slider.OnChangeListener { _, value, _ -> updateTimeDisplay(value.toInt()) }

    private fun updateTimeDisplay(minutes: Int) {
        binding.tvTimeDisplay.text = getString(R.string.setup_time_format, minutes)
    }

    companion object {
        private const val MINUTES_MIN = 1
        private const val MINUTES_MAX = 10
        private const val MINUTES_DEFAULT = 5
    }
}