package com.comradebite

import com.comradebite.data.BaseMeal
import com.comradebite.data.MealCombination
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoringLogicTest {

    @Test
    fun testScoringPriority() {
        // High score means better pick
        val baseMeals = listOf(
            BaseMeal(name = "Rice", totalPrice = 100.0, numPeople = 1), // 100/p
            BaseMeal(name = "Rice", totalPrice = 300.0, numPeople = 4)  // 75/p
        )
        val combo = MealCombination(name = "Plain Rice", baseNames = listOf("Rice"), targetTime = "Lunch")

        val score1p = calculateScore(combo, baseMeals, 1)
        val score4p = calculateScore(combo, baseMeals, 4)

        // 4-person variation is cheaper per person, so it should have a higher score
        assertTrue("Cheaper per-person variation should have higher score", score4p > score1p)
    }

    private fun calculateScore(combo: MealCombination, baseMeals: List<BaseMeal>, size: Int): Double {
        val share = getIndividualShare(combo, baseMeals, size) ?: return -1.0
        val affordability = 2000.0 / (share + 1.0)
        val variety = 10.0 / (combo.frequency + 1.0)
        return (affordability * 0.7) + (variety * 0.3)
    }

    private fun getIndividualShare(combo: MealCombination, baseMeals: List<BaseMeal>, size: Int): Double? {
        var totalShare = 0.0
        for (name in combo.baseNames) {
            val preciseMatch = baseMeals.find { it.name.equals(name, true) && it.numPeople == size }
            if (preciseMatch != null) {
                totalShare += preciseMatch.pricePerPerson
            } else {
                val anyMatch = baseMeals.find { it.name.equals(name, true) } ?: return null
                totalShare += anyMatch.pricePerPerson
            }
        }
        return totalShare
    }
}
