package com.termproject.sprintyou.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.termproject.sprintyou.R
import com.termproject.sprintyou.auth.AuthManager
import com.termproject.sprintyou.databinding.ActivityLoginBinding
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
        binding.btnSignOut.setOnClickListener {
            AuthManager.signOut()
            Toast.makeText(this, R.string.login_toast_sign_out, Toast.LENGTH_SHORT).show()
            updateState()
        }
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
            val result = runCatching {
                AuthManager.signIn(email, password)
            }
            setLoading(false)
            result
                .onSuccess {
                    Toast.makeText(this@LoginActivity, R.string.login_toast_sign_in, Toast.LENGTH_SHORT)
                        .show()
                    updateState()
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

    companion object {
        private const val PASSWORD_MIN_LENGTH = 6
    }
}

