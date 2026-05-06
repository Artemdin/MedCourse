package com.example.medcourse.ui

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.medcourse.R
import com.example.medcourse.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val medName = intent.getStringExtra("MED_NAME") ?: "Ліки"
        val medId = intent.getIntExtra("MED_ID", -1)
        if (medId == -1) return

        // Якщо попереднє сповіщення по цих ліках проігноровано, наступне не показуємо.
        if (NotificationStateStore.isPending(context, medId)) return

        GlobalScope.launch(Dispatchers.IO) { // Запускаємо корутину фонового потоку для перевірки бази
            val db = AppDatabase.getDatabase(context)
            val medicine = db.medicineDao().getAllMedicine().find { it.id == medId } // шукаємо ліки в базі даних по ID

            // дізнаваємось який сьогодні день
            val calendar = Calendar.getInstance()
            val currentDay = calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale("uk"))?.lowercase() ?: ""

            // перевірка чи є сьогоднішній день в списках про прийняття ліків
            val isToday = medicine?.days?.lowercase()?.contains(currentDay) == true || medicine?.days == "Щодня"

            // перевірка: показуємо сповіщення, тільки якщо прийом ще не завершено
            // -- Додано перевірку дня (&& isToday), щоб не дзвонило в неділю, якщо треба в суботу
            if (medicine != null && medicine.takenDoses < medicine.totalDoses && isToday) {

                withContext(Dispatchers.Main) { // -- Перемикаємось на головний потік для роботи з повідомленнями
                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    val channelId = "MED_NOTIF_CHANNEL"

                    // кнопка прийняв
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

                    // кнопка "Пропустити"
                    val skipIntent = Intent(context, MedicineActionReceiver::class.java).apply {
                        action = "ACTION_SKIP"
                        putExtra("MED_NAME", medName)
                        putExtra("MED_ID", medId)
                    }

                    val skipPendingIntent = PendingIntent.getBroadcast(
                        context,
                        medId + 1000, // Унікальний ID, щоб не конфліктував з першою кнопкою
                        skipIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    // Збираємо сповіщення разом із кнопками
                    val notification = NotificationCompat.Builder(context, channelId)
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentTitle("Час прийому ліків!")
                        .setContentText("Не забудьте прийняти: $medName")
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .addAction(R.drawable.ic_launcher_foreground, "Прийняв", takenPendingIntent)
                        .addAction(R.drawable.ic_launcher_foreground, "Пропустити", skipPendingIntent) // ДОДАНО КНОПКУ
                        .build()

                    // перевірка дозволів
                    val hasPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        androidx.core.app.ActivityCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.POST_NOTIFICATIONS
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    } else true

                    // показуємо сповіщення
                    if (hasPermission) {
                        notificationManager.notify(medId, notification)
                        NotificationStateStore.setPending(context, medId, true)
                    }
                }
            }
        }
    }
}