package com.example.medcourse

import java.time.LocalTime

data class Medicine(
    val name: String,
    val type: String,
    val dosage: String,
    val time: LocalTime,
    var isTaken: Boolean = false
)