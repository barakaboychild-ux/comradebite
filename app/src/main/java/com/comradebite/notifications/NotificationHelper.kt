package com.comradebite.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.comradebite.viewmodel.MealViewModel
import java.util.*

object NotificationHelper {
    private const val CHANNEL_ID = "meal_reminders"
    private const val CHANNEL_NAME = "Meal Reminders"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val descriptionText = "Notifications for meal times"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun scheduleMealReminders(context: Context) {
        scheduleAlarm(context, 100, MealViewModel.BREAKFAST_TIME.hour, MealViewModel.BREAKFAST_TIME.minute, "Breakfast Time! Comrade, fuel up for the day.")
        scheduleAlarm(context, 101, MealViewModel.LUNCH_TIME.hour, MealViewModel.LUNCH_TIME.minute, "Lunch Time! Take a break and grab a bite.")
        scheduleAlarm(context, 102, MealViewModel.DINNER_TIME.hour, MealViewModel.DINNER_TIME.minute, "Dinner Time! The day is ending, enjoy your meal.")
    }

    private fun scheduleAlarm(context: Context, id: Int, hour: Int, minute: Int, message: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, MealReminderReceiver::class.java).apply {
            putExtra("message", message)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
            Log.d("NotificationHelper", "Scheduled alarm for $hour:$minute")
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Failed to schedule alarm", e)
        }
    }
}
