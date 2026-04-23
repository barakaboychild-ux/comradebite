package com.comradebite.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.comradebite.viewmodel.MealViewModel

@Composable
fun GroceryAssistantScreen(viewModel: MealViewModel) {
    val allCombinations by viewModel.allCombinations.collectAsState(initial = emptyList())
    val allBaseMeals by viewModel.allBaseMeals.collectAsState(initial = emptyList())

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

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Plan Your Shopping", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text("Select Combinations & Quantities", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
            
            items(allCombinations) { combo ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(combo.name, fontWeight = FontWeight.Bold)
                            Text(combo.baseNames.joinToString(", "), style = MaterialTheme.typography.bodySmall)
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val currentQty = quantities.getOrDefault(combo.id, 0)
                            IconButton(onClick = { if (currentQty > 0) quantities[combo.id] = currentQty - 1 }) {
                                Text("-", style = MaterialTheme.typography.headlineMedium)
                            }
                            Text(currentQty.toString(), modifier = Modifier.padding(horizontal = 8.dp))
                            IconButton(onClick = { quantities[combo.id] = currentQty + 1 }) {
                                Text("+", style = MaterialTheme.typography.headlineMedium)
                            }
                        }
                    }
                }
            }
        }

        if (shoppingList.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.medium
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Total Grocery Estimate", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    shoppingList.forEach { (item, cost) ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(item)
                            Text("KSh ${cost.toInt()}")
                        }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("GRAND TOTAL", fontWeight = FontWeight.ExtraBold)
                        Text("KSh ${shoppingList.values.sum().toInt()}", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}
