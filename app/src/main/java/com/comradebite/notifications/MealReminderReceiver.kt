package com.comradebite.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.comradebite.MainActivity
import com.comradebite.R

class MealReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val message = intent.getStringExtra("message") ?: "Time for a meal, Comrade!"
        val alarmId = intent.getIntExtra("alarm_id", 0)
        
        // Show the notification
        val notificationIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, alarmId, notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, "meal_reminders")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("ComradeBite Reminder")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // CRITICAL FIX: Use fixed alarmId (100, 101, 102) to prevent spamming multiple notifications.
        // New notifications will overwrite the old ones for the same meal type.
        notificationManager.notify(alarmId, notification)

        // Reschedule for tomorrow/next meal cycle
        NotificationHelper.scheduleMealReminders(context)
    }
}
