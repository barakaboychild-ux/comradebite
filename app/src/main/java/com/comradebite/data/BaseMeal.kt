package com.comradebite.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents an individual ingredient and its price for a certain group size.
 * e.g., "Rice" for 1 person vs "Rice" for 4 people.
 */
@Entity(tableName = "base_meals")
data class BaseMeal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val totalPrice: Double,
    val numPeople: Int,
    val pricePerPerson: Double = totalPrice / numPeople.toDouble()
)
