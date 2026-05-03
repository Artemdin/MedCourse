package com.example.medcourse.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.medcourse.R
import com.example.medcourse.data.Medicine

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
        holder.txtTime.text = med.time // Тепер без пробілів і підключено правильно

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

        // Вивід деталей / тип + доза
        holder.details.text = "${med.type} • ${med.dosage}"

        holder.btnDelete.setOnClickListener { onDeleteClick(med) }
        holder.itemView.setOnClickListener { onItemClick(med) }
    }

    override fun getItemCount() = meds.size

    fun updateData(newList: List<Medicine>) {
        meds = newList
        notifyDataSetChanged()
    }
}