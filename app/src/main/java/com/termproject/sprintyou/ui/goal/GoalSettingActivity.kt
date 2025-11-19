package com.termproject.sprintyou.ui.goal

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import com.termproject.sprintyou.R
import com.termproject.sprintyou.databinding.ActivityGoalSettingBinding
import com.termproject.sprintyou.ui.history.HistoryActivity
import com.termproject.sprintyou.ui.navigation.IntentKeys
import com.termproject.sprintyou.ui.setup.SetupActivity

class GoalSettingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGoalSettingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGoalSettingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val handled = prefillGoal(intent.getStringExtra(IntentKeys.EXTRA_GOAL_CONTENT))
        if (!handled) {
            setReadyEnabled(false)
        }
        setupInputWatcher()
        bindClicks()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        prefillGoal(intent.getStringExtra(IntentKeys.EXTRA_GOAL_CONTENT))
    }

    private fun prefillGoal(goal: String?): Boolean {
        val value = goal?.trim().orEmpty()
        if (value.isNotEmpty()) {
            binding.etGoal.setText(value)
            binding.etGoal.setSelection(value.length)
            setReadyEnabled(true)
            return true
        }
        return false
    }

    private fun setupInputWatcher() {
        binding.etGoal.doOnTextChanged { text, _, _, _ ->
            setReadyEnabled(!text.isNullOrBlank())
        }
    }

    private fun bindClicks() {
        binding.btnReady.setOnClickListener {
            val goal = binding.etGoal.text?.toString()?.trim().orEmpty()
            if (goal.isEmpty()) {
                Toast.makeText(this, R.string.toast_goal_required, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intent = Intent(this, SetupActivity::class.java).apply {
                putExtra(IntentKeys.EXTRA_GOAL_CONTENT, goal)
            }
            startActivity(intent)
        }

        binding.btnHistory.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
    }

    private fun setReadyEnabled(enabled: Boolean) {
        binding.btnReady.isEnabled = enabled
        binding.btnReady.alpha = if (enabled) 1f else 0.5f
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            currentFocus?.let { view ->
                val rect = Rect()
                view.getGlobalVisibleRect(rect)
                if (!rect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                    hideKeyboard(view)
                    view.clearFocus()
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun hideKeyboard(view: View?) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        view?.windowToken?.let { token ->
            imm?.hideSoftInputFromWindow(token, 0)
        }
    }
}

