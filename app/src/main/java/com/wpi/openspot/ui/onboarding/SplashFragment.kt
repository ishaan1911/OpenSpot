package com.wpi.openspot.ui.onboarding

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.wpi.openspot.R

class SplashFragment : Fragment(R.layout.fragment_splash) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null) {
            // Already signed in — go straight to map
            findNavController().navigate(R.id.toHome)
        } else {
            // Not signed in — go to sign in screen
            findNavController().navigate(R.id.toSignIn)
        }
    }
}
