package com.wpi.openspot.ui.home

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.wpi.openspot.R
import com.wpi.openspot.domain.model.LotStatus
import com.wpi.openspot.domain.model.ParkingLot
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LotBottomSheetFragment : BottomSheetDialogFragment() {

    companion object {
        private const val ARG_LOT_ID = "lot_id"

        fun newInstance(lot: ParkingLot): LotBottomSheetFragment {
            val fragment = LotBottomSheetFragment()
            val bundle = Bundle().apply {
                putString("name", lot.name)
                putDouble("latitude", lot.latitude)
                putDouble("longitude", lot.longitude)
                putString("status", lot.status.name)
                putInt("occupancy", lot.occupancy)
                putInt("capacity", lot.capacity)
                putString("permitTypes", lot.permitTypes.joinToString(", "))
                putLong("lastUpdatedAt", lot.lastUpdatedAt)
            }
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.bottom_sheet_lot, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val args = arguments ?: return

        val name = args.getString("name", "")
        val latitude = args.getDouble("latitude")
        val longitude = args.getDouble("longitude")
        val status = LotStatus.valueOf(args.getString("status", "UNKNOWN"))
        val occupancy = args.getInt("occupancy")
        val capacity = args.getInt("capacity")
        val permitTypes = args.getString("permitTypes", "")
        val lastUpdatedAt = args.getLong("lastUpdatedAt")

        val tvLotName = view.findViewById<TextView>(R.id.tvLotName)
        val tvStatusBadge = view.findViewById<TextView>(R.id.tvStatusBadge)
        val statusDot = view.findViewById<View>(R.id.statusDot)
        val tvOccupancy = view.findViewById<TextView>(R.id.tvOccupancy)
        val tvOccupancyCount = view.findViewById<TextView>(R.id.tvOccupancyCount)
        val occupancyProgress = view.findViewById<ProgressBar>(R.id.occupancyProgress)
        val tvPermitTypes = view.findViewById<TextView>(R.id.tvPermitTypes)
        val tvLastUpdated = view.findViewById<TextView>(R.id.tvLastUpdated)
        val btnDirections = view.findViewById<MaterialButton>(R.id.btnDirections)
        val btnGoogleMaps = view.findViewById<MaterialButton>(R.id.btnGoogleMaps)

        tvLotName.text = name

        val (statusColor, badgeColor, textColor) = when (status) {
            LotStatus.AVAILABLE  -> Triple("#4CAF50", "#E8F5E9", "#2E7D32")
            LotStatus.ALMOST_FULL -> Triple("#FF9800", "#FFF3E0", "#E65100")
            LotStatus.FULL       -> Triple("#F44336", "#FFEBEE", "#C62828")
            LotStatus.UNKNOWN    -> Triple("#9E9E9E", "#F5F5F5", "#424242")
        }

        statusDot.background.setTint(Color.parseColor(statusColor))
        tvStatusBadge.text = status.name.replace("_", " ")
        tvStatusBadge.setTextColor(Color.parseColor(textColor))
        tvStatusBadge.background.setTint(Color.parseColor(badgeColor))

        // Occupancy
        val percent = if (capacity > 0) (occupancy * 100) / capacity else 0
        tvOccupancy.text = "Occupancy"
        tvOccupancyCount.text = "$occupancy / $capacity spaces"
        occupancyProgress.progress = percent
        occupancyProgress.progressTintList = android.content.res.ColorStateList
            .valueOf(Color.parseColor(statusColor))

        tvPermitTypes.text = permitTypes.ifEmpty { "All permits" }

        // Last updated
        if (lastUpdatedAt > 0) {
            val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
            tvLastUpdated.text = "Updated ${sdf.format(Date(lastUpdatedAt))}"
        } else {
            tvLastUpdated.text = "Last updated unknown"
        }

        // In-app directions — zoom map to lot
        btnDirections.setOnClickListener {
            dismiss()
            // Parent fragment handles the camera move via callback
            (parentFragment as? HomeFragment)?.moveToLot(latitude, longitude)
        }

        // Open Google Maps
        btnGoogleMaps.setOnClickListener {
            val uri = Uri.parse("google.navigation:q=$latitude,$longitude&mode=d")
            val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.google.android.apps.maps")
            }
            if (mapIntent.resolveActivity(requireActivity().packageManager) != null) {
                startActivity(mapIntent)
            } else {
                // Fallback to browser Google Maps
                val browserUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$latitude,$longitude&travelmode=driving")
                startActivity(Intent(Intent.ACTION_VIEW, browserUri))
            }
        }
    }
}
