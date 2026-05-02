package com.wpi.openspot.ui.profile

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

class EditProfileFragment : Fragment(R.layout.fragment_edit_profile) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etName = view.findViewById<TextInputEditText>(R.id.etName)
        val etWpiId = view.findViewById<TextInputEditText>(R.id.etWpiId)
        val etPermitType = view.findViewById<TextInputEditText>(R.id.etPermitType)
        val btnSave = view.findViewById<MaterialButton>(R.id.btnSave)
        val btnCancel = view.findViewById<MaterialButton>(R.id.btnCancel)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBar)

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            findNavController().popBackStack()
            return
        }

        // Load existing profile data
        progressBar.visibility = View.VISIBLE
        Firebase.firestore.collection("users").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                progressBar.visibility = View.GONE
                etName.setText(doc.getString("fullName") ?: "")
                etWpiId.setText(doc.getString("wpiIdNumber") ?: "")
                etPermitType.setText(doc.getString("permitType") ?: "")
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Could not load profile", Toast.LENGTH_SHORT).show()
            }

        btnSave.setOnClickListener {
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
            btnSave.isEnabled = false

            val updates = mapOf(
                "fullName" to name,
                "wpiIdNumber" to wpiId,
                "permitType" to permitType
            )

            Firebase.firestore.collection("users").document(uid)
                .update(updates)
                .addOnSuccessListener {
                    Log.d("OpenSpot", "Profile updated for $uid")
                    progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
                .addOnFailureListener { e ->
                    progressBar.visibility = View.GONE
                    btnSave.isEnabled = true
                    Log.e("OpenSpot", "Profile update failed: ${e.message}")
                    Toast.makeText(requireContext(), "Update failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }

        btnCancel.setOnClickListener {
            findNavController().popBackStack()
        }
    }
}
