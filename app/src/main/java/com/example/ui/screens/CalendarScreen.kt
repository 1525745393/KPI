package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AttendanceEntity
import com.example.ui.AttendanceViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class CalendarDay(
    val dateString: String, // yyyy-MM-dd
    val dayOfMonth: Int,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val attendance: AttendanceEntity?
)

@Composable
fun CalendarScreen(
    viewModel: AttendanceViewModel,
    modifier: Modifier = Modifier
) {
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val monthlyRecords by viewModel.monthlyRecords.collectAsStateWithLifecycle()

    var selectedDayRecord by remember { mutableStateOf<CalendarDay?>(null) }

    val calendarDays = remember(selectedMonth, monthlyRecords) {
        buildCalendarDaysForMonth(selectedMonth, monthlyRecords)
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
            .verticalScroll(rememberScrollState())
    ) {
        // Month Selector Header
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
                    onClick = {
                        viewModel.setSelectedMonth(shiftMonth(selectedMonth, -1))
                    },
                    modifier = Modifier.testTag("prev_month_btn")
                ) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = "上一月")
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Today, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = monthDisplayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = {
                        viewModel.setSelectedMonth(shiftMonth(selectedMonth, 1))
                    },
                    modifier = Modifier.testTag("next_month_btn")
                ) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = "下一月")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Weekday Headers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            listOf("一", "二", "三", "四", "五", "六", "日").forEach { weekDay ->
                Text(
                    text = weekDay,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Month Grid View
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                val chunks = calendarDays.chunked(7)
                chunks.forEach { week ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        week.forEach { calDay ->
                            CalendarDayCell(
                                day = calDay,
                                isSelected = selectedDayRecord?.dateString == calDay.dateString,
                                onClick = { selectedDayRecord = calDay },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendItem(color = Color(0xFF059669), label = "正常打卡")
            LegendItem(color = Color(0xFFD97706), label = "迟到")
            LegendItem(color = Color(0xFF6D28D9), label = "早退")
            LegendItem(color = Color(0xFFDC2626), label = "缺勤/未打卡")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Selected Day Record Detail Card
        selectedDayRecord?.let { calDay ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${calDay.dateString} 打卡详情",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        calDay.attendance?.let {
                            StatusBadge(status = it.status)
                        } ?: run {
                            StatusBadge(status = "absent")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (calDay.attendance != null) {
                        Text("上班打卡时间: ${calDay.attendance.checkIn ?: "未记录"}")
                        Text("下班打卡时间: ${calDay.attendance.checkOut ?: "未记录"}")
                        Text("备注: ${calDay.attendance.note ?: "无"}")
                    } else {
                        Text("当天未打卡或处于休假日")
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: CalendarDay,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (statusColor, badgeBg) = when (day.attendance?.status) {
        "normal" -> Pair(Color(0xFF059669), Color(0xFFD1FAE5))
        "late" -> Pair(Color(0xFFD97706), Color(0xFFFEF3C7))
        "early" -> Pair(Color(0xFF6D28D9), Color(0xFFEDE9FE))
        "absent" -> Pair(Color(0xFFDC2626), Color(0xFFFEE2E2))
        else -> Pair(Color.Transparent, Color.Transparent)
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                else if (day.attendance != null) badgeBg
                else Color.Transparent
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = day.dayOfMonth.toString(),
                fontSize = 14.sp,
                fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
                color = if (day.isToday) MaterialTheme.colorScheme.primary
                else if (day.isCurrentMonth) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )

            if (day.attendance != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}

private fun buildCalendarDaysForMonth(
    monthStr: String,
    monthlyRecords: List<AttendanceEntity>
): List<CalendarDay> {
    val list = mutableListOf<CalendarDay>()
    try {
        val sdfMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = sdfDate.format(Date())

        val cal = Calendar.getInstance()
        cal.time = sdfMonth.parse(monthStr) ?: Date()
        cal.set(Calendar.DAY_OF_MONTH, 1)

        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1 = Sunday, 2 = Monday
        val offset = if (firstDayOfWeek == Calendar.SUNDAY) 6 else firstDayOfWeek - 2

        // Add padding from previous month
        cal.add(Calendar.DAY_OF_MONTH, -offset)
        val recordMap = monthlyRecords.associateBy { it.date }

        for (i in 0 until 42) { // 6 weeks
            val dateString = sdfDate.format(cal.time)
            val isCurrentMonth = dateString.startsWith(monthStr)
            val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
            val isToday = dateString == todayStr

            list.add(
                CalendarDay(
                    dateString = dateString,
                    dayOfMonth = dayOfMonth,
                    isCurrentMonth = isCurrentMonth,
                    isToday = isToday,
                    attendance = recordMap[dateString]
                )
            )
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return list
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
