package com.wpi.openspot.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "parking_lots")
data class ParkingLotEntity(
    @PrimaryKey val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val status: String,
    val permitTypes: String,
    val lastUpdatedAt: Long
)