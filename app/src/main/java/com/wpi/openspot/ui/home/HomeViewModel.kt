package com.wpi.openspot.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.wpi.openspot.domain.model.LotStatus
import com.wpi.openspot.domain.model.ParkingLot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class HomeViewModel : ViewModel() {

    private val _lots = MutableStateFlow<List<ParkingLot>>(emptyList())
    val lots: StateFlow<List<ParkingLot>> = _lots

    private val db = Firebase.firestore

    init {
        listenToLots()
    }

    private fun listenToLots() {
        db.collection("lots")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("OpenSpot", "Firestore error: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot == null) return@addSnapshotListener

                Log.d("OpenSpot", "Snapshot received: ${snapshot.documents.size} documents")

                val lots = snapshot.documents.mapNotNull { doc ->
                    try {
                        ParkingLot(
                            id = doc.id,
                            name = doc.getString("name") ?: "",
                            latitude = doc.getDouble("latitude") ?: 0.0,
                            longitude = doc.getDouble("longitude") ?: 0.0,
                            status = LotStatus.valueOf(
                                (doc.getString("status") ?: "UNKNOWN").trim().uppercase()
                            ),
                            permitTypes = (doc.get("permitTypes") as? List<*>)
                                ?.filterIsInstance<String>() ?: emptyList(),
                            capacity = (doc.getLong("capacity") ?: 0L).toInt(),
                            occupancy = (doc.getLong("occupancy") ?: 0L).toInt()
                        )
                    } catch (e: Exception) {
                        Log.e("OpenSpot", "Failed to parse doc ${doc.id}: ${e.message}")
                        null
                    }
                }
                Log.d("OpenSpot", "Parsed ${lots.size} lots, emitting to StateFlow")
                _lots.value = lots
            }
    }
}
