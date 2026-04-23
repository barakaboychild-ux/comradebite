package com.comradebite.data

import kotlinx.coroutines.flow.Flow

class MealRepository(private val mealDao: MealDao) {
    // Base Meals
    val allBaseMeals: Flow<List<BaseMeal>> = mealDao.getAllBaseMeals()

    suspend fun insertBaseMeal(meal: BaseMeal) {
        mealDao.insertBaseMeal(meal)
    }

    suspend fun insertBaseMeals(meals: List<BaseMeal>) {
        mealDao.insertBaseMeals(meals)
    }

    suspend fun updateBaseMeal(meal: BaseMeal) {
        mealDao.updateBaseMeal(meal)
    }

    suspend fun deleteBaseMeal(meal: BaseMeal) {
        mealDao.deleteBaseMeal(meal)
    }

    // Combinations
    val allCombinations: Flow<List<MealCombination>> = mealDao.getAllCombinations()

    suspend fun insertCombination(combo: MealCombination) {
        mealDao.insertCombination(combo)
    }

    suspend fun insertCombinations(combos: List<MealCombination>) {
        mealDao.insertCombinations(combos)
    }

    suspend fun updateCombination(combo: MealCombination) {
        mealDao.updateCombination(combo)
    }

    suspend fun deleteCombination(combo: MealCombination) {
        mealDao.deleteCombination(combo)
    }

    fun getCombosByTime(time: String): Flow<List<MealCombination>> {
        return mealDao.getCombosByTime(time)
    }

    suspend fun getComboById(id: Int): MealCombination? {
        return mealDao.getComboById(id)
    }
}
