package com.example.medcourse.ui

import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
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
                this,
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                101
            )
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val recyclerView = findViewById<RecyclerView>(R.id.rvMedicineList)

        adapter = MedicineAdapter(emptyList()) { medicine ->
            lifecycleScope.launch(Dispatchers.IO) {
                database.medicineDao().delete(medicine)
                refreshData()
            }
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        val fabAdd = findViewById<FloatingActionButton>(R.id.fabAdd)
        fabAdd.setOnClickListener {
            showAddMedicineDialog()
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
    // функція для оновлення списку ліків на екрані
    private fun refreshData() {
        lifecycleScope.launch(Dispatchers.IO) {
            val allMeds = database.medicineDao().getAllMedicine()

            withContext(Dispatchers.Main) {
                adapter.updateData(allMeds)

                val txtNext = findViewById<TextView>(R.id.txtNextMedicine)
                if (allMeds.isNotEmpty()) {
                    txtNext.text = "${allMeds.last().name} о ${allMeds.last().time}"
                } else {
                    txtNext.text = "Немає запланованих"
                }
            }
        }
    }


    private fun showAddMedicineDialog() {
        val dialog = com.google.android.material.bottomsheet.BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_add_medicine, null)
        dialog.setContentView(view)

        val btnSave = view.findViewById<android.widget.Button>(R.id.btnSave)
        val editName = view.findViewById<android.widget.EditText>(R.id.editName)
        val editDosage = view.findViewById<android.widget.EditText>(R.id.editDosage)
        val editTime = view.findViewById<android.widget.EditText>(R.id.editTime)
        val spinnerType = view.findViewById<android.widget.Spinner>(R.id.spinnerType)

        // Додаємо годинник для вибору часу
        editTime.isFocusable = false
        editTime.setOnClickListener {
            val cal = Calendar.getInstance()
            val timeSetListener = TimePickerDialog.OnTimeSetListener { _, hour, minute ->
                val selectedTime = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
                editTime.setText(selectedTime)
            }
            TimePickerDialog(
                this,
                timeSetListener,
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                true
            ).show()
        }

        val cbAllDays = view.findViewById<android.widget.CheckBox>(R.id.cbAllDays)
        val dayBoxes = listOf(
            view.findViewById<android.widget.CheckBox>(R.id.cbMon),
            view.findViewById<android.widget.CheckBox>(R.id.cbTue),
            view.findViewById<android.widget.CheckBox>(R.id.cbWed),
            view.findViewById<android.widget.CheckBox>(R.id.cbThu),
            view.findViewById<android.widget.CheckBox>(R.id.cbFri),
            view.findViewById<android.widget.CheckBox>(R.id.cbSat),
            view.findViewById<android.widget.CheckBox>(R.id.cbSun)
        )

        // Логіка щоб вибрати дні 1 кліком
        cbAllDays.setOnCheckedChangeListener { _, isChecked ->
            dayBoxes.forEach { it.isChecked = isChecked }
        }

        val types = arrayOf("Пігулка", "Капсула", "Шприц (мл)", "Краплі", "Спрей")
        spinnerType.adapter =
            android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, types)

        btnSave.setOnClickListener {
            val name = editName.text.toString()
            val dosage = editDosage.text.toString()
            val time = editTime.text.toString().trim()
            val type = spinnerType.selectedItem.toString()

            // збирання галочок
            val selectedDaysList = dayBoxes.filter { it.isChecked }.map { it.text.toString() }
            val daysResult =
                if (selectedDaysList.isEmpty()) "Не вказано" else selectedDaysList.joinToString(", ")

            if (name.isNotBlank() && time.isNotBlank()) {
                lifecycleScope.launch(Dispatchers.IO) {
                    // Відправляємо в базу вже з днями з чекбоксів
                    val newMed = Medicine(
                        name = name,
                        type = type,
                        dosage = dosage,
                        time = time,
                        days = daysResult
                    )
                    scheduleNotification(newMed)
                    database.medicineDao().insert(newMed)

                    withContext(Dispatchers.Main) {
                        refreshData()
                        dialog.dismiss()
                    }
                }
            }
        }
        dialog.show()
    }

    private fun scheduleNotification(medicine: Medicine) {
        try {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            val intent = android.content.Intent(this, AlarmReceiver::class.java)
            intent.putExtra("MED_NAME", medicine.name)
            intent.putExtra("MED_ID", medicine.id)

            // Додаємо прапорці для безпеки (обов'язково для нових Android)
            val pendingIntent = android.app.PendingIntent.getBroadcast(
                this,
                medicine.id,
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )

            val timeParts = medicine.time.split(":")
            if (timeParts.size != 2) return

            val calendar = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, timeParts[0].toInt())
                set(java.util.Calendar.MINUTE, timeParts[1].toInt())
                set(java.util.Calendar.SECOND, 0)

                // Якщо час уже минув сьогодні, ставимо на завтра
                if (before(java.util.Calendar.getInstance())) {
                    add(java.util.Calendar.DATE, 1)
                }
            }

            alarmManager.setAndAllowWhileIdle(
                android.app.AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )

        } catch (e: Exception) {
            android.util.Log.e("ALARM_ERROR", "Помилка будильника: ${e.message}")
        }
    }
}