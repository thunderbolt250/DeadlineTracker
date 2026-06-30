package com.example.deadlinetracker

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.text.SimpleDateFormat
import java.util.Locale

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when {
            intent.action == Intent.ACTION_BOOT_COMPLETED -> handleBoot(context)
            intent.action == NotificationScheduler.ACTION_TASK_REMINDER -> handleTaskReminder(context, intent)
            else -> handleDailyDigest(context)
        }
    }

    private fun handleBoot(context: Context) {
        NotificationHelper.createNotificationChannel(context)
        NotificationScheduler.scheduleDailyNotification(context)

        // Re-schedule per-task reminders for tasks not yet due
        val db = DatabaseHelper(context)
        val tasks = db.getAllTasks()
        val now = System.currentTimeMillis()
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())

        for (i in 0 until tasks.length()) {
            val t = tasks.getJSONObject(i)
            if (t.getBoolean("done")) continue
            val dueStr = if (t.isNull("due")) null else t.getString("due") ?: continue
            dueStr ?: continue
            try {
                val dueMillis = sdf.parse(dueStr)?.time ?: continue
                if (dueMillis > now) {
                    NotificationScheduler.scheduleTaskReminder(
                        context,
                        t.getInt("id"),
                        t.getString("name"),
                        t.getString("course"),
                        dueMillis
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun handleTaskReminder(context: Context, intent: Intent) {
        val taskName   = intent.getStringExtra("task_name")   ?: return
        val taskCourse = intent.getStringExtra("task_course") ?: return
        val taskId     = intent.getIntExtra("task_id", -1)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, taskId, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Due tomorrow: $taskName")
            .setContentText("$taskCourse · Due in ~24 hours")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        with(NotificationManagerCompat.from(context)) {
            notify(2000 + taskId, notification)
        }
    }

    private fun handleDailyDigest(context: Context) {
        val db = DatabaseHelper(context)
        val schedule = db.getTodaySchedule()
        if (schedule.isEmpty()) return

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Good morning! Deadlines today")
            .setContentText("You have tasks due soon — tap to see your schedule")
            .setStyle(NotificationCompat.BigTextStyle().bigText(schedule))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        with(NotificationManagerCompat.from(context)) {
            notify(1001, notification)
        }
    }
}
