package com.wpi.openspot.domain.model

data class UserProfile(
    val uid: String = "",
    val fullName: String = "",
    val email: String = "",
    val permitType: String = "",
    val wpiIdNumber: String = "",
    val reportCount: Int = 0
)