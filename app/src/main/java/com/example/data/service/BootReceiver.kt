package com.example.data.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.AttendanceApplication
import com.example.data.repository.AttendanceRepository

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val app = context.applicationContext as AttendanceApplication
            val repository = AttendanceRepository(context, app.database.attendanceDao())

            if (repository.getSettings().autoCheckInEnabled) {
                val serviceIntent = Intent(context, LocationService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            }
        }
    }
}
