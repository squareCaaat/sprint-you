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
import com.termproject.sprintyou.auth.AuthManager
import com.termproject.sprintyou.auth.FirebaseScopeResolver
import com.termproject.sprintyou.data.MainGoal
import com.termproject.sprintyou.data.MainGoalStatus
import com.termproject.sprintyou.data.SprintDatabaseProvider
import com.termproject.sprintyou.databinding.ActivityGoalSettingBinding
import com.termproject.sprintyou.sync.FirebaseSyncManager
import com.termproject.sprintyou.ui.history.CalendarActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope

class GoalSettingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGoalSettingBinding
    private val database by lazy { SprintDatabaseProvider.getDatabase(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGoalSettingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setReadyEnabled(false)
        setupInputWatcher()
        bindClicks()
    }

    private fun setupInputWatcher() {
        binding.etGoal.doOnTextChanged { text, _, _, _ ->
            setReadyEnabled(!text.isNullOrBlank())
        }
    }

    private fun bindClicks() {
        binding.btnBack.setOnClickListener {
            finish()
        }
        binding.btnReady.setOnClickListener {
            createMainGoal()
        }

        binding.btnHistory.setOnClickListener {
            startActivity(Intent(this, CalendarActivity::class.java))
        }
    }

    private fun createMainGoal() {
        val goal = binding.etGoal.text?.toString()?.trim().orEmpty()
        if (goal.isEmpty()) {
            Toast.makeText(this, R.string.toast_goal_required, Toast.LENGTH_SHORT).show()
            return
        }
        val totalSprints = binding.etTotalSprints.text?.toString()?.toIntOrNull()?.takeIf { it > 0 }

        lifecycleScope.launch {
            val now = System.currentTimeMillis()
            val ownerId = FirebaseScopeResolver.ownerId(this@GoalSettingActivity)
            withContext(Dispatchers.IO) {
                val dao = database.mainGoalDao()
                dao.updateStatusFor(
                    currentStatus = MainGoalStatus.ACTIVE,
                    newStatus = MainGoalStatus.GAVE_UP,
                    lastModified = now
                )
                dao.insert(
                    MainGoal(
                        title = goal,
                        totalSprints = totalSprints,
                        ownerUid = ownerId,
                        lastModified = now,
                        isSynced = false
                    )
                )
            }
            if (AuthManager.isLoggedIn) {
                runCatching { FirebaseSyncManager.pushLocalData(applicationContext) }
            }
            Toast.makeText(this@GoalSettingActivity, R.string.toast_goal_created, Toast.LENGTH_SHORT)
                .show()
            finish()
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