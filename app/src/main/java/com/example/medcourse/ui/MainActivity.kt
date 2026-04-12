package com.example.medcourse.ui

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
            addNewMedicineTest()
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
    private fun addNewMedicineTest() {
        val newMed = Medicine(
            name = "Ліки #${(1..100).random()}",
            type = "Пігулка",
            dosage = "1 шт",
            time = "12:00"
        )
        lifecycleScope.launch(Dispatchers.IO) {
            database.medicineDao().insert(newMed)
            refreshData()
        }
    }
}