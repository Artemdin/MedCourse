package com.example.medcourse.ui

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.medcourse.R
import com.example.medcourse.data.AppDatabase
import com.example.medcourse.data.Medicine
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val database by lazy { AppDatabase.getDatabase(this) }
    private lateinit var adapter: MedicineAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            androidx.core.app.ActivityCompat.requestPermissions(
                this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101
            )
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val recyclerView = findViewById<RecyclerView>(R.id.rvMedicineList)

        adapter = MedicineAdapter(emptyList(),
            onDeleteClick = { medicine ->
                lifecycleScope.launch(Dispatchers.IO) {
                    cancelMultipleAlarms(medicine)
                    database.medicineDao().delete(medicine)
                    NotificationStateStore.clear(this@MainActivity, medicine.id)
                    refreshData()
                }
            },
            onItemClick = { medicine ->
                showMedicineDialog(medicine)
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        val fabAdd = findViewById<FloatingActionButton>(R.id.fabAdd)
        fabAdd.setOnClickListener {
            showMedicineDialog(null)
        }

        refreshData()
        createNotificationChannel()
    }

    // реєструє канал сповіщень у системі Android
    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val name = "Medicine Reminder"
            val descriptionText = "Канал для нагадувань про прийом ліків"
            val importance = android.app.NotificationManager.IMPORTANCE_HIGH
            val channel =
                android.app.NotificationChannel("MED_NOTIF_CHANNEL", name, importance).apply {
                    description = descriptionText
                }
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    override fun onResume()
    {
        super.onResume()
        refreshData()
    }
    // функція для оновлення списку ліків на екрані
    private fun refreshData() {
        // -- Отримуємо посилання на TextView один раз перед запуском корутини
        val txtNext = findViewById<TextView>(R.id.txtNextMedicine)

        lifecycleScope.launch(Dispatchers.IO) {
            val allMeds = database.medicineDao().getAllMedicine() // з бази беремо ліки

            val sortedMeds = allMeds.sortedBy { getNextDoseTime(it) }

            //  Отримуємо поточний системний час
            // -- Використовуємо Locale.US, щоб формат годин завжди був передбачуваним (24г)
            val currentTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.US).format(java.util.Date())

            withContext(Dispatchers.Main) {
                // Оновлюємо основний список
                adapter.updateData(sortedMeds)

                // Пошук лік
                // -Ще не настав час (або зараз)
                // -Які ще не закінчились в вживанні (taken < total)
                // -- Додано ?: для пошуку першого не закінченого курсу, якщо на сьогодні все випито
                val nextMed = sortedMeds.firstOrNull {
                    getNextDoseTime(it) >= currentTime && it.takenDoses < it.totalDoses
                } ?: sortedMeds.firstOrNull { it.takenDoses < it.totalDoses } // ящо на сьогодні нема ліків, шукаємо перші не закінчені на ближчий до нас час

                // Виводимо текст у синю картку
                if (nextMed != null) {
                    txtNext.text = "${nextMed.name} о ${getNextDoseTime(nextMed)}"
                } else {
                    txtNext.text = "Немає запланованих"
                }
            }
        }
    }

    private fun showMedicineDialog(med: Medicine?) {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_add_medicine, null)
        dialog.setContentView(view)

        val editName = view.findViewById<EditText>(R.id.editName)
        val editDosage = view.findViewById<EditText>(R.id.editDosage)
        val editTime = view.findViewById<EditText>(R.id.editTime)
        val editDays = view.findViewById<EditText>(R.id.editDays)
        val cbAllDays = view.findViewById<CheckBox>(R.id.cbAllDays)
        val editTotalDoses = view.findViewById<EditText>(R.id.editTotalDoses)
        val editInterval = view.findViewById<EditText>(R.id.editInterval)
        val spinnerType = view.findViewById<android.widget.Spinner>(R.id.spinnerType)
        val btnSave = view.findViewById<android.widget.Button>(R.id.btnSave)

        btnSave.setOnClickListener {
            val name = editName.text.toString()
            val time = normalizeTimeInput(editTime.text.toString())
            val days = editDays.text.toString().ifBlank { "Щодня" }
            val total = editTotalDoses.text.toString().toIntOrNull() ?: 1
            val interval = editInterval.text.toString().toIntOrNull() ?: 0

            if (name.isNotBlank() && time.isNotBlank()) {
                lifecycleScope.launch(Dispatchers.IO) {
                    val medicineForAlarms = if (med != null) {
                        val updatedMed = med.copy(
                            name = name,
                            type = spinnerType.selectedItem.toString(),
                            dosage = editDosage.text.toString(),
                            time = time,
                            days = days,
                            totalDoses = total,
                            interval = interval,
                            isSkipped = false
                        )
                        cancelMultipleAlarms(med)
                        NotificationStateStore.setPending(this@MainActivity, med.id, false)
                        database.medicineDao().update(updatedMed)
                        updatedMed
                    } else {
                        val newMed = Medicine(
                            name = name,
                            type = spinnerType.selectedItem.toString(),
                            dosage = editDosage.text.toString(),
                            time = time,
                            days = days,
                            totalDoses = total,
                            takenDoses = 0,
                            interval = interval,
                            isSkipped = false
                        )
                        val insertedId = database.medicineDao().insert(newMed).toInt()
                        newMed.copy(id = insertedId)
                    }

                    scheduleMultipleAlarms(medicineForAlarms)

                    withContext(Dispatchers.Main) {
                        dialog.dismiss() // Закриваємо вікно
                    }

                    kotlinx.coroutines.delay(150)
                    // оновлюємо дані
                    withContext(Dispatchers.Main) {
                        refreshData()
                    }
                }
            } else {
                editTime.error = "Введіть час у форматі H:mm або HH:mm"
            }
        }

        // Налаштування вибору днів через AlertDialog (красиве вікно замість чекбоксів)
        val daysArray = arrayOf("Понеділок", "Вівторок", "Середа", "Четвер", "П'ятниця", "Субота", "Неділя")
        val selectedDays = BooleanArray(daysArray.size)
        val userSelectedDays = mutableListOf<String>()

        cbAllDays.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedDays.fill(true)
                userSelectedDays.clear()
                userSelectedDays.addAll(daysArray)
                editDays.setText("Щодня")
            } else if (editDays.text.toString() == "Щодня") {
                selectedDays.fill(false)
                userSelectedDays.clear()
                editDays.setText("")
            }
        }

        editDays.isFocusable = false
        editDays.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Виберіть дні")
                .setMultiChoiceItems(daysArray, selectedDays) { _, which, isChecked ->
                    if (isChecked) userSelectedDays.add(daysArray[which])
                    else userSelectedDays.remove(daysArray[which])
                }
                .setPositiveButton("OK") { _, _ ->
                    val allDaysSelected = selectedDays.all { it }
                    cbAllDays.isChecked = allDaysSelected
                    editDays.setText(
                        if (allDaysSelected || userSelectedDays.isEmpty()) "Щодня"
                        else userSelectedDays.joinToString(", ")
                    )
                }
                .setNegativeButton("Скасувати", null)
                .show()
        }

        // Час вводиться вручну (наприклад: 8:00, 9:33)

        val types = arrayOf("Пігулка", "Капсула", "Шприц (мл)", "Краплі", "Спрей")
        spinnerType.adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, types)

        // Якщо редагуємо, заповнюємо поля існуючими даними
        med?.let {
            editName.setText(it.name)
            editDosage.setText(it.dosage)
            editTime.setText(it.time)
            editDays.setText(it.days)
            editTotalDoses.setText(it.totalDoses.toString())
            editInterval.setText(it.interval.toString())

            selectedDays.fill(false)
            userSelectedDays.clear()
            if (it.days == "Щодня") {
                cbAllDays.isChecked = true
            } else {
                cbAllDays.isChecked = false
                val parsedDays = it.days.split(",").map { day -> day.trim() }
                for (i in daysArray.indices) {
                    val selected = parsedDays.contains(daysArray[i])
                    selectedDays[i] = selected
                    if (selected) userSelectedDays.add(daysArray[i])
                }
            }
        }

        dialog.show()
    }

    private fun scheduleMultipleAlarms(medicine: Medicine) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        val timeParts = medicine.time.split(":")
        if (timeParts.size != 2) return

        for (i in 0 until medicine.totalDoses) {
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
                set(Calendar.MINUTE, timeParts[1].toInt())
                set(Calendar.SECOND, 0)
                add(Calendar.HOUR_OF_DAY, i * medicine.interval) // Додаємо інтервал у годинах
                if (before(Calendar.getInstance())) add(Calendar.DATE, 1)
            }

            val intent = Intent(this, AlarmReceiver::class.java).apply {
                putExtra("MED_NAME", medicine.name)
                putExtra("MED_ID", medicine.id)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                this, medicine.id * 100 + i, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.setAndAllowWhileIdle(android.app.AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        }
    }

    private fun cancelMultipleAlarms(medicine: Medicine) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        for (i in 0 until medicine.totalDoses) {
            val intent = Intent(this, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                medicine.id * 100 + i,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }

    private fun normalizeTimeInput(raw: String): String {
        val trimmed = raw.trim()
        val match = Regex("^(\\d{1,2}):(\\d{2})$").matchEntire(trimmed) ?: return ""
        val hour = match.groupValues[1].toIntOrNull() ?: return ""
        val minute = match.groupValues[2].toIntOrNull() ?: return ""
        if (hour !in 0..23 || minute !in 0..59) return ""
        return String.format(Locale.US, "%02d:%02d", hour, minute)
    }

    private fun getNextDoseTime(medicine: Medicine): String {
        val timeParts = medicine.time.split(":")
        if (timeParts.size != 2) return medicine.time
        val hour = timeParts[0].toIntOrNull() ?: return medicine.time
        val minute = timeParts[1].toIntOrNull() ?: return medicine.time

        val shiftHours = if (medicine.interval > 0) medicine.takenDoses * medicine.interval else 0
        val totalMinutes = ((hour + shiftHours) * 60 + minute) % (24 * 60)
        val displayHour = totalMinutes / 60
        val displayMinute = totalMinutes % 60
        return String.format(Locale.US, "%02d:%02d", displayHour, displayMinute)
    }
}