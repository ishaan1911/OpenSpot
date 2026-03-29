package com.wpi.openspot.ui.onboarding

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.wpi.openspot.R

class SplashFragment : Fragment(R.layout.fragment_splash) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvLogo = view.findViewById<TextView>(R.id.tvLogo)
        val tvAppName = view.findViewById<TextView>(R.id.tvAppName)
        val tvTagline = view.findViewById<TextView>(R.id.tvTagline)

        // Animate logo in
        tvLogo.animate()
            .alpha(1f)
            .translationYBy(-20f)
            .setDuration(500)
            .setStartDelay(100)
            .start()

        // Animate app name in
        tvAppName.animate()
            .alpha(1f)
            .translationYBy(-20f)
            .setDuration(500)
            .setStartDelay(300)
            .start()

        // Animate tagline in
        tvTagline.animate()
            .alpha(1f)
            .translationYBy(-20f)
            .setDuration(500)
            .setStartDelay(500)
            .withEndAction {
                // After animation completes check auth and navigate
                view.postDelayed({
                    val auth = FirebaseAuth.getInstance()
                    if (auth.currentUser != null) {
                        findNavController().navigate(R.id.toHome)
                    } else {
                        findNavController().navigate(R.id.toSignIn)
                    }
                }, 600)
            }
            .start()
    }
}
