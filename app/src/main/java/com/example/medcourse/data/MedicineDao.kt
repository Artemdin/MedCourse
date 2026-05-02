package com.example.medcourse.data

import androidx.room.*

@Dao
interface MedicineDao {

    // Додати ліки в базу,якщо ID співпадає — замінити
    // Це універсальний метод: він і додає нові, і може оновлювати старі.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(medicine: Medicine)

    // Для явного редагування існуючих ліків
    @Update
    suspend fun update(medicine: Medicine)

    // Видалити конкретний запис
    @Delete
    suspend fun delete(medicine: Medicine)

    // Отримати список усіх ліків для головного екрана
    @Query("SELECT * FROM medicine_table")
    suspend fun getAllMedicine(): List<Medicine>

    // Логіка для кнопки "Прийняв": додаємо +1 до прийнятих доз
    @Query("UPDATE medicine_table SET takenDoses = takenDoses + 1 WHERE id = :medId AND takenDoses < totalDoses")
    suspend fun incrementTakenDoses(medId: Int)
}