package com.wpi.openspot.data.remote

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.wpi.openspot.domain.model.LotStatus
import com.wpi.openspot.domain.model.ParkingLot
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class FirestoreDataSource {

    private val db = Firebase.firestore

    fun getLots(): Flow<List<ParkingLot>> = callbackFlow {
        val listener = db.collection("lots")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val lots = snapshot.documents.mapNotNull { doc ->
                    ParkingLot(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        latitude = doc.getDouble("latitude") ?: 0.0,
                        longitude = doc.getDouble("longitude") ?: 0.0,
                        status = LotStatus.valueOf(
                            doc.getString("status") ?: "UNKNOWN"
                        ),
                        permitTypes = (doc.get("permitTypes") as? List<*>)
                            ?.filterIsInstance<String>() ?: emptyList(),
                        lastUpdatedAt = doc.getTimestamp("lastUpdatedAt")
                            ?.toDate()?.time ?: 0L
                    )
                }
                trySend(lots)
            }
        awaitClose { listener.remove() }
    }
}