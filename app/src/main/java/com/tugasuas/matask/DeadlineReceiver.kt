package com.tugasuas.matask

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class DeadlineReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra("TASK_ID") ?: return
        val action = intent.action

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        when (action) {
            NotificationHelper.ACTION_DONE -> {
                markTaskAsDone(context, taskId)
                notificationManager.cancel(taskId.hashCode())
            }
            NotificationHelper.ACTION_RESCHEDULE -> {
                openRescheduleActivity(context, taskId)
                notificationManager.cancel(taskId.hashCode())
            }
            else -> {
                val title = intent.getStringExtra("TASK_TITLE") ?: "MaTask Reminder"
                val message = intent.getStringExtra("MESSAGE") ?: "Tugas Anda hampir mencapai batas waktu!"
                val notificationHelper = NotificationHelper(context)
                notificationHelper.showNotification(title, message, taskId)
            }
        }
    }

    private fun markTaskAsDone(context: Context, taskId: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(userId).collection("tasks").document(taskId)
                .update("completed", true)
                .addOnSuccessListener {
                    Log.d("DeadlineReceiver", "Task marked as done from notification")
                }
                .addOnFailureListener { e ->
                    Log.e("DeadlineReceiver", "Failed to mark task as done", e)
                }
        }
    }

    private fun openRescheduleActivity(context: Context, taskId: String) {
        // Opening the app to the detail page
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // In a real app, you might pass extras to jump directly to the detail
            putExtra("OPEN_TASK_ID", taskId) 
        }
        context.startActivity(intent)
    }
}
