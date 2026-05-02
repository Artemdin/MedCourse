package com.example.medcourse.ui

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.medcourse.R

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val medName = intent.getStringExtra("MED_NAME") ?: "Ліки"
        val medId = intent.getIntExtra("MED_ID", -1)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = "MED_NOTIF_CHANNEL"

        // створюємо намір (Intent), який викличе MedicineActionReceiver при натисканні на кнопку
        val takenIntent = Intent(context, MedicineActionReceiver::class.java).apply {
            action = "ACTION_TAKEN"
            putExtra("MED_NAME", medName)
            putExtra("MED_ID", medId)
        }

        val takenPendingIntent = PendingIntent.getBroadcast(
            context,
            medId,
            takenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 3. Збираємо сповіщення разом із кнопкою .addAction
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Час прийому ліків!")
            .setContentText("Не забудьте прийняти: $medName")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_launcher_foreground, "Прийняв", takenPendingIntent)
            .build()

        // перевірка дозволів (для нових Android)
        val hasPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            androidx.core.app.ActivityCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else true

        // показуємо сповіщення
        if (hasPermission) {
            notificationManager.notify(medId, notification)
        }
    }
}