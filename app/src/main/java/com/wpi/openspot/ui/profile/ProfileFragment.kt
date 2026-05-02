package com.wpi.openspot.ui.profile

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.wpi.openspot.R

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvName = view.findViewById<TextView>(R.id.tvName)
        val tvEmail = view.findViewById<TextView>(R.id.tvEmail)
        val tvWpiId = view.findViewById<TextView>(R.id.tvWpiId)
        val tvPermitType = view.findViewById<TextView>(R.id.tvPermitType)
        val tvReportCount = view.findViewById<TextView>(R.id.tvReportCount)
        val btnEditProfile = view.findViewById<MaterialButton>(R.id.btnEditProfile)
        val btnSignOut = view.findViewById<MaterialButton>(R.id.btnSignOut)

        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser

        tvEmail.text = "Email: ${user?.email ?: "—"}"

        user?.uid?.let { uid ->
            Firebase.firestore.collection("users").document(uid)
                .get()
                .addOnSuccessListener { doc ->
                    tvName.text = "Name: ${doc.getString("fullName") ?: "—"}"
                    tvWpiId.text = "WPI ID: ${doc.getString("wpiIdNumber") ?: "—"}"
                    tvPermitType.text = "Permit Type: ${doc.getString("permitType") ?: "—"}"
                    tvReportCount.text = "Reports: ${doc.getLong("reportCount") ?: 0}"
                }
        }

        btnEditProfile.setOnClickListener {
            findNavController().navigate(R.id.toEditProfile)
        }

        btnSignOut.setOnClickListener {
            auth.signOut()
            findNavController().navigate(R.id.splashFragment)
        }
    }

    override fun onResume() {
        super.onResume()
        // Reload profile data when returning from EditProfile
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val tvName = view?.findViewById<TextView>(R.id.tvName) ?: return
        val tvWpiId = view?.findViewById<TextView>(R.id.tvWpiId) ?: return
        val tvPermitType = view?.findViewById<TextView>(R.id.tvPermitType) ?: return
        val tvReportCount = view?.findViewById<TextView>(R.id.tvReportCount) ?: return

        Firebase.firestore.collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { doc ->
                tvName.text = "Name: ${doc.getString("fullName") ?: "—"}"
                tvWpiId.text = "WPI ID: ${doc.getString("wpiIdNumber") ?: "—"}"
                tvPermitType.text = "Permit Type: ${doc.getString("permitType") ?: "—"}"
                tvReportCount.text = "Reports: ${doc.getLong("reportCount") ?: 0}"
            }
    }
}
