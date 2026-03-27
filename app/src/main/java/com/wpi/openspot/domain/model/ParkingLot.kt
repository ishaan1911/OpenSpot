package com.wpi.openspot.domain.model

data class ParkingLot(
    val id: String = "",
    val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val status: LotStatus = LotStatus.UNKNOWN,
    val permitTypes: List<String> = emptyList(),
    val lastUpdatedAt: Long = 0L,
    val capacity: Int = 0,
    val occupancy: Int = 0
)
