package com.example.data.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.model.AttendanceEntity
import java.io.File
import java.io.FileWriter

object CsvExportUtil {

    fun exportAndShareCsv(context: Context, records: List<AttendanceEntity>, monthLabel: String) {
        val fileName = "attendance_${monthLabel.replace("-", "")}.csv"
        val exportDir = File(context.cacheDir, "export_csv")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }

        val file = File(exportDir, fileName)

        try {
            val writer = FileWriter(file)
            // Write UTF-8 BOM for Chinese Excel compatibility
            writer.write("\uFEFF")
            writer.append("日期,上班打卡,下班打卡,考勤状态,备注\n")

            for (record in records) {
                val statusCN = when (record.status) {
                    "normal" -> "正常"
                    "late" -> "迟到"
                    "early" -> "早退"
                    "absent" -> "缺勤/未打卡"
                    else -> "正常"
                }
                val checkIn = record.checkIn ?: "--:--:--"
                val checkOut = record.checkOut ?: "--:--:--"
                val note = record.note?.replace(",", " ") ?: ""

                writer.append("${record.date},$checkIn,$checkOut,$statusCN,$note\n")
            }

            writer.flush()
            writer.close()

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "考勤记录导出 ($monthLabel)")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "分享考勤CSV数据文件"))

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
