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
        const val GEOFENCE_RADIUS_METERS = 50f
        const val ACTION_GEOFENCE_EVENT = "com.wpi.openspot.GEOFENCE_EVENT"
    }

    private val geofencingClient: GeofencingClient =
        LocationServices.getGeofencingClient(context)

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java).apply {
            action = ACTION_GEOFENCE_EVENT
        }
        PendingIntent.getBroadcast(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    fun registerGeofences(lots: List<ParkingLot>) {
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("OpenSpot", "Location permission not granted — skipping geofence registration")
            return
        }

        // Remove any existing geofences first to avoid stale registrations
        geofencingClient.removeGeofences(geofencePendingIntent)
            .addOnCompleteListener {
                val geofences = lots.map { lot ->
                    Log.d("OpenSpot", "Registering geofence for lot ID: '${lot.id}' at ${lot.latitude}, ${lot.longitude}")
                    Geofence.Builder()
                        .setRequestId(lot.id)
                        .setCircularRegion(lot.latitude, lot.longitude, GEOFENCE_RADIUS_METERS)
                        .setExpirationDuration(Geofence.NEVER_EXPIRE)
                        .setTransitionTypes(
                            Geofence.GEOFENCE_TRANSITION_ENTER or
                            Geofence.GEOFENCE_TRANSITION_EXIT
                        )
                        .setNotificationResponsiveness(2000) // fire within 2 seconds
                        .build()
                }

                val request = GeofencingRequest.Builder()
                    .setInitialTrigger(0) // don't fire on registration — only on actual crossing
                    .addGeofences(geofences)
                    .build()

                geofencingClient.addGeofences(request, geofencePendingIntent)
                    .addOnSuccessListener {
                        Log.d("OpenSpot", "Geofences registered successfully: ${lots.map { it.id }}")
                    }
                    .addOnFailureListener { e ->
                        Log.e("OpenSpot", "Failed to register geofences: ${e.message}")
                    }
            }
    }

    fun removeGeofences() {
        geofencingClient.removeGeofences(geofencePendingIntent)
            .addOnSuccessListener { Log.d("OpenSpot", "Geofences removed") }
    }
}
