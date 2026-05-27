package com.comradebite.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.comradebite.data.FoodDatabase
import com.comradebite.ui.theme.CyanAccent
import com.comradebite.viewmodel.MealViewModel

@Composable
fun GroceryAssistantScreen(viewModel: MealViewModel) {
    val allCombinations by viewModel.allCombinations.collectAsState(initial = emptyList())
    val allBaseMeals by viewModel.allBaseMeals.collectAsState(initial = emptyList())
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()

    // Map to store quantities for each combination
    val quantities = remember { mutableStateMapOf<Int, Int>() }

    val shoppingList = remember(quantities.toMap(), allCombinations, allBaseMeals) {
        val list = mutableMapOf<String, Double>()
        quantities.forEach { (comboId, qty) ->
            if (qty > 0) {
                val combo = allCombinations.find { it.id == comboId }
                combo?.baseNames?.forEach { ingredient ->
                    val baseMeal = allBaseMeals.find { it.name.equals(ingredient, ignoreCase = true) }
                    if (baseMeal != null) {
                        val currentTotal = list.getOrDefault(ingredient, 0.0)
                        list[ingredient] = currentTotal + (baseMeal.totalPrice * qty)
                    }
                }
            }
        }
        list
    }

    val nutritionInsight = remember(shoppingList.keys) {
        if (shoppingList.isEmpty()) null
        else {
            val groups = shoppingList.keys.mapNotNull { FoodDatabase.getFoodData(it)?.group }.toSet()
            val missing = listOf("carb", "protein", "vegetable", "fruit").filter { !groups.contains(it) }
            missing
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Grocery Assistant", style = MaterialTheme.typography.headlineSmall, color = if(isDarkTheme) Color.White else Color.Black, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text("How many times will you eat these this week?", style = MaterialTheme.typography.bodyMedium, color = if(isDarkTheme) Color.White.copy(alpha = 0.6f) else Color.Gray)
            }
            
            items(allCombinations) { combo ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = if(isDarkTheme) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f)),
                    border = BorderStroke(1.dp, (if(isDarkTheme) Color.White else Color.Black).copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(combo.name, fontWeight = FontWeight.Bold, color = if(isDarkTheme) Color.White else Color.Black)
                            Text(combo.baseNames.joinToString(", "), style = MaterialTheme.typography.bodySmall, color = if(isDarkTheme) Color.White.copy(alpha = 0.6f) else Color.Gray)
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val currentQty = quantities.getOrDefault(combo.id, 0)
                            IconButton(onClick = { if (currentQty > 0) quantities[combo.id] = currentQty - 1 }) {
                                Text("-", style = MaterialTheme.typography.headlineMedium, color = CyanAccent)
                            }
                            Text(currentQty.toString(), modifier = Modifier.padding(horizontal = 8.dp), color = if(isDarkTheme) Color.White else Color.Black)
                            IconButton(onClick = { quantities[combo.id] = currentQty + 1 }) {
                                Text("+", style = MaterialTheme.typography.headlineMedium, color = CyanAccent)
                            }
                        }
                    }
                }
            }
        }

        if (shoppingList.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            
            // AI Insight Block
            nutritionInsight?.let { missing ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = if(missing.isEmpty()) Color(0xFF10B981).copy(alpha = 0.1f) else Color(0xFFF59E0B).copy(alpha = 0.1f)),
                    border = BorderStroke(1.dp, (if(missing.isEmpty()) Color(0xFF10B981) else Color(0xFFF59E0B)).copy(alpha = 0.3f))
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            text = if (missing.isEmpty()) "✅ Balanced Weekly Shop!" else "⚠️ Missing Food Groups",
                            fontWeight = FontWeight.Bold,
                            color = if (missing.isEmpty()) Color(0xFF10B981) else Color(0xFFF59E0B),
                            fontSize = 14.sp
                        )
                        if (missing.isNotEmpty()) {
                            val msg = "Your list is missing: ${missing.joinToString(", ")}. Consider adding some to stay healthy!"
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.7f)
                            )
                        } else {
                            Text(
                                text = "Great job! You've covered all the essential nutrients for the week.",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = CyanAccent.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.3f))
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Bulk Market Estimate", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CyanAccent)
                    HorizontalDivider(Modifier.padding(vertical = 8.dp), color = CyanAccent.copy(alpha = 0.2f))
                    shoppingList.forEach { (item, cost) ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(item, color = if(isDarkTheme) Color.White else Color.Black)
                            Text("KSh ${cost.toInt()}", color = if(isDarkTheme) Color.White else Color.Black)
                        }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 4.dp), color = CyanAccent.copy(alpha = 0.2f))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("TOTAL", fontWeight = FontWeight.ExtraBold, color = if(isDarkTheme) Color.White else Color.Black)
                        Text("KSh ${shoppingList.values.sum().toInt()}", fontWeight = FontWeight.ExtraBold, color = CyanAccent, fontSize = 18.sp)
                    }
                }
            }
        }
    }
}
