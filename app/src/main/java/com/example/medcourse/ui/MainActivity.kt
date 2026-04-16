package com.example.medcourse.ui

import android.app.TimePickerDialog
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
    }

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
            TimePickerDialog(this, timeSetListener, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
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
        spinnerType.adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, types)

        btnSave.setOnClickListener {
            val name = editName.text.toString()
            val dosage = editDosage.text.toString()
            val time = editTime.text.toString().trim()
            val type = spinnerType.selectedItem.toString()

            // збирання галочок
            val selectedDaysList = dayBoxes.filter { it.isChecked }.map { it.text.toString() }
            val daysResult = if (selectedDaysList.isEmpty()) "Не вказано" else selectedDaysList.joinToString(", ")

            if (name.isNotBlank() && time.isNotBlank()) {
                lifecycleScope.launch(Dispatchers.IO) {
                    // Відправляємо в базу вже з днями з чекбоксів
                    val newMed = Medicine(name = name, type = type, dosage = dosage, time = time, days = daysResult)
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
}