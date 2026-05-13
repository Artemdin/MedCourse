package com.example.medcourse.data

// Базовий клас (Суперклас). Назвали інакше, щоб не було конфлікту з БД
open class MedicineDosage(
    val name: String,
    val dosageValue: String
) {
    // кожен нащадок реалізує його по-своєму
    open fun getDosageInstruction(): String {
        return "Прийняти ліки: $name ($dosageValue)"
    }
}

// Нащадок для твердих ліків (пігулки, капсули)
class PillDosage(name: String, dosageValue: String)
    : MedicineDosage(name, dosageValue) {

    override fun getDosageInstruction(): String {
        return "💊 Випити $dosageValue шт. ($name)" // -- Своя логіка для пігулок
    }
}

// Нащадок для рідких ліків (краплі, сиропи, уколи)
class LiquidDosage(name: String, dosageValue: String)
    : MedicineDosage(name, dosageValue) {

    override fun getDosageInstruction(): String {
        return "💧 Відміряти $dosageValue мл. розчину ($name)" // -- Своя логіка для рідин
    }
}