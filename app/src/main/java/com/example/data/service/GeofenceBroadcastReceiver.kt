package com.example.data.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.AttendanceApplication
import com.example.data.repository.AttendanceRepository
import com.example.data.repository.CheckType
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return
        if (geofencingEvent.hasError()) {
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER || geofenceTransition == Geofence.GEOFENCE_TRANSITION_DWELL) {
            val app = context.applicationContext as AttendanceApplication
            val repository = AttendanceRepository(context, app.database.attendanceDao())
            val notificationHelper = NotificationHelper(context)

            if (repository.getSettings().autoCheckInEnabled) {
                CoroutineScope(Dispatchers.IO).launch {
                    val result = repository.smartCheckInOrOut(note = "地理围栏打卡")
                    if (result.success) {
                        val title = if (result.type == CheckType.CHECK_IN) "自动打卡：上班" else "自动打卡：下班"
                        notificationHelper.sendCheckInNotification(title, result.message)
                    }
                }
            }
        }
    }
}
