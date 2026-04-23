package com.comradebite.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a combination of ingredients (BaseMeals) under a name.
 */
@Entity(tableName = "meal_combinations")
data class MealCombination(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val baseNames: List<String>,
    val targetTime: String, // Breakfast, Lunch, Dinner
    val frequency: Int = 0,
    val lastEaten: Long = 0,
    val isSpecial: Boolean = false, // Reserved for special days (e.g. Friday/Sunday Dinners)
    val isRare: Boolean = false     // Should appear very infrequently (variety logic)
)
