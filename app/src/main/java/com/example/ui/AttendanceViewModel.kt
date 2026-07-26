package com.example.ui

import android.content.Context
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.AttendanceEntity
import com.example.data.model.UserSettings
import com.example.data.repository.AttendanceRepository
import com.example.data.repository.CheckResult
import com.example.data.util.CsvExportUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AttendanceViewModel(
    private val repository: AttendanceRepository
) : ViewModel() {

    private val _settings = MutableStateFlow(repository.getSettings())
    val settings: StateFlow<UserSettings> = _settings.asStateFlow()

    private val _todayRecord = MutableStateFlow<AttendanceEntity?>(null)
    val todayRecord: StateFlow<AttendanceEntity?> = _todayRecord.asStateFlow()

    val allRecords: StateFlow<List<AttendanceEntity>> = repository.allAttendance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedMonth = MutableStateFlow(getCurrentMonthString())
    val selectedMonth: StateFlow<String> = _selectedMonth.asStateFlow()

    val monthlyRecords: StateFlow<List<AttendanceEntity>> = combine(
        allRecords,
        _selectedMonth
    ) { records, month ->
        records.filter { it.date.startsWith(month) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentDistanceMeters = MutableStateFlow<Float?>(null)
    val currentDistanceMeters: StateFlow<Float?> = _currentDistanceMeters.asStateFlow()

    private val _lastCheckInResult = MutableStateFlow<CheckResult?>(null)
    val lastCheckInResult: StateFlow<CheckResult?> = _lastCheckInResult.asStateFlow()

    init {
        refreshTodayRecord()
    }

    fun refreshTodayRecord() {
        viewModelScope.launch {
            _todayRecord.value = repository.getTodayAttendance()
        }
    }

    fun setSelectedMonth(monthStr: String) {
        _selectedMonth.value = monthStr
    }

    fun updateLocation(latitude: Double, longitude: Double) {
        val currentSettings = _settings.value
        val results = FloatArray(1)
        Location.distanceBetween(
            latitude,
            longitude,
            currentSettings.companyLatitude,
            currentSettings.companyLongitude,
            results
        )
        _currentDistanceMeters.value = results[0]
    }

    fun performManualCheckIn(note: String? = null) {
        viewModelScope.launch {
            val result = repository.smartCheckInOrOut(note)
            _lastCheckInResult.value = result
            refreshTodayRecord()
        }
    }

    fun updateSettings(newSettings: UserSettings) {
        repository.saveSettings(newSettings)
        _settings.value = newSettings
    }

    fun clearCheckInResultMsg() {
        _lastCheckInResult.value = null
    }

    fun exportCurrentMonthCsv(context: Context) {
        viewModelScope.launch {
            val records = monthlyRecords.value
            CsvExportUtil.exportAndShareCsv(context, records, _selectedMonth.value)
        }
    }

    fun generateSampleDataIfEmpty() {
        viewModelScope.launch {
            val existing = allRecords.first()
            if (existing.isNotEmpty()) return@launch

            val cal = Calendar.getInstance()
            val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            
            // Generate last 20 workdays records
            for (i in 1..25) {
                cal.add(Calendar.DAY_OF_YEAR, -1)
                val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) continue

                val dateStr = sdfDate.format(cal.time)
                val status = when {
                    i % 7 == 0 -> "late"
                    i % 11 == 0 -> "early"
                    i == 19 -> "absent"
                    else -> "normal"
                }

                val checkInTime = when (status) {
                    "late" -> "09:22:15"
                    "absent" -> null
                    else -> "08:52:${10 + (i % 40)}"
                }

                val checkOutTime = when (status) {
                    "early" -> "17:15:00"
                    "absent" -> null
                    else -> "18:06:${15 + (i % 30)}"
                }

                val note = when (status) {
                    "late" -> "交通拥堵迟到"
                    "early" -> "家中有事早退"
                    "absent" -> "带薪病假"
                    else -> "围栏打卡"
                }

                repository.smartCheckInOrOut(note = null)
                // Direct insert mock data
                val mockEntity = AttendanceEntity(
                    date = dateStr,
                    checkIn = checkInTime,
                    checkOut = checkOutTime,
                    status = status,
                    note = note
                )
                com.example.AttendanceApplication().database.attendanceDao().insert(mockEntity)
            }
            refreshTodayRecord()
        }
    }

    companion object {
        fun getCurrentMonthString(): String {
            return SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        }
    }
}

class AttendanceViewModelFactory(
    private val repository: AttendanceRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AttendanceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AttendanceViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
