package com.example.medcourse.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import com.example.medcourse.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MedicineActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val medicineName = intent.getStringExtra("MED_NAME") ?: "ліки"
        val medId = intent.getIntExtra("MED_ID", -1)

        if (medId == -1) return

        // Отримуємо доступ до бази даних
        val db = AppDatabase.getDatabase(context)

        when (action) {
            "ACTION_TAKEN" -> {
                // Запускаємо оновлення статистики в базі (фоновий потік)
                GlobalScope.launch(Dispatchers.IO) {
                    db.medicineDao().incrementTakenDoses(medId)
                    // Скидаємо статус пропуску, якщо ліки прийнято
                    db.medicineDao().updateSkippedStatus(medId, false)
                    
                    val medicine = db.medicineDao().getAllMedicine().find { it.id == medId }
                    if (medicine != null) {
                        cancelMultipleAlarms(context, medicine)
                        scheduleMultipleAlarms(context, medicine)
                    }
                }
                // Показуємо підтвердження користувачу
                Toast.makeText(context, "Прийом $medicineName зафіксовано!", Toast.LENGTH_SHORT).show()
            }

            "ACTION_SKIP" -> {
                // Позначаємо в базі, що прийом пропущено
                GlobalScope.launch(Dispatchers.IO) {
                    db.medicineDao().updateSkippedStatus(medId, true)
                    
                    val medicine = db.medicineDao().getAllMedicine().find { it.id == medId }
                    if (medicine != null) {
                        cancelMultipleAlarms(context, medicine)
                        scheduleMultipleAlarms(context, medicine)
                    }
                }
                // Показуємо підтвердження пропуску
                Toast.makeText(context, "Прийом $medicineName пропущено!", Toast.LENGTH_SHORT).show()
            }
        }

        NotificationStateStore.setPending(context, medId, false)

        // Закриваємо сповіщення в шторці
        NotificationManagerCompat.from(context).cancel(medId)

        Toast.makeText(context, "Сигнал отримано!", Toast.LENGTH_SHORT).show()
    }
}