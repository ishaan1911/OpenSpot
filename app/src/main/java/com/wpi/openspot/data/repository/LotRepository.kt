package com.wpi.openspot.data.repository

import com.wpi.openspot.domain.model.LotStatus
import com.wpi.openspot.domain.model.ParkingLot
import kotlinx.coroutines.flow.Flow

interface LotRepository {
    fun getLots(): Flow<List<ParkingLot>>
    suspend fun updateLotStatus(lotId: String, status: LotStatus)
}