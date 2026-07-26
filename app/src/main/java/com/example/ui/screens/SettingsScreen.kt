package com.example.ui.screens

import android.app.TimePickerDialog
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.UserSettings
import com.example.data.service.LocationService
import com.example.ui.AttendanceViewModel
import com.example.ui.components.BatteryOptimizationGuideDialog
import com.example.ui.components.LocationPickerModal
import java.util.Calendar

@Composable
fun SettingsScreen(
    viewModel: AttendanceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    var showLocationPicker by remember { mutableStateOf(false) }
    var showBatteryGuide by remember { mutableStateOf(false) }

    var lateTolInput by remember(settings) { mutableStateOf(settings.lateToleranceMinutes.toString()) }
    var earlyTolInput by remember(settings) { mutableStateOf(settings.earlyToleranceMinutes.toString()) }
    var radiusInput by remember(settings) { mutableStateOf(settings.geofenceRadiusMeters.toInt().toString()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "系统与考勤规则设置",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Auto Check-In Toggle Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "自动打卡 (地理围栏)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "开启后进入公司50米范围内停留1分钟自动触发上下班打卡",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = settings.autoCheckInEnabled,
                    onCheckedChange = { isEnabled ->
                        val updated = settings.copy(autoCheckInEnabled = isEnabled)
                        viewModel.updateSettings(updated)

                        val serviceIntent = Intent(context, LocationService::class.java)
                        if (isEnabled) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                context.startForegroundService(serviceIntent)
                            } else {
                                context.startService(serviceIntent)
                            }
                        } else {
                            context.stopService(serviceIntent)
                        }
                    },
                    modifier = Modifier.testTag("auto_checkin_switch")
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Schedule & Tolerances Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "工作时间与判定规则",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = {
                            val parts = settings.workStartTime.split(":")
                            val hour = parts.getOrNull(0)?.toIntOrNull() ?: 9
                            val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
                            TimePickerDialog(context, { _, h, m ->
                                val formatted = String.format("%02d:%02d", h, m)
                                viewModel.updateSettings(settings.copy(workStartTime = formatted))
                            }, hour, minute, true).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("上班时间: ${settings.workStartTime}")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedButton(
                        onClick = {
                            val parts = settings.workEndTime.split(":")
                            val hour = parts.getOrNull(0)?.toIntOrNull() ?: 18
                            val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
                            TimePickerDialog(context, { _, h, m ->
                                val formatted = String.format("%02d:%02d", h, m)
                                viewModel.updateSettings(settings.copy(workEndTime = formatted))
                            }, hour, minute, true).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("下班时间: ${settings.workEndTime}")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = lateTolInput,
                        onValueChange = {
                            lateTolInput = it
                            it.toIntOrNull()?.let { mins ->
                                viewModel.updateSettings(settings.copy(lateToleranceMinutes = mins))
                            }
                        },
                        label = { Text("迟到宽容 (分钟)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedTextField(
                        value = earlyTolInput,
                        onValueChange = {
                            earlyTolInput = it
                            it.toIntOrNull()?.let { mins ->
                                viewModel.updateSettings(settings.copy(earlyToleranceMinutes = mins))
                            }
                        },
                        label = { Text("早退宽容 (分钟)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Location & Radius Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "公司地理围栏参数",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("当前位置: ${settings.companyAddressName}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "经纬度坐标: ${settings.companyLatitude}, ${settings.companyLongitude}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = radiusInput,
                        onValueChange = {
                            radiusInput = it
                            it.toFloatOrNull()?.let { radius ->
                                viewModel.updateSettings(settings.copy(geofenceRadiusMeters = radius))
                            }
                        },
                        label = { Text("围栏半径 (米)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { showLocationPicker = true },
                        modifier = Modifier
                            .weight(1.2f)
                            .align(Alignment.CenterVertically)
                    ) {
                        Icon(Icons.Filled.Map, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("设定位置点")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Device Battery Optimization & Test Tools Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "系统兼容性与辅助", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { showBatteryGuide = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.BatterySaver, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("国产手机 (华为/小米/OPPO/vivo) 保活指南")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { viewModel.generateSampleDataIfEmpty() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Build, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("填充近1个月测试考勤示例数据")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showLocationPicker) {
        LocationPickerModal(
            currentLat = settings.companyLatitude,
            currentLng = settings.companyLongitude,
            currentName = settings.companyAddressName,
            onConfirm = { name, lat, lng ->
                viewModel.updateSettings(
                    settings.copy(
                        companyAddressName = name,
                        companyLatitude = lat,
                        companyLongitude = lng
                    )
                )
            },
            onDismiss = { showLocationPicker = false }
        )
    }

    if (showBatteryGuide) {
        BatteryOptimizationGuideDialog(
            onDismiss = { showBatteryGuide = false }
        )
    }
}
