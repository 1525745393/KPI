package com.example.data.model

data class UserSettings(
    val workStartTime: String = "09:00", // HH:mm
    val workEndTime: String = "18:00", // HH:mm
    val lateToleranceMinutes: Int = 15,
    val earlyToleranceMinutes: Int = 15,
    val companyLatitude: Double = 31.2304, // Default e.g. Shanghai / City Center
    val companyLongitude: Double = 121.4737,
    val companyAddressName: String = "公司总部 (科技园区A座)",
    val geofenceRadiusMeters: Float = 50f,
    val autoCheckInEnabled: Boolean = true
)
