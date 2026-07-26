package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.AttendanceDao
import com.example.data.model.AttendanceEntity
import com.example.data.model.UserSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class CheckType {
    CHECK_IN,
    CHECK_OUT,
    ALREADY_COMPLETED
}

data class CheckResult(
    val type: CheckType,
    val success: Boolean,
    val timeString: String,
    val dateString: String,
    val status: String, // normal, late, early
    val message: String
)

class AttendanceRepository(
    private val context: Context,
    private val dao: AttendanceDao
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("attendance_prefs", Context.MODE_PRIVATE)

    fun getSettings(): UserSettings {
        return UserSettings(
            workStartTime = prefs.getString("work_start_time", "09:00") ?: "09:00",
            workEndTime = prefs.getString("work_end_time", "18:00") ?: "18:00",
            lateToleranceMinutes = prefs.getInt("late_tolerance", 15),
            earlyToleranceMinutes = prefs.getInt("early_tolerance", 15),
            companyLatitude = prefs.getFloat("company_lat", 31.2304f).toDouble(),
            companyLongitude = prefs.getFloat("company_lng", 121.4737f).toDouble(),
            companyAddressName = prefs.getString("company_name", "公司总部 (科技园区A座)") ?: "公司总部 (科技园区A座)",
            geofenceRadiusMeters = prefs.getFloat("geofence_radius", 50f),
            autoCheckInEnabled = prefs.getBoolean("auto_checkin_enabled", true)
        )
    }

    fun saveSettings(settings: UserSettings) {
        prefs.edit()
            .putString("work_start_time", settings.workStartTime)
            .putString("work_end_time", settings.workEndTime)
            .putInt("late_tolerance", settings.lateToleranceMinutes)
            .putInt("early_tolerance", settings.earlyToleranceMinutes)
            .putFloat("company_lat", settings.companyLatitude.toFloat())
            .putFloat("company_lng", settings.companyLongitude.toFloat())
            .putString("company_name", settings.companyAddressName)
            .putFloat("geofence_radius", settings.geofenceRadiusMeters)
            .putBoolean("auto_checkin_enabled", settings.autoCheckInEnabled)
            .apply()
    }

    val allAttendance: Flow<List<AttendanceEntity>> = dao.getAllAttendance()

    fun getAttendanceByMonth(monthPrefix: String): Flow<List<AttendanceEntity>> {
        return dao.getAttendanceByMonth(monthPrefix)
    }

    suspend fun getTodayAttendance(): AttendanceEntity? = withContext(Dispatchers.IO) {
        val today = getTodayDateString()
        dao.getAttendanceByDate(today)
    }

    suspend fun smartCheckInOrOut(note: String? = null): CheckResult = withContext(Dispatchers.IO) {
        val todayStr = getTodayDateString()
        val nowTimeStr = getCurrentTimeString()
        val settings = getSettings()

        val existing = dao.getAttendanceByDate(todayStr)

        if (existing == null || existing.checkIn.isNullOrEmpty()) {
            // Perform Check-In
            val isLate = calculateIsLate(nowTimeStr, settings.workStartTime, settings.lateToleranceMinutes)
            val statusStr = if (isLate) "late" else "normal"

            val entity = existing?.copy(
                checkIn = nowTimeStr,
                status = statusStr,
                note = note ?: existing.note
            ) ?: AttendanceEntity(
                date = todayStr,
                checkIn = nowTimeStr,
                status = statusStr,
                note = note
            )

            dao.insert(entity)
            CheckResult(
                type = CheckType.CHECK_IN,
                success = true,
                timeString = nowTimeStr,
                dateString = todayStr,
                status = statusStr,
                message = if (isLate) "上班打卡成功 $nowTimeStr (迟到)" else "上班打卡成功 $nowTimeStr"
            )
        } else if (existing.checkOut.isNullOrEmpty()) {
            // Perform Check-Out
            val isEarly = calculateIsEarly(nowTimeStr, settings.workEndTime, settings.earlyToleranceMinutes)
            val finalStatus = when {
                existing.status == "late" -> "late"
                isEarly -> "early"
                else -> "normal"
            }

            val entity = existing.copy(
                checkOut = nowTimeStr,
                status = finalStatus,
                note = note ?: existing.note
            )

            dao.update(entity)
            CheckResult(
                type = CheckType.CHECK_OUT,
                success = true,
                timeString = nowTimeStr,
                dateString = todayStr,
                status = finalStatus,
                message = if (isEarly) "下班打卡成功 $nowTimeStr (早退)" else "下班打卡成功 $nowTimeStr"
            )
        } else {
            CheckResult(
                type = CheckType.ALREADY_COMPLETED,
                success = false,
                timeString = nowTimeStr,
                dateString = todayStr,
                status = existing.status,
                message = "今日上下班打卡已全部完成"
            )
        }
    }

    suspend fun updateNote(date: String, note: String) = withContext(Dispatchers.IO) {
        val record = dao.getAttendanceByDate(date)
        if (record != null) {
            dao.update(record.copy(note = note))
        }
    }

    private fun calculateIsLate(checkInTime: String, startTime: String, toleranceMins: Int): Boolean {
        return try {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            val checkInDate = sdf.parse(checkInTime.take(5))
            val startDate = sdf.parse(startTime)
            if (checkInDate != null && startDate != null) {
                val diffMins = (checkInDate.time - startDate.time) / (1000 * 60)
                diffMins > toleranceMins
            } else false
        } catch (e: Exception) {
            false
        }
    }

    private fun calculateIsEarly(checkOutTime: String, endTime: String, toleranceMins: Int): Boolean {
        return try {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            val checkOutDate = sdf.parse(checkOutTime.take(5))
            val endDate = sdf.parse(endTime)
            if (checkOutDate != null && endDate != null) {
                val diffMins = (endDate.time - checkOutDate.time) / (1000 * 60)
                diffMins > toleranceMins
            } else false
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        fun getTodayDateString(): String {
            return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        }

        fun getCurrentTimeString(): String {
            return SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        }
    }
}
