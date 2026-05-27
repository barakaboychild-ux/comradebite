package com.comradebite.data

data class FoodData(
    val kcal: Int,
    val group: String, // carb, protein, vegetable, fruit, dairy, other
    val emoji: String,
    val label: String
)

object FoodDatabase {
    val DATA = mapOf(
        "rice" to FoodData(390, "carb", "🍚", "Carb"),
        "ugali" to FoodData(450, "carb", "🫓", "Carb"),
        "chapati" to FoodData(300, "carb", "🫓", "Carb"),
        "bread" to FoodData(260, "carb", "🍞", "Carb"),
        "mandazi" to FoodData(350, "carb", "🍩", "Carb"),
        "porridge" to FoodData(180, "carb", "🥣", "Carb"),
        "potato" to FoodData(231, "carb", "🥔", "Carb"),
        "sweet potato" to FoodData(258, "carb", "🍠", "Carb"),
        "plantain" to FoodData(366, "carb", "🍌", "Carb"),
        "pasta" to FoodData(371, "carb", "🍝", "Carb"),
        "noodles" to FoodData(385, "carb", "🍜", "Carb"),
        "githeri" to FoodData(360, "mixed", "🥘", "Mixed"),
        "pilau" to FoodData(600, "mixed", "🍛", "Mixed"),
        "beans" to FoodData(390, "protein", "🫘", "Protein"),
        "lentils" to FoodData(348, "protein", "🫘", "Protein"),
        "egg" to FoodData(155, "protein", "🥚", "Protein"),
        "eggs" to FoodData(155, "protein", "🥚", "Protein"),
        "chicken" to FoodData(495, "protein", "🍗", "Protein"),
        "beef" to FoodData(600, "protein", "🥩", "Protein"),
        "fish" to FoodData(420, "protein", "🐟", "Protein"),
        "pork" to FoodData(720, "protein", "🥩", "Protein"),
        "meat" to FoodData(550, "protein", "🥩", "Protein"),
        "groundnuts" to FoodData(570, "protein", "🥜", "Protein"),
        "peanuts" to FoodData(570, "protein", "🥜", "Protein"),
        "sukuma" to FoodData(50, "vegetable", "🥬", "Vegetable"),
        "kale" to FoodData(50, "vegetable", "🥬", "Vegetable"),
        "spinach" to FoodData(23, "vegetable", "🥬", "Vegetable"),
        "cabbage" to FoodData(25, "vegetable", "🥦", "Vegetable"),
        "carrot" to FoodData(41, "vegetable", "🥕", "Vegetable"),
        "tomato" to FoodData(18, "vegetable", "🍅", "Vegetable"),
        "onion" to FoodData(40, "vegetable", "🧅", "Vegetable"),
        "peas" to FoodData(81, "vegetable", "🫛", "Vegetable"),
        "banana" to FoodData(89, "fruit", "🍌", "Fruit"),
        "mango" to FoodData(60, "fruit", "🥭", "Fruit"),
        "apple" to FoodData(52, "fruit", "🍎", "Fruit"),
        "orange" to FoodData(47, "fruit", "🍊", "Fruit"),
        "avocado" to FoodData(160, "fruit", "🥑", "Fruit"),
        "passion" to FoodData(97, "fruit", "🍋", "Fruit"),
        "watermelon" to FoodData(30, "fruit", "🍉", "Fruit"),
        "pineapple" to FoodData(50, "fruit", "🍍", "Fruit"),
        "milk" to FoodData(126, "dairy", "🥛", "Dairy"),
        "yogurt" to FoodData(177, "dairy", "🥛", "Dairy"),
        "cheese" to FoodData(400, "dairy", "🧀", "Dairy"),
        "tea" to FoodData(30, "other", "☕", "Other"),
        "coffee" to FoodData(15, "other", "☕", "Other")
    )

    fun getFoodData(name: String): FoodData? {
        val lower = name.lowercase().trim()
        return DATA.entries.find { lower.contains(it.key) }?.value
    }
}
