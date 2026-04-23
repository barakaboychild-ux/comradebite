package com.comradebite

import com.comradebite.data.BaseMeal
import com.comradebite.data.MealCombination
import org.junit.Assert.*
import org.junit.Test
import java.util.*
import kotlin.random.Random

class WeeklyTimetableTest {

    // Mock constants from MealViewModel for testing
    private val BONUS_SPECIAL_MEAL = 5000.0
    private val PENALTY_NOT_SPECIAL_MEAL = 4000.0
    private val PENALTY_UGALI_LUNCH = 3000.0
    private val SCORE_AFFORDABILITY_WEIGHT = 0.7
    private val SCORE_VARIETY_WEIGHT = 0.3
    private val PENALTY_VARIETY_STEP = 500.0
    private val TWO_WEEKS_DAYS = 14L
    private val PENALTY_RARE_RECENT = 10000.0
    private val PENALTY_RARE_BASE = 1500.0
    private val RANDOM_OFFSET_MAX = 50.0

    @Test
    fun testWeeklyTimetableRules() {
        val baseMeals = listOf(
            BaseMeal(id = 1, name = "Rice", totalPrice = 200.0, numPeople = 4),
            BaseMeal(id = 2, name = "Beef", totalPrice = 300.0, numPeople = 2),
            BaseMeal(id = 3, name = "Chicken", totalPrice = 600.0, numPeople = 4),
            BaseMeal(id = 4, name = "Beans", totalPrice = 150.0, numPeople = 4)
        )

        val combos = listOf(
            MealCombination(id = 1, name = "Rice & Beans", baseNames = listOf("Rice", "Beans"), targetTime = "Lunch"),
            MealCombination(id = 2, name = "Rice & Beef", baseNames = listOf("Rice", "Beef"), targetTime = "Dinner", isSpecial = true),
            MealCombination(id = 3, name = "Rice & Chicken", baseNames = listOf("Rice", "Chicken"), targetTime = "Dinner", isSpecial = true, isRare = true),
            MealCombination(id = 4, name = "Tea & Bread", baseNames = listOf("Tea Leaves", "Bread"), targetTime = "Breakfast")
        )

        // Simulate weeklyTimetable logic
        val weekly = mutableListOf<Map<String, MealCombination?>>()
        val rareIdsUsedThisWeek = mutableSetOf<Int>()
        val allIdsUsedThisWeek = mutableMapOf<Int, Int>()
        val ingredientHistory = mutableSetOf<String>()

        val weekRandom = Random(123) // Stable seed

        repeat(7) { i ->
            val dayNumber = i + 1
            val isDaySpecial = dayNumber == 3 || dayNumber == 5 || dayNumber == 7

            val dayPlan = calculatePlanForDay(
                combos, baseMeals, 4, emptySet(), emptySet(), isDaySpecial, 
                allIdsUsedThisWeek, true, weekRandom, rareIdsUsedThisWeek, ingredientHistory
            )

            // 1. Verify No duplicate meals in a day
            val mealsInDay = dayPlan.values.filterNotNull()
            assertEquals("Should have 3 meals if enough combos exist", 3, mealsInDay.size)
            assertEquals("No duplicate meal IDs in a day", mealsInDay.size, mealsInDay.map { it.id }.distinct().size)

            // 2. Verify Special Day Logic (Wed, Fri, Sun)
            if (isDaySpecial) {
                val dinner = dayPlan["Dinner"]
                assertTrue("Special day ($dayNumber) should have a special dinner if available", dinner?.isSpecial == true)
            }

            weekly.add(dayPlan)
            dayPlan.values.filterNotNull().forEach { combo ->
                allIdsUsedThisWeek[combo.id] = allIdsUsedThisWeek.getOrDefault(combo.id, 0) + 1
                if (combo.isRare) rareIdsUsedThisWeek.add(combo.id)
                combo.baseNames.forEach { ingredientHistory.add(it.trim().lowercase()) }
            }
            if ((i + 1) % 2 == 0) ingredientHistory.clear()
        }

        // 3. Verify Rare meal strictly once per week
        val rareCombo = combos.find { it.isRare }!!
        val rareOccurrences = weekly.flatMap { it.values }.filterNotNull().count { it.id == rareCombo.id }
        assertTrue("Rare meal should appear at most once per week, found $rareOccurrences", rareOccurrences <= 1)
    }

    private fun calculatePlanForDay(
        combos: List<MealCombination>,
        baseMeals: List<BaseMeal>,
        size: Int,
        eaten: Set<String>,
        eatenIds: Set<Int>,
        forceSpecial: Boolean,
        planned: Map<Int, Int>,
        random: Boolean,
        randomSource: Random,
        rareIdsThisWeek: Set<Int>,
        weeklyIngredientHistory: Set<String>
    ): Map<String, MealCombination?> {
        val plan = mutableMapOf<String, MealCombination?>()
        val ingsUsedToday = eaten.map { it.trim().lowercase() }.toMutableSet()
        val idsUsedToday = eatenIds.toMutableSet()

        listOf("Breakfast", "Lunch", "Dinner").forEach { time ->
            val cand = combos.filter { combo ->
                combo.targetTime == time && 
                combo.id !in idsUsedToday &&
                combo.id !in rareIdsThisWeek
            }
            val filteredByIng = cand.filter { !isAnyIngredientUsed(it, ingsUsedToday) }
            val finalCandidates = if (filteredByIng.isNotEmpty()) filteredByIng else cand

            val pick = finalCandidates.maxByOrNull { combo ->
                var score = calculateScore(combo, baseMeals, size, forceSpecial)
                val timesUsedInPlan = planned[combo.id] ?: 0
                if (timesUsedInPlan > 0) {
                    val repeatPenalty = if (forceSpecial && combo.isSpecial) 1000.0 else 5000.0
                    score -= repeatPenalty * timesUsedInPlan
                }
                if (isAnyIngredientUsed(combo, weeklyIngredientHistory)) score -= 1200.0
                if (random) score += randomSource.nextDouble(0.0, RANDOM_OFFSET_MAX)
                score
            }
            plan[time] = pick
            pick?.let {
                idsUsedToday.add(it.id)
                it.baseNames.forEach { n -> ingsUsedToday.add(n.trim().lowercase()) }
            }
        }
        return plan
    }

    private fun isAnyIngredientUsed(c: MealCombination, used: Set<String>): Boolean {
        return c.baseNames.any { it.trim().lowercase() in used }
    }

    private fun calculateScore(c: MealCombination, bm: List<BaseMeal>, s: Int, isSpecialDay: Boolean): Double {
        val share = getIndividualShare(c, bm, s) ?: return -10000.0
        var score = (2000.0 / (share + 1.0)) * SCORE_AFFORDABILITY_WEIGHT
        score += (10.0 / (c.frequency + 1.0)) * SCORE_VARIETY_WEIGHT
        if (isSpecialDay) {
            if (c.isSpecial) score += BONUS_SPECIAL_MEAL
            else if (c.targetTime != "Breakfast") score -= PENALTY_NOT_SPECIAL_MEAL
        }
        if (c.isRare) score -= PENALTY_RARE_BASE
        return score
    }

    private fun getIndividualShare(c: MealCombination, bm: List<BaseMeal>, s: Int): Double? {
        var total = 0.0
        for (n in c.baseNames) {
            val m = bm.find { it.name.trim().equals(n.trim(), true) && it.numPeople == s } ?: bm.find { it.name.trim().equals(n.trim(), true) } ?: return null
            total += m.pricePerPerson
        }
        return total
    }
}
