package com.example.data.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.IBinder
import android.os.Looper
import com.example.AttendanceApplication
import com.example.data.model.UserSettings
import com.example.data.repository.AttendanceRepository
import com.example.data.repository.CheckType
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class LocationService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var repository: AttendanceRepository

    private var enterGeofenceTime: Long = 0L
    private var hasTriggeredForCurrentVisit: Boolean = false

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            handleLocationUpdate(location)
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        notificationHelper = NotificationHelper(this)
        val app = applicationContext as AttendanceApplication
        repository = AttendanceRepository(applicationContext, app.database.attendanceDao())

        val notification = notificationHelper.createServiceNotification("考勤自动定位监控已开启")
        startForeground(NOTIFICATION_ID, notification)

        startLocationUpdates()
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 15000L)
            .setMinUpdateIntervalMillis(10000L)
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun handleLocationUpdate(location: Location) {
        val settings = repository.getSettings()
        if (!settings.autoCheckInEnabled) {
            return
        }

        val distanceResults = FloatArray(1)
        Location.distanceBetween(
            location.latitude,
            location.longitude,
            settings.companyLatitude,
            settings.companyLongitude,
            distanceResults
        )

        val distanceMeters = distanceResults[0]
        val isInsideGeofence = distanceMeters <= settings.geofenceRadiusMeters

        if (isInsideGeofence) {
            if (enterGeofenceTime == 0L) {
                enterGeofenceTime = System.currentTimeMillis()
            }

            val stayDurationSeconds = (System.currentTimeMillis() - enterGeofenceTime) / 1000
            
            // Check if user stayed > 60 seconds inside radius and hasn't triggered for this visit
            if (stayDurationSeconds >= 60 && !hasTriggeredForCurrentVisit) {
                hasTriggeredForCurrentVisit = true
                triggerAutoCheckIn(settings)
            }
        } else {
            // User exited geofence radius
            enterGeofenceTime = 0L
            hasTriggeredForCurrentVisit = false
        }
    }

    private fun triggerAutoCheckIn(settings: UserSettings) {
        serviceScope.launch {
            val result = repository.smartCheckInOrOut(note = "围栏自动打卡")
            if (result.success) {
                val title = if (result.type == CheckType.CHECK_IN) "自动打卡：上班" else "自动打卡：下班"
                notificationHelper.sendCheckInNotification(title, result.message)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        serviceScope.cancel()
    }

    companion object {
        const val NOTIFICATION_ID = 1001
    }
}
