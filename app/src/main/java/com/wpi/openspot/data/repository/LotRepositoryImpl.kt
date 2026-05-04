package com.wpi.openspot.data.repository

import com.wpi.openspot.data.local.AppDatabase
import com.wpi.openspot.data.local.ParkingLotEntity
import com.wpi.openspot.data.remote.FirestoreDataSource
import com.wpi.openspot.domain.model.LotStatus
import com.wpi.openspot.domain.model.ParkingLot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach

class LotRepositoryImpl(
    private val db: AppDatabase,
    private val remote: FirestoreDataSource
) : LotRepository {

    override fun getLots(): Flow<List<ParkingLot>> =
        remote.getLots().onEach { lots ->
            db.parkingLotDao().upsertLots(lots.map { it.toEntity() })
        }

    override suspend fun updateLotStatus(lotId: String, status: LotStatus) {
        // Will implement with Firestore write in Week 2
    }

    private fun ParkingLot.toEntity() = ParkingLotEntity(
        id = id,
        name = name,
        latitude = latitude,
        longitude = longitude,
        status = status.name,
        permitTypes = permitTypes.joinToString(","),
        lastUpdatedAt = lastUpdatedAt
    )
}