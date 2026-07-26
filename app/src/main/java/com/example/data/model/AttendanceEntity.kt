package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "attendance",
    indices = [Index(value = ["date"], unique = true)]
)
data class AttendanceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val date: String, // yyyy-MM-dd
    val checkIn: String? = null, // HH:mm:ss
    val checkOut: String? = null, // HH:mm:ss
    val status: String = "normal", // 'normal' | 'late' | 'early' | 'absent'
    val note: String? = null
)
