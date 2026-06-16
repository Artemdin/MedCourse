package com.example.medcourse.data

// Абстрактний базовий клас. Визначає спільний контракт для всіх ліків
abstract class MedicineDosage(
    val name: String,
    val dosageValue: String
) {
    abstract fun getDosageInstruction(): String
}
class GeneralDosage(name: String, dosageValue: String)
    : MedicineDosage(name, dosageValue) {

    override fun getDosageInstruction(): String {
        return "Прийняти ліки: $name ($dosageValue)"
    }
}
// Нащадок для твердих ліків (пігулки, капсули)
class PillDosage(
    name: String, dosageValue: String)
    : MedicineDosage(name, dosageValue) {

    override fun getDosageInstruction(): String {
        return "💊 Випити $dosageValue шт. ($name)" // логіка для пігулок
    }
}

// Нащадок для сиропів
class SyrupDosage(name: String, dosageValue: String)
    : MedicineDosage(name, dosageValue) {

    override fun getDosageInstruction(): String {
        return "🥄 Відміряти $dosageValue мл. сиропу ($name)" // логіка для рідин
    }
}

// Нащадок для уколів
class InjectionDosage(name: String, dosageValue: String)
    : MedicineDosage(name, dosageValue) {

    override fun getDosageInstruction(): String {
        return "💉 Зробити $dosageValue укол(ів) ($name)" // логіка для ін'єкцій
    }
}
