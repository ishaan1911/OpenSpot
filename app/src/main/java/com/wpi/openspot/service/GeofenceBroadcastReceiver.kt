package com.wpi.openspot.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("OpenSpot", "GeofenceBroadcastReceiver fired")

        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent == null) {
            Log.e("OpenSpot", "GeofencingEvent is null")
            return
        }

        if (geofencingEvent.hasError()) {
            val msg = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
            Log.e("OpenSpot", "Geofence error: $msg")
            return
        }

        val transitionType = geofencingEvent.geofenceTransition
        val triggeringGeofences = geofencingEvent.triggeringGeofences

        if (triggeringGeofences.isNullOrEmpty()) {
            Log.e("OpenSpot", "No triggering geofences")
            return
        }

        Log.d("OpenSpot", "Transition: $transitionType lots: ${triggeringGeofences.map { it.requestId }}")

        triggeringGeofences.forEach { geofence ->
            val lotId = geofence.requestId
            when (transitionType) {
                Geofence.GEOFENCE_TRANSITION_ENTER -> {
                    Log.d("OpenSpot", "ENTER: $lotId — incrementing")
                    updateOccupancy(lotId, increment = true)
                }
                Geofence.GEOFENCE_TRANSITION_EXIT -> {
                    Log.d("OpenSpot", "EXIT: $lotId — decrementing")
                    updateOccupancy(lotId, increment = false)
                }
                else -> Log.d("OpenSpot", "Unknown transition: $transitionType")
            }
        }
    }

    private fun updateOccupancy(lotId: String, increment: Boolean) {
        val db = Firebase.firestore
        val lotRef = db.collection("lots").document(lotId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(lotRef)

            if (!snapshot.exists()) {
                Log.e("OpenSpot", "Lot $lotId does not exist in Firestore")
                return@runTransaction
            }

            val capacity = (snapshot.getLong("capacity") ?: 50L).toInt()
            val current = (snapshot.getLong("occupancy") ?: 0L).toInt()
            val newOccupancy = if (increment) current + 1 else maxOf(0, current - 1)

            val newStatus = when {
                newOccupancy >= capacity -> "FULL"
                newOccupancy >= (capacity * 0.85).toInt() -> "ALMOST_FULL"
                else -> "AVAILABLE"
            }

            transaction.update(lotRef, mapOf(
                "occupancy" to newOccupancy,
                "status" to newStatus,
                "lastUpdatedAt" to FieldValue.serverTimestamp()
            ))

            Log.d("OpenSpot", "$lotId: $current → $newOccupancy / $capacity ($newStatus)")
        }
        .addOnSuccessListener {
            Log.d("OpenSpot", "Transaction success: $lotId")
        }
        .addOnFailureListener { e ->
            Log.e("OpenSpot", "Transaction failed: $lotId — ${e.message}")
        }
    }
}
