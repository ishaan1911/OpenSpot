package com.wpi.openspot.ui.onboarding

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.wpi.openspot.R

class ConfirmFragment : Fragment(R.layout.fragment_confirm) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etName = view.findViewById<TextInputEditText>(R.id.etName)
        val etWpiId = view.findViewById<TextInputEditText>(R.id.etWpiId)
        val etPermitType = view.findViewById<TextInputEditText>(R.id.etPermitType)
        val btnConfirm = view.findViewById<MaterialButton>(R.id.btnConfirm)
        val btnRescan = view.findViewById<MaterialButton>(R.id.btnRescan)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)

        // Pre-fill from scan if available
        arguments?.let {
            etName.setText(it.getString("name", ""))
            etWpiId.setText(it.getString("wpiId", ""))
        }

        btnConfirm.setOnClickListener {
            val name = etName.text.toString().trim()
            val wpiId = etWpiId.text.toString().trim()
            val permitType = etPermitType.text.toString().trim()

            if (name.isEmpty() || wpiId.isEmpty() || permitType.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (wpiId.length != 9 || !wpiId.all { it.isDigit() }) {
                Toast.makeText(requireContext(), "WPI ID must be exactly 9 digits", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            btnConfirm.isEnabled = false

            // Force token refresh to ensure Firestore auth is fully ready
            // This fixes the PERMISSION_DENIED error that occurs right after account creation
            val auth = FirebaseAuth.getInstance()
            val user = auth.currentUser

            if (user == null) {
                progressBar.visibility = View.GONE
                btnConfirm.isEnabled = true
                Toast.makeText(requireContext(), "Session expired. Please sign in again.", Toast.LENGTH_LONG).show()
                findNavController().navigate(R.id.splashFragment)
                return@setOnClickListener
            }

            user.getIdToken(true)  // force refresh the auth token
                .addOnSuccessListener {
                    Log.d("OpenSpot", "Token refreshed — proceeding to save profile for UID: ${user.uid}")
                    saveUserProfile(user.uid, name, wpiId, permitType) {
                        progressBar.visibility = View.GONE
                        findNavController().navigate(R.id.toHome)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("OpenSpot", "Token refresh failed: ${e.message}")
                    progressBar.visibility = View.GONE
                    btnConfirm.isEnabled = true
                    Toast.makeText(requireContext(), "Authentication error. Please try again.", Toast.LENGTH_LONG).show()
                }
        }

        btnRescan.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun saveUserProfile(
        uid: String,
        name: String,
        wpiId: String,
        permitType: String,
        onComplete: () -> Unit
    ) {
        val db = Firebase.firestore
        val userProfile = hashMapOf(
            "uid" to uid,
            "fullName" to name,
            "wpiIdNumber" to wpiId,
            "permitType" to permitType,
            "reportCount" to 0
        )

        db.collection("users").document(uid)
            .set(userProfile)
            .addOnSuccessListener {
                Log.d("OpenSpot", "Profile saved successfully for $uid")
                onComplete()
            }
            .addOnFailureListener { e ->
                Log.e("OpenSpot", "Firestore write failed: ${e.message}")
                requireActivity().runOnUiThread {
                    // Show error but still navigate — profile can be updated later via Edit Profile
                    Toast.makeText(
                        requireContext(),
                        "Profile saved locally. Tap Edit Profile later to sync.",
                        Toast.LENGTH_LONG
                    ).show()
                    onComplete()  // Navigate to Home anyway
                }
            }
    }
}
