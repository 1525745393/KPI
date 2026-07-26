package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.data.local.AppDatabase

class AttendanceApplication : Application() {

    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_SERVICE_ID,
                "考勤定位服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "保持考勤自动打卡在后台运行"
            }

            val checkInChannel = NotificationChannel(
                CHANNEL_CHECKIN_ID,
                "考勤打卡提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "自动打卡成功通知"
                enableVibration(true)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(serviceChannel)
            notificationManager.createNotificationChannel(checkInChannel)
        }
    }

    companion object {
        const val CHANNEL_SERVICE_ID = "attendance_location_service"
        const val CHANNEL_CHECKIN_ID = "attendance_checkin_notice"
    }
}
