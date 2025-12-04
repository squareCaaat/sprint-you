package com.termproject.sprintyou.ui.auth

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.termproject.sprintyou.R
import com.termproject.sprintyou.auth.AuthManager
import com.termproject.sprintyou.databinding.ActivitySignupBinding
import com.termproject.sprintyou.sync.FirebaseSyncManager
import com.termproject.sprintyou.sync.SnapshotManager
import kotlinx.coroutines.launch

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bindClicks()
    }

    private fun bindClicks() {
        binding.btnClose.setOnClickListener { finish() }
        binding.btnGoToLogin.setOnClickListener { finish() }
        binding.btnSignup.setOnClickListener { attemptSignUp() }
        binding.etPassword.doOnTextChanged { text, _, _, _ ->
            updatePasswordStrength(text?.toString().orEmpty())
        }
    }

    private fun attemptSignUp() {
        if (!AuthManager.isFirebaseReady) {
            Toast.makeText(this, R.string.login_unavailable_toast, Toast.LENGTH_SHORT).show()
            return
        }

        val email = binding.etEmail.text?.toString()?.trim().orEmpty()
        val password = binding.etPassword.text?.toString().orEmpty()

        var hasError = false
        if (email.isEmpty()) {
            binding.tilEmail.error = getString(R.string.error_email_required)
            hasError = true
        } else {
            binding.tilEmail.error = null
        }

        if (password.isEmpty()) {
            binding.tilPassword.error = getString(R.string.error_password_required)
            hasError = true
        } else if (password.length < PASSWORD_MIN_LENGTH) {
            binding.tilPassword.error = getString(R.string.error_password_short)
            hasError = true
        } else {
            binding.tilPassword.error = null
        }

        if (hasError) return

        setLoading(true)
        lifecycleScope.launch {
            runCatching { SnapshotManager.saveSnapshot(applicationContext) }
            val result = runCatching { AuthManager.signUp(email, password) }
            setLoading(false)
            result
                .onSuccess {
                    Toast.makeText(this@SignupActivity, R.string.signup_toast_success, Toast.LENGTH_SHORT)
                        .show()
                    showSyncDecisionDialog()
                }
                .onFailure {
                    binding.tilPassword.error = it.localizedMessage
                }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressLoading.isVisible = isLoading
        binding.btnSignup.isEnabled = !isLoading
        binding.btnGoToLogin.isEnabled = !isLoading
    }

    private fun showSyncDecisionDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.sync_dialog_title)
            .setMessage(R.string.sync_dialog_message)
            .setPositiveButton(R.string.sync_action_restore) { _, _ ->
                lifecycleScope.launch {
                    setLoading(true)
                    runCatching { FirebaseSyncManager.pullRemoteData(applicationContext) }
                    setLoading(false)
                    Toast.makeText(this@SignupActivity, R.string.sync_toast_restore, Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .setNegativeButton(R.string.sync_action_backup) { _, _ ->
                lifecycleScope.launch {
                    setLoading(true)
                    runCatching { FirebaseSyncManager.pushLocalData(applicationContext) }
                    setLoading(false)
                    Toast.makeText(this@SignupActivity, R.string.sync_toast_backup, Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .setNeutralButton(R.string.sync_action_later) { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun updatePasswordStrength(password: String) {
        when {
            password.length >= PASSWORD_MIN_LENGTH && SPECIAL_CHAR_REGEX.containsMatchIn(password) -> {
                binding.tvPasswordStrength.isVisible = true
                binding.tvPasswordStrength.text = getString(R.string.password_strength_strong)
                binding.tvPasswordStrength.setTextColor(
                    ContextCompat.getColor(this, R.color.primary_blue)
                )
            }
            password.length >= PASSWORD_MIN_LENGTH -> {
                binding.tvPasswordStrength.isVisible = true
                binding.tvPasswordStrength.text = getString(R.string.password_strength_weak)
                binding.tvPasswordStrength.setTextColor(
                    ContextCompat.getColor(this, R.color.urgent_red)
                )
            }
            else -> binding.tvPasswordStrength.isVisible = false
        }
    }

    companion object {
        private const val PASSWORD_MIN_LENGTH = 6
        private val SPECIAL_CHAR_REGEX = Regex("[^A-Za-z0-9]")
    }
}

