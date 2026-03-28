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

        // Pre-fill with scanned data if available
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

            progressBar.visibility = View.VISIBLE
            btnConfirm.isEnabled = false

            saveUserProfile(name, wpiId, permitType) {
                progressBar.visibility = View.GONE
                findNavController().navigate(R.id.toHome)
            }
        }

        // Go back to scan screen
        btnRescan.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun saveUserProfile(
        name: String,
        wpiId: String,
        permitType: String,
        onComplete: () -> Unit
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            Toast.makeText(requireContext(), "User not found", Toast.LENGTH_SHORT).show()
            return
        }

        val db = Firebase.firestore
        val userProfile = mapOf(
            "uid" to uid,
            "fullName" to name,
            "wpiIdNumber" to wpiId,
            "permitType" to permitType,
            "reportCount" to 0
        )

        db.collection("users").document(uid)
            .set(userProfile)
            .addOnSuccessListener {
                Log.d("OpenSpot", "User profile saved for $uid")
                onComplete()
            }
            .addOnFailureListener { e ->
                Log.e("OpenSpot", "Failed to save profile: ${e.message}")
                Toast.makeText(requireContext(), "Failed to save profile: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}
