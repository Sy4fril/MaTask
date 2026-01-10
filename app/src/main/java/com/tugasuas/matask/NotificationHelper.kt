package com.tugasuas.matask

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import java.util.Calendar

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "deadline_channel"
        private const val CHANNEL_NAME = "Pengingat Deadline"
        const val ACTION_DONE = "com.tugasuas.matask.ACTION_DONE"
        const val ACTION_RESCHEDULE = "com.tugasuas.matask.ACTION_RESCHEDULE"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi untuk batas waktu tugas"
                enableLights(true)
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun scheduleNotification(task: Task) {
        val deadlineDate = task.deadline?.toDate() ?: return
        val currentTime = System.currentTimeMillis()
        
        // Daftar interval pengingat (dalam milidetik sebelum deadline)
        val intervals = mapOf(
            "24h" to 24 * 60 * 60 * 1000L, // H-1 (24 jam sebelum)
            "12h" to 12 * 60 * 60 * 1000L, // 12 jam sebelum
            "1h"  to 1 * 60 * 60 * 1000L,  // 1 jam sebelum
            "30m" to 30 * 60 * 1000L       // 30 menit sebelum
        )

        intervals.forEach { (label, offset) ->
            val triggerTime = deadlineDate.time - offset
            if (triggerTime > currentTime) {
                val message = when(label) {
                    "24h" -> "H-1 Deadline: ${task.title}. Jangan lupa diselesaikan!"
                    "12h" -> "12 jam lagi deadline: ${task.title}"
                    "1h"  -> "Sangat mendesak! 1 jam lagi deadline: ${task.title}"
                    else -> "30 menit lagi deadline: ${task.title}"
                }
                scheduleAlarm(task, triggerTime, "${task.id}_$label".hashCode(), message)
            }
        }

        // Notifikasi Tepat saat Deadline
        if (deadlineDate.time > currentTime) {
            scheduleAlarm(task, deadlineDate.time, "${task.id}_now".hashCode(), "Waktu habis! Segera selesaikan: ${task.title}")
        }
    }

    private fun scheduleAlarm(task: Task, timeInMillis: Long, requestCode: Int, message: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.e("NotificationHelper", "Izin SCHEDULE_EXACT_ALARM tidak diberikan.")
                return
            }
        }

        val intent = Intent(context, DeadlineReceiver::class.java).apply {
            putExtra("TASK_TITLE", task.title)
            putExtra("MESSAGE", message)
            putExtra("TASK_ID", task.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
            }
            Log.d("NotificationHelper", "Alarm dijadwalkan ($requestCode): $message")
        } catch (e: SecurityException) {
            Log.e("NotificationHelper", "Gagal menjadwalkan alarm: ${e.message}")
        }
    }

    fun showNotification(title: String, message: String, taskId: String) {
        val detailIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("OPEN_TASK_ID", taskId)
        }
        val detailPendingIntent = PendingIntent.getActivity(
            context, taskId.hashCode(), detailIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val doneIntent = Intent(context, DeadlineReceiver::class.java).apply {
            action = ACTION_DONE
            putExtra("TASK_ID", taskId)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            context, taskId.hashCode() + 1, doneIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val rescheduleIntent = Intent(context, DeadlineReceiver::class.java).apply {
            action = ACTION_RESCHEDULE
            putExtra("TASK_ID", taskId)
        }
        val reschedulePendingIntent = PendingIntent.getBroadcast(
            context, taskId.hashCode() + 2, rescheduleIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(detailPendingIntent)
            .addAction(0, "Ditandai selesai", donePendingIntent)
            .addAction(0, "Jadwal ulang", reschedulePendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(taskId.hashCode(), notification)
    }
}
