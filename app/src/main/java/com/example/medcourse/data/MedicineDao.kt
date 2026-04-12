package com.example.medcourse.data

import androidx.room.*

@Dao
interface MedicineDao {
    // Отримати список усіх ліків
    @Query("SELECT * FROM medicine_table")
    suspend fun getAllMedicine(): List<Medicine>

    // Додати ліки в базу (якщо ID співпадає — замінити)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(medicine: Medicine)

    // Видалити конкретний запис
    @Delete
    suspend fun delete(medicine: Medicine)
}