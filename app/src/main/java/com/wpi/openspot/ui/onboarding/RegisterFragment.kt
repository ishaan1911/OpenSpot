package com.wpi.openspot.ui.onboarding

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.wpi.openspot.R

class RegisterFragment : Fragment(R.layout.fragment_register) {

    private lateinit var auth: FirebaseAuth

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        val etEmail = view.findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = view.findViewById<TextInputEditText>(R.id.etPassword)
        val etConfirmPassword = view.findViewById<TextInputEditText>(R.id.etConfirmPassword)
        val btnRegister = view.findViewById<MaterialButton>(R.id.btnRegister)
        val tvSignIn = view.findViewById<TextView>(R.id.tvSignIn)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)

        btnRegister.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            // Basic field validation
            if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // WPI email validation — only @wpi.edu accepted
            if (!email.lowercase().endsWith("@wpi.edu")) {
                Toast.makeText(
                    requireContext(),
                    "Only WPI email addresses (@wpi.edu) are accepted",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(requireContext(), "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            btnRegister.isEnabled = false

            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    Log.d("OpenSpot", "Registration successful — UID: ${result.user?.uid}")
                    progressBar.visibility = View.GONE
                    // Navigate to ScanId — user is now fully authenticated
                    findNavController().navigate(R.id.toScanId)
                }
                .addOnFailureListener { e ->
                    progressBar.visibility = View.GONE
                    btnRegister.isEnabled = true
                    val msg = when {
                        e.message?.contains("email address is already") == true ->
                            "An account with this email already exists. Please sign in."
                        e.message?.contains("email address is badly") == true ->
                            "Please enter a valid email address."
                        else -> "Registration failed: ${e.message}"
                    }
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                }
        }

        tvSignIn.setOnClickListener {
            findNavController().popBackStack()
        }
    }
}
