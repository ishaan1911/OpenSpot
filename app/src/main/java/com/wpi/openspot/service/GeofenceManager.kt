package com.wpi.openspot.service

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.wpi.openspot.domain.model.ParkingLot

class GeofenceManager(private val context: Context) {

    companion object {
        const val GEOFENCE_RADIUS_METERS = 100f
        const val GEOFENCE_EXPIRATION = Geofence.NEVER_EXPIRE
        const val ACTION_GEOFENCE_EVENT = "com.wpi.openspot.GEOFENCE_EVENT"
    }

    private val geofencingClient: GeofencingClient =
        LocationServices.getGeofencingClient(context)

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java).apply {
            action = ACTION_GEOFENCE_EVENT
        }
        PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    fun registerGeofences(lots: List<ParkingLot>) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("OpenSpot", "Location permission not granted, skipping geofence registration")
            return
        }

        val geofences = lots.map { lot ->
            Geofence.Builder()
                .setRequestId(lot.id)
                .setCircularRegion(lot.latitude, lot.longitude, GEOFENCE_RADIUS_METERS)
                .setExpirationDuration(GEOFENCE_EXPIRATION)
                .setTransitionTypes(
                    Geofence.GEOFENCE_TRANSITION_ENTER or
                    Geofence.GEOFENCE_TRANSITION_EXIT
                )
                .build()
        }

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(geofences)
            .build()

        geofencingClient.addGeofences(request, geofencePendingIntent)
            .addOnSuccessListener {
                Log.d("OpenSpot", "Geofences registered: ${lots.size} lots")
            }
            .addOnFailureListener { e ->
                Log.e("OpenSpot", "Failed to register geofences: ${e.message}")
            }
    }

    fun removeGeofences() {
        geofencingClient.removeGeofences(geofencePendingIntent)
            .addOnSuccessListener {
                Log.d("OpenSpot", "Geofences removed")
            }
    }
}
