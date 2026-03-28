package com.wpi.openspot.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import com.wpi.openspot.R
import com.wpi.openspot.service.LocationService

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val switchGeofence = view.findViewById<SwitchCompat>(R.id.switchGeofence)
        val switchNotifications = view.findViewById<SwitchCompat>(R.id.switchNotifications)

        switchGeofence.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                requireContext().startForegroundService(
                    Intent(requireContext(), LocationService::class.java)
                )
            } else {
                requireContext().stopService(
                    Intent(requireContext(), LocationService::class.java)
                )
            }
        }

        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            // Notification preference saved for Week 5
        }
    }
}
