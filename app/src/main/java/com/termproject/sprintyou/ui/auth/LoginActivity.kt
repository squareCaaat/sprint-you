package com.termproject.sprintyou.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.termproject.sprintyou.R
import com.termproject.sprintyou.auth.AuthManager
import com.termproject.sprintyou.databinding.ActivityLoginBinding
import com.termproject.sprintyou.sync.FirebaseSyncManager
import com.termproject.sprintyou.sync.SnapshotManager
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bindClicks()
    }

    override fun onResume() {
        super.onResume()
        updateState()
    }

    private fun bindClicks() {
        binding.btnClose.setOnClickListener { finish() }
        binding.btnSkip.setOnClickListener { finish() }
        binding.btnLogin.setOnClickListener { attemptSignIn() }
        binding.btnSignup.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
        binding.btnSignOut.setOnClickListener { performSignOut() }
    }

    private fun attemptSignIn() {
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
            val result = runCatching { AuthManager.signIn(email, password) }
            setLoading(false)
            result
                .onSuccess {
                    Toast.makeText(this@LoginActivity, R.string.login_toast_sign_in, Toast.LENGTH_SHORT)
                        .show()
                    updateState()
                    showSyncDecisionDialog()
                }
                .onFailure {
                    binding.tilPassword.error = it.localizedMessage
                }
        }
    }

    private fun updateState() {
        val firebaseReady = AuthManager.isFirebaseReady
        val loggedIn = firebaseReady && AuthManager.isLoggedIn

        binding.layoutAuthForm.isVisible = firebaseReady && !loggedIn
        binding.layoutLoggedIn.isVisible = firebaseReady && loggedIn
        binding.tvLoginSubtitle.text = if (firebaseReady) {
            getString(R.string.login_subtitle)
        } else {
            getString(R.string.login_unavailable_message)
        }

        binding.tilPassword.error = null
        binding.btnSkip.isVisible = !loggedIn

        if (loggedIn) {
            val email = AuthManager.currentUserEmail ?: getString(R.string.login_unknown_user)
            binding.tvLoggedInStatus.text =
                getString(R.string.login_logged_in_status, email)
        }

        binding.btnLogin.isEnabled = firebaseReady
        binding.btnSignup.isEnabled = firebaseReady
        binding.btnSignOut.isEnabled = firebaseReady && loggedIn
        binding.progressLoading.isVisible = false
    }

    private fun setLoading(isLoading: Boolean) {
        binding.progressLoading.isVisible = isLoading
        binding.btnLogin.isEnabled = !isLoading
        binding.btnSignup.isEnabled = !isLoading
        binding.btnSignOut.isEnabled = !isLoading
    }

    private fun showSyncDecisionDialog() {
        if (!AuthManager.isFirebaseReady) {
            finish()
            return
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.sync_dialog_title)
            .setMessage(R.string.sync_dialog_message)
            .setPositiveButton(R.string.sync_action_restore) { _, _ ->
                lifecycleScope.launch {
                    setLoading(true)
                    runCatching { FirebaseSyncManager.pullRemoteData(applicationContext) }
                    setLoading(false)
                    Toast.makeText(this@LoginActivity, R.string.sync_toast_restore, Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .setNegativeButton(R.string.sync_action_backup) { _, _ ->
                lifecycleScope.launch {
                    setLoading(true)
                    runCatching { FirebaseSyncManager.pushLocalData(applicationContext) }
                    setLoading(false)
                    Toast.makeText(this@LoginActivity, R.string.sync_toast_backup, Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .setNeutralButton(R.string.sync_action_later) { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun performSignOut() {
        lifecycleScope.launch {
            setLoading(true)
            AuthManager.signOut()
            runCatching { SnapshotManager.restoreSnapshot(applicationContext) }
            SnapshotManager.clearSnapshot(applicationContext)
            setLoading(false)
            Toast.makeText(this@LoginActivity, R.string.login_toast_sign_out, Toast.LENGTH_SHORT).show()
            updateState()
        }
    }

    companion object {
        private const val PASSWORD_MIN_LENGTH = 6
    }
}

