package com.comradebite.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {
    // Base Meals (Inventory)
    @Query("SELECT * FROM base_meals ORDER BY name ASC")
    fun getAllBaseMeals(): Flow<List<BaseMeal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBaseMeal(meal: BaseMeal)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBaseMeals(meals: List<BaseMeal>)

    @Update
    suspend fun updateBaseMeal(meal: BaseMeal)

    @Delete
    suspend fun deleteBaseMeal(meal: BaseMeal)

    // Combinations
    @Query("SELECT * FROM meal_combinations")
    fun getAllCombinations(): Flow<List<MealCombination>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCombination(combo: MealCombination)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCombinations(combos: List<MealCombination>)

    @Update
    suspend fun updateCombination(combo: MealCombination)

    @Delete
    suspend fun deleteCombination(combo: MealCombination)

    @Query("SELECT * FROM meal_combinations WHERE targetTime = :time")
    fun getCombosByTime(time: String): Flow<List<MealCombination>>

    @Query("SELECT * FROM meal_combinations WHERE id = :id")
    suspend fun getComboById(id: Int): MealCombination?
}
