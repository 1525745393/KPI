package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AttendanceEntity
import com.example.ui.AttendanceViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun StatsHistoryScreen(
    viewModel: AttendanceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val monthlyRecords by viewModel.monthlyRecords.collectAsStateWithLifecycle()

    // Calculate metrics for current month
    val totalAttendanceDays = remember(monthlyRecords) {
        monthlyRecords.count { !it.checkIn.isNullOrEmpty() }
    }
    val lateCount = remember(monthlyRecords) {
        monthlyRecords.count { it.status == "late" }
    }
    val earlyCount = remember(monthlyRecords) {
        monthlyRecords.count { it.status == "early" }
    }
    val absentCount = remember(monthlyRecords) {
        monthlyRecords.count { it.status == "absent" }
    }

    val monthDisplayName = remember(selectedMonth) {
        try {
            val date = SimpleDateFormat("yyyy-MM", Locale.getDefault()).parse(selectedMonth)
            SimpleDateFormat("yyyy年 MM月", Locale.CHINA).format(date ?: Date())
        } catch (e: Exception) {
            selectedMonth
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Month Switcher Header
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.setSelectedMonth(shiftMonth(selectedMonth, -1)) }
                ) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = "上一月")
                }

                Text(
                    text = "$monthDisplayName 统计报表",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = { viewModel.setSelectedMonth(shiftMonth(selectedMonth, 1)) }
                ) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = "下一月")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Metrics Summary Cards Grid
        Row(modifier = Modifier.fillMaxWidth()) {
            MetricCard(
                title = "出勤天数",
                count = "$totalAttendanceDays 天",
                color = Color(0xFF059669),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            MetricCard(
                title = "迟到次数",
                count = "$lateCount 次",
                color = Color(0xFFD97706),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            MetricCard(
                title = "早退次数",
                count = "$earlyCount 次",
                color = Color(0xFF6D28D9),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            MetricCard(
                title = "缺勤/请假",
                count = "$absentCount 天",
                color = Color(0xFFDC2626),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Export CSV Action Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "考勤打卡明细列表",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Button(
                onClick = { viewModel.exportCurrentMonthCsv(context) },
                modifier = Modifier.testTag("export_csv_btn")
            ) {
                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("导出CSV分享")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // History Records List (Sorted by date descending)
        if (monthlyRecords.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("本月暂无考勤记录数据", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(monthlyRecords.sortedByDescending { it.date }) { record ->
                    HistoryRecordCard(record = record)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    count: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, style = MaterialTheme.typography.labelMedium, color = color)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = count, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun HistoryRecordCard(record: AttendanceEntity) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = record.date,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "上班: ${record.checkIn ?: "--:--"}   下班: ${record.checkOut ?: "--:--"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!record.note.isNullOrEmpty()) {
                    Text(
                        text = "备注: ${record.note}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            StatusBadge(status = record.status)
        }
    }
}

private fun shiftMonth(currentMonthStr: String, amount: Int): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val cal = Calendar.getInstance()
        cal.time = sdf.parse(currentMonthStr) ?: Date()
        cal.add(Calendar.MONTH, amount)
        sdf.format(cal.time)
    } catch (e: Exception) {
        currentMonthStr
    }
}
