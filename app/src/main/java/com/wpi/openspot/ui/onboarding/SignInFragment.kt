package com.wpi.openspot.ui.onboarding

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.wpi.openspot.R

class SignInFragment : Fragment(R.layout.fragment_sign_in) {

    private lateinit var auth: FirebaseAuth
    private val RC_GOOGLE_SIGN_IN = 1001

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        if (auth.currentUser != null) {
            findNavController().navigate(R.id.toHome)
            return
        }

        val etEmail = view.findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = view.findViewById<TextInputEditText>(R.id.etPassword)
        val btnSignIn = view.findViewById<MaterialButton>(R.id.btnSignIn)
        val btnGoogle = view.findViewById<MaterialButton>(R.id.btnGoogle)
        val tvRegister = view.findViewById<TextView>(R.id.tvRegister)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)

        btnSignIn.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            btnSignIn.isEnabled = false

            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    Log.d("OpenSpot", "Sign in successful")
                    progressBar.visibility = View.GONE
                    findNavController().navigate(R.id.toHome)
                }
                .addOnFailureListener { e ->
                    progressBar.visibility = View.GONE
                    btnSignIn.isEnabled = true
                    Toast.makeText(requireContext(), "Sign in failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }

        btnGoogle.setOnClickListener {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()
            val googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
            startActivityForResult(googleSignInClient.signInIntent, RC_GOOGLE_SIGN_IN)
        }

        // Navigate to dedicated register screen
        tvRegister.setOnClickListener {
            findNavController().navigate(R.id.toRegister)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_GOOGLE_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                auth.signInWithCredential(credential)
                    .addOnSuccessListener {
                        Log.d("OpenSpot", "Google sign in successful")
                        findNavController().navigate(R.id.toHome)
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Google sign in failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            } catch (e: ApiException) {
                Toast.makeText(requireContext(), "Google sign in failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
