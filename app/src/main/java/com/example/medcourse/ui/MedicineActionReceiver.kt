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

        if (action == "ACTION_TAKEN" && medId != -1) {
            // Отримуємо доступ до бази даних
            val db = AppDatabase.getDatabase(context)

            // Запускаємо оновлення статистики в базі (фоновий потік)
            GlobalScope.launch(Dispatchers.IO) {
                db.medicineDao().incrementTakenDoses(medId)
            }

            // Показуємо підтвердження користувачу
            Toast.makeText(context, "Прийом $medicineName зафіксовано!", Toast.LENGTH_SHORT).show()
        }

        // Закриваємо сповіщення в шторці (щоб воно не висіло після натискання)
        if (medId != -1) {
            NotificationManagerCompat.from(context).cancel(medId)
        }
    }
}