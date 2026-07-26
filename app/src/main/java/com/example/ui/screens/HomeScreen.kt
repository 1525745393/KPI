package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.repository.AttendanceRepository
import com.example.ui.AttendanceViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: AttendanceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val todayRecord by viewModel.todayRecord.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val distanceMeters by viewModel.currentDistanceMeters.collectAsStateWithLifecycle()
    val lastResult by viewModel.lastCheckInResult.collectAsStateWithLifecycle()

    var showNoteDialog by remember { mutableStateOf(false) }
    var noteText by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    // Location permission request launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocation = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocation = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineLocation || coarseLocation) {
            viewModel.refreshTodayRecord()
        }
    }

    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LaunchedEffect(lastResult) {
        lastResult?.let { result ->
            snackbarHostState.showSnackbar(result.message)
        }
    }

    val todayDateStr = remember { AttendanceRepository.getTodayDateString() }
    val formattedDisplayDate = remember {
        SimpleDateFormat("yyyy年MM月dd日 EEEE", Locale.CHINA).format(Date())
    }

    val isCheckInDone = !todayRecord?.checkIn.isNullOrEmpty()
    val isCheckOutDone = !todayRecord?.checkOut.isNullOrEmpty()

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Hero Banner Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_banner_1785049636217),
                    contentDescription = "Attendance Banner",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.3f),
                                    Color.Black.copy(alpha = 0.7f)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (settings.autoCheckInEnabled) "自动打卡服务已生效" else "自动打卡已暂停",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formattedDisplayDate,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "工作时间: ${settings.workStartTime} - ${settings.workEndTime}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Status & Geofence Distance Meter Card
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                GeofenceDistanceMeterCard(
                    distanceMeters = distanceMeters,
                    geofenceRadius = settings.geofenceRadiusMeters,
                    companyName = settings.companyAddressName
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Big Action Check-In Button
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val isInside = distanceMeters?.let { it <= settings.geofenceRadiusMeters } ?: true

                        val buttonText = when {
                            !isCheckInDone -> "上班打卡 (${settings.workStartTime})"
                            !isCheckOutDone -> "下班打卡 (${settings.workEndTime})"
                            else -> "今日打卡已完成"
                        }

                        val buttonColor = when {
                            !isCheckInDone -> MaterialTheme.colorScheme.primary
                            !isCheckOutDone -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.secondary
                        }

                        Button(
                            onClick = {
                                if (!isCheckInDone || !isCheckOutDone) {
                                    viewModel.performManualCheckIn()
                                }
                            },
                            enabled = !isCheckInDone || !isCheckOutDone,
                            colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
                            shape = CircleShape,
                            modifier = Modifier
                                .size(170.dp)
                                .testTag("check_in_button")
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = if (!isCheckInDone) Icons.Filled.Login else if (!isCheckOutDone) Icons.Filled.Logout else Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (!isCheckInDone) "上班打卡" else if (!isCheckOutDone) "下班打卡" else "已打卡",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isCheckInDone && isCheckOutDone) "全部完成" else "手动触发表",
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        TextButton(
                            onClick = { showNoteDialog = true }
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = if (todayRecord?.note.isNullOrEmpty()) "添加今日打卡备注" else "备注: ${todayRecord?.note}")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Today Timeline
                Text(
                    text = "今日考勤记录",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                TodayTimelineCard(
                    isCheckInDone = isCheckInDone,
                    checkInTime = todayRecord?.checkIn,
                    isCheckOutDone = isCheckOutDone,
                    checkOutTime = todayRecord?.checkOut,
                    status = todayRecord?.status ?: "normal"
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
        )
    }

    if (showNoteDialog) {
        AlertDialog(
            onDismissRequest = { showNoteDialog = false },
            title = { Text("添加今日打卡备注") },
            text = {
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("例如：外勤打卡、补卡等") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.performManualCheckIn(note = noteText)
                        showNoteDialog = false
                    }
                ) {
                    Text("确定保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNoteDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun GeofenceDistanceMeterCard(
    distanceMeters: Float?,
    geofenceRadius: Float,
    companyName: String
) {
    val isInside = distanceMeters?.let { it <= geofenceRadius } ?: false
    val statusColor = if (isInside) Color(0xFF10B981) else MaterialTheme.colorScheme.primary

    Card(
        colors = CardDefaults.cardColors(
            containerColor = statusColor.copy(alpha = 0.12f)
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isInside) Icons.Filled.NearMe else Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = statusColor
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = companyName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                val distanceDesc = if (distanceMeters == null) {
                    "正在定位当前与公司地理围栏的距离..."
                } else {
                    val distInt = distanceMeters.toInt()
                    if (isInside) "距离公司 ${distInt} 米 · 已进入 ${geofenceRadius.toInt()} 米打卡围栏"
                    else "距离公司 ${distInt} 米 · 未进入 ${geofenceRadius.toInt()} 米围栏"
                }
                Text(
                    text = distanceDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isInside) Color(0xFF047857) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TodayTimelineCard(
    isCheckInDone: Boolean,
    checkInTime: String?,
    isCheckOutDone: Boolean,
    checkOutTime: String?,
    status: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Check In Row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            if (isCheckInDone) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Login,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (isCheckInDone) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("上班打卡", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = if (isCheckInDone) "打卡时间: $checkInTime" else "暂未打卡",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isCheckInDone) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }
                if (isCheckInDone) {
                    StatusBadge(status = status)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Check Out Row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            if (isCheckOutDone) MaterialTheme.colorScheme.tertiaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (isCheckOutDone) MaterialTheme.colorScheme.tertiary else Color.Gray
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("下班打卡", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = if (isCheckOutDone) "打卡时间: $checkOutTime" else "暂未打卡",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isCheckOutDone) MaterialTheme.colorScheme.tertiary else Color.Gray
                    )
                }
                if (isCheckOutDone) {
                    StatusBadge(status = status)
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (bgColor, textColor, text) = when (status) {
        "late" -> Triple(Color(0xFFFEF3C7), Color(0xFFD97706), "迟到")
        "early" -> Triple(Color(0xFFEDE9FE), Color(0xFF6D28D9), "早退")
        "absent" -> Triple(Color(0xFFFEE2E2), Color(0xFFDC2626), "缺勤")
        else -> Triple(Color(0xFFD1FAE5), Color(0xFF059669), "正常")
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}
