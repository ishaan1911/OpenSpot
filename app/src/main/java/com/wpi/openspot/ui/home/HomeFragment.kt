package com.wpi.openspot.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.wpi.openspot.R
import com.wpi.openspot.domain.model.LotStatus
import com.wpi.openspot.domain.model.ParkingLot
import kotlinx.coroutines.launch

class HomeFragment : Fragment(R.layout.fragment_home), OnMapReadyCallback {

    private var googleMap: GoogleMap? = null
    private val WPI_CENTER = LatLng(42.2746, -71.8063)
    private val viewModel: HomeViewModel by viewModels()

    // Track markers by lot ID so we update in place instead of clearing all
    private val markerMap = HashMap<String, Marker>()

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            enableMyLocation()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(WPI_CENTER, 16f))

        // Observe Firestore lots and update markers individually
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.lots.collect { lots ->
                updateMarkers(map, lots)
            }
        }

        // Request or enable location
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            enableMyLocation()
        } else {
            locationPermissionRequest.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun updateMarkers(map: GoogleMap, lots: List<ParkingLot>) {
        // Remove markers for lots that no longer exist
        val incomingIds = lots.map { it.id }.toSet()
        val removedIds = markerMap.keys - incomingIds
        removedIds.forEach { id ->
            markerMap[id]?.remove()
            markerMap.remove(id)
        }

        // Add or update markers for each lot
        lots.forEach { lot ->
            val color = when (lot.status) {
                LotStatus.AVAILABLE -> BitmapDescriptorFactory.HUE_GREEN
                LotStatus.FULL      -> BitmapDescriptorFactory.HUE_RED
                LotStatus.UNCERTAIN -> BitmapDescriptorFactory.HUE_YELLOW
                LotStatus.UNKNOWN   -> BitmapDescriptorFactory.HUE_AZURE
            }
            val existingMarker = markerMap[lot.id]
            if (existingMarker != null) {
                // Update existing marker color and snippet
                existingMarker.setIcon(BitmapDescriptorFactory.defaultMarker(color))
                existingMarker.snippet = lot.status.name
            } else {
                // Add new marker
                val marker = map.addMarker(
                    MarkerOptions()
                        .position(LatLng(lot.latitude, lot.longitude))
                        .title(lot.name)
                        .snippet(lot.status.name)
                        .icon(BitmapDescriptorFactory.defaultMarker(color))
                )
                if (marker != null) markerMap[lot.id] = marker
            }
        }
    }

    private fun enableMyLocation() {
        val map = googleMap ?: return
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            map.isMyLocationEnabled = true
        }
    }
}
