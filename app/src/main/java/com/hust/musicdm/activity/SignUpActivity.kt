package com.hust.musicdm.activity

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.hust.musicdm.databinding.ActivitySignupBinding
import com.hust.musicdm.database.UserManager

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private lateinit var userManager: UserManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userManager = UserManager(this)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnSignUp.setOnClickListener {
            validateAndSignUp()
        }

        binding.tvSignIn.setOnClickListener {
            finish()
        }
    }

    private fun validateAndSignUp() {
        val fullName = binding.etFullName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()

        // Reset errors
        binding.tilFullName.error = null
        binding.tilEmail.error = null
        binding.tilPassword.error = null
        binding.tilConfirmPassword.error = null

        // Validate full name
        if (fullName.isEmpty()) {
            binding.tilFullName.error = "Full name is required"
            return
        }

        if (fullName.length < 3) {
            binding.tilFullName.error = "Full name must be at least 3 characters"
            return
        }

        // Validate email
        if (email.isEmpty()) {
            binding.tilEmail.error = "Email is required"
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Invalid email format"
            return
        }

        // Validate password
        if (password.isEmpty()) {
            binding.tilPassword.error = "Password is required"
            return
        }

        if (password.length < 6) {
            binding.tilPassword.error = "Password must be at least 6 characters"
            return
        }

        // Validate confirm password
        if (confirmPassword.isEmpty()) {
            binding.tilConfirmPassword.error = "Please confirm your password"
            return
        }

        if (password != confirmPassword) {
            binding.tilConfirmPassword.error = "Passwords do not match"
            return
        }

        // Check terms and conditions
        if (!binding.cbTerms.isChecked) {
            Toast.makeText(this, "Please accept the Terms and Conditions", Toast.LENGTH_SHORT).show()
            return
        }

        // Show loading
        showLoading(true)

        // Simulate network delay
        binding.root.postDelayed({
            performSignUp(fullName, email, password)
        }, 1000)
    }

    private fun performSignUp(fullName: String, email: String, password: String) {
        val result = userManager.registerUser(fullName, email, password)

        showLoading(false)

        result.onSuccess { user ->
            userManager.saveUser(user)
            Toast.makeText(this, "Account created successfully! Welcome, ${user.fullName}", Toast.LENGTH_SHORT).show()
            finish()
        }.onFailure { exception ->
            Toast.makeText(this, exception.message ?: "Sign up failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnSignUp.isEnabled = !isLoading
        binding.etFullName.isEnabled = !isLoading
        binding.etEmail.isEnabled = !isLoading
        binding.etPassword.isEnabled = !isLoading
        binding.etConfirmPassword.isEnabled = !isLoading
        binding.cbTerms.isEnabled = !isLoading
    }
}