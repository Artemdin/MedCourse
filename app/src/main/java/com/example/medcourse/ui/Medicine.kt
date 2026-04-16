package com.example.medcourse.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medicine_table")
data class Medicine(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,

    val name: String,
    val type: String,
    val dosage: String,
    val time: String,
    var isTaken: Boolean = false,
    val days: String
)