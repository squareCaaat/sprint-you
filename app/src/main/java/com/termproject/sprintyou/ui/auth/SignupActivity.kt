package com.termproject.sprintyou.ui.auth

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.termproject.sprintyou.R
import com.termproject.sprintyou.auth.AuthManager
import com.termproject.sprintyou.databinding.ActivitySignupBinding
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
            val result = runCatching {
                AuthManager.signUp(email, password)
            }
            setLoading(false)
            result
                .onSuccess {
                    Toast.makeText(this@SignupActivity, R.string.signup_toast_success, Toast.LENGTH_SHORT)
                        .show()
                    finish()
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

    companion object {
        private const val PASSWORD_MIN_LENGTH = 6
    }
}

