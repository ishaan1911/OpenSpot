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
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return

        if (geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes.getStatusCodeString(geofencingEvent.errorCode)
            Log.e("OpenSpot", "Geofence error: $errorMessage")
            return
        }

        val transitionType = geofencingEvent.geofenceTransition
        val triggeringGeofences = geofencingEvent.triggeringGeofences ?: return

        // No speed filter — trigger on any entry or exit
        triggeringGeofences.forEach { geofence ->
            val lotId = geofence.requestId
            when (transitionType) {
                Geofence.GEOFENCE_TRANSITION_ENTER -> {
                    Log.d("OpenSpot", "Entered lot: $lotId")
                    handleEntry(lotId)
                }
                Geofence.GEOFENCE_TRANSITION_EXIT -> {
                    Log.d("OpenSpot", "Exited lot: $lotId")
                    handleExit(lotId)
                }
            }
        }
    }

    private fun handleEntry(lotId: String) {
        val db = Firebase.firestore
        val lotRef = db.collection("lots").document(lotId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(lotRef)
            val capacity = (snapshot.getLong("capacity") ?: 50L).toInt()
            val currentOccupancy = (snapshot.getLong("occupancy") ?: 0L).toInt()
            val newOccupancy = currentOccupancy + 1

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

            Log.d("OpenSpot", "$lotId entry: $currentOccupancy → $newOccupancy / $capacity → $newStatus")
        }.addOnSuccessListener {
            Log.d("OpenSpot", "Entry transaction successful for $lotId")
        }.addOnFailureListener { e ->
            Log.e("OpenSpot", "Entry transaction failed for $lotId: ${e.message}")
        }
    }

    private fun handleExit(lotId: String) {
        val db = Firebase.firestore
        val lotRef = db.collection("lots").document(lotId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(lotRef)
            val capacity = (snapshot.getLong("capacity") ?: 50L).toInt()
            val currentOccupancy = (snapshot.getLong("occupancy") ?: 0L).toInt()
            val newOccupancy = maxOf(0, currentOccupancy - 1)

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

            Log.d("OpenSpot", "$lotId exit: $currentOccupancy → $newOccupancy / $capacity → $newStatus")
        }.addOnSuccessListener {
            Log.d("OpenSpot", "Exit transaction successful for $lotId")
        }.addOnFailureListener { e ->
            Log.e("OpenSpot", "Exit transaction failed for $lotId: ${e.message}")
        }
    }
}
