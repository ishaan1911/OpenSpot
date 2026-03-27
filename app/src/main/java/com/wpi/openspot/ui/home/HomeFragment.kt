package com.wpi.openspot.ui.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
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
import com.wpi.openspot.service.GeofenceManager
import com.wpi.openspot.service.LocationService
import android.util.Log
import kotlinx.coroutines.launch

class HomeFragment : Fragment(R.layout.fragment_home), OnMapReadyCallback {

    private var googleMap: GoogleMap? = null
    private val WPI_CENTER = LatLng(42.2746, -71.8063)
    private val viewModel: HomeViewModel by viewModels()
    private val markerMap = HashMap<String, Marker>()
    private lateinit var geofenceManager: GeofenceManager
    private var geofencesRegistered = false

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            enableMyLocation()
            requestBackgroundLocationPermission()
        }
    }

    private val backgroundLocationRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            Log.d("OpenSpot", "Background location granted")
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        geofenceManager = GeofenceManager(requireContext())
        val mapFragment = childFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(WPI_CENTER, 16f))

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.lots.collect { lots ->
                updateMarkers(map, lots)
                if (!geofencesRegistered && lots.isNotEmpty()) {
                    geofenceManager.registerGeofences(lots)
                    geofencesRegistered = true
                }
            }
        }

        checkLocationPermissions()
    }

    private fun checkLocationPermissions() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            enableMyLocation()
            startLocationService()
            requestBackgroundLocationPermission()
        } else {
            locationPermissionRequest.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun requestBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            backgroundLocationRequest.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }

    private fun startLocationService() {
        val intent = Intent(requireContext(), LocationService::class.java)
        requireContext().startForegroundService(intent)
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

    private fun updateMarkers(map: GoogleMap, lots: List<ParkingLot>) {
        val incomingIds = lots.map { it.id }.toSet()
        val removedIds = markerMap.keys - incomingIds
        removedIds.forEach { id ->
            markerMap[id]?.remove()
            markerMap.remove(id)
        }

        lots.forEach { lot ->
            val color = when (lot.status) {
                LotStatus.AVAILABLE -> BitmapDescriptorFactory.HUE_GREEN
                LotStatus.FULL      -> BitmapDescriptorFactory.HUE_RED
                LotStatus.ALMOST_FULL -> BitmapDescriptorFactory.HUE_YELLOW
                LotStatus.UNKNOWN   -> BitmapDescriptorFactory.HUE_AZURE
            }
            val existingMarker = markerMap[lot.id]
            if (existingMarker != null) {
                existingMarker.setIcon(BitmapDescriptorFactory.defaultMarker(color))
                existingMarker.snippet = lot.status.name
            } else {
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

    override fun onDestroy() {
        super.onDestroy()
        geofenceManager.removeGeofences()
    }
}
