package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun BatteryOptimizationGuideDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("电池优化与后台运行设置") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "为确保自动打卡功能在后台（尤其手机熄屏时）稳定运行，请将本应用加入系统的电池优化白名单：",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))

                GuideCard(
                    brand = "华为 / 荣耀",
                    steps = "设置 -> 应用 -> 应用启动管理 -> 找到‘考勤打卡’ -> 关闭‘自动管理’，开启‘允许后台活动’与‘允许自启动’。"
                )
                Spacer(modifier = Modifier.height(8.dp))

                GuideCard(
                    brand = "小米 / MIUI / HyperOS",
                    steps = "设置 -> 应用设置 -> 应用管理 -> 找到‘考勤打卡’ -> 省电策略 -> 设置为‘无限制’；并开启自启动权限。"
                )
                Spacer(modifier = Modifier.height(8.dp))

                GuideCard(
                    brand = "OPPO / vivo",
                    steps = "设置 -> 电池 -> 后台高耗电 / 应用耗电管理 -> 找到‘考勤打卡’ -> 允许后台高耗电行为。"
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    openBatteryOptimizationSettings(context)
                    onDismiss()
                }
            ) {
                Text("前往设置")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("已知晓")
            }
        }
    )
}

@Composable
private fun GuideCard(brand: String, steps: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = brand,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = steps,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private fun openBatteryOptimizationSettings(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        } else {
            val intent = Intent(Settings.ACTION_SETTINGS)
            context.startActivity(intent)
        }
    } catch (e: Exception) {
        try {
            val intent = Intent(Settings.ACTION_SETTINGS)
            context.startActivity(intent)
        } catch (e2: Exception) {
            e2.printStackTrace()
        }
    }
}
