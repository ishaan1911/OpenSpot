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
        val btnSignOut = view.findViewById<MaterialButton>(R.id.btnSignOut)

        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser

        tvEmail.text = "Email: ${user?.email ?: "—"}"

        // Load profile from Firestore
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

        btnSignOut.setOnClickListener {
            auth.signOut()
            findNavController().navigate(R.id.splashFragment)
        }
    }
}
