package com.example.medcourse.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.medcourse.R
import com.example.medcourse.data.Medicine
import java.util.Locale

class MedicineAdapter(
    private var meds: List<Medicine>,
    private val onDeleteClick: (Medicine) -> Unit,
    private val onItemClick: (Medicine) -> Unit // Клік по всій картці для редагування
) : RecyclerView.Adapter<MedicineAdapter.MedViewHolder>() {

    class MedViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.txtName)
        val txtTime: TextView = view.findViewById(R.id.txtTime)
        val details: TextView = view.findViewById(R.id.txtDetails)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDelete)
        val txtProgress: TextView = view.findViewById(R.id.txtProgress)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_medicine, parent, false)
        return MedViewHolder(view)
    }

    override fun onBindViewHolder(holder: MedViewHolder, position: Int) {
        val med = meds[position]

        holder.name.text = med.name
        holder.txtTime.text = getNextDoseTimeText(med)

        // перевіряємо статус пропуску прямо при відмальовці списку
        if (med.isSkipped)
        {
            holder.txtProgress.text = "ПРОПУЩЕНИЙ ПРИЙОМ ЛІКІВ!"
            holder.txtProgress.setTextColor(android.graphics.Color.RED)
            holder.name.setTextColor(android.graphics.Color.RED) // красить в червоний
        }
        else if (med.takenDoses >= med.totalDoses)
        {
            holder.txtProgress.text = "КУРС ЗАВЕРШЕНО! (${med.takenDoses}/${med.totalDoses})"
            holder.txtProgress.setTextColor(android.graphics.Color.parseColor("#388E3C")) // Темно-зелений
            holder.name.setTextColor(android.graphics.Color.parseColor("#388E3C"))
        }
        else
        {
            // вивід статистики
            holder.txtProgress.text = "Прийнято: ${med.takenDoses} з ${med.totalDoses}"
            holder.txtProgress.setTextColor(android.graphics.Color.GRAY)
            holder.name.setTextColor(android.graphics.Color.BLACK)
        }


        // Оголошуємо змінну типу базового суперкласу
        val dosageObj: com.example.medcourse.data.MedicineDosage

        //  Залежно від типу ліків (з БД), створюємо об'єкт конкретного класу-нащадка
        if (med.type == "Пігулка" || med.type == "Капсула") {
            dosageObj = com.example.medcourse.data.PillDosage(med.name, med.dosage)
        } else if (med.type == "Шприц (мл)" || med.type == "Краплі" || med.type == "Спрей") {
            dosageObj = com.example.medcourse.data.LiquidDosage(med.name, med.dosage)
        } else {
            //  Резервний варіант (базовий клас), якщо тип невідомий
            dosageObj = com.example.medcourse.data.MedicineDosage(med.name, med.dosage)
        }

        // Вивід деталей / тип + доза
        holder.details.text = dosageObj.getDosageInstruction()

        holder.btnDelete.setOnClickListener { onDeleteClick(med) }
        holder.itemView.setOnClickListener { onItemClick(med) }
    }

    override fun getItemCount() = meds.size

    fun updateData(newList: List<Medicine>) {
        meds = newList
        notifyDataSetChanged()
    }

    private fun getNextDoseTimeText(med: Medicine): String {
        val parts = med.time.split(":")
        if (parts.size != 2) return med.time
        val hour = parts[0].toIntOrNull() ?: return med.time
        val minute = parts[1].toIntOrNull() ?: return med.time

        val shiftHours = if (med.interval > 0) med.takenDoses * med.interval else 0
        val totalMinutes = ((hour + shiftHours) * 60 + minute) % (24 * 60)
        val displayHour = totalMinutes / 60
        val displayMinute = totalMinutes % 60
        return String.format(Locale.US, "%02d:%02d", displayHour, displayMinute)
    }
}