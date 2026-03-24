package com.wpi.openspot.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ParkingLotDao {

    @Query("SELECT * FROM parking_lots")
    fun getAllLots(): Flow<List<ParkingLotEntity>>

    @Query("SELECT * FROM parking_lots WHERE id = :lotId")
    suspend fun getLotById(lotId: String): ParkingLotEntity?

    @Upsert
    suspend fun upsertLots(lots: List<ParkingLotEntity>)

    @Query("DELETE FROM parking_lots")
    suspend fun clearAll()
}