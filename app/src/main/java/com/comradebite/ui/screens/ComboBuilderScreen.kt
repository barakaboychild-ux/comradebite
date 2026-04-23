package com.comradebite.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.comradebite.viewmodel.MealViewModel

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ComboBuilderScreen(viewModel: MealViewModel) {
    val allBaseMeals by viewModel.allBaseMeals.collectAsState(initial = emptyList())
    val allCombinations by viewModel.allCombinations.collectAsState(initial = emptyList())
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()

    val uniqueIngredients = allBaseMeals.map { it.name.lowercase() }.distinct()
    var selectedIngredients by remember { mutableStateOf(setOf<String>()) }
    var comboName by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf("Breakfast") }
    var isSpecial by remember { mutableStateOf(false) }
    var isRare by remember { mutableStateOf(false) }

    val textColor = if (isDarkTheme) Color.White else Color.Black

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Combine Ingredients", style = MaterialTheme.typography.titleMedium, color = textColor)
        
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            uniqueIngredients.forEach { ingredient ->
                val isSelected = selectedIngredients.contains(ingredient)
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        selectedIngredients = if (isSelected) selectedIngredients - ingredient else selectedIngredients + ingredient
                    },
                    label = { Text(ingredient) }
                )
            }
        }

        OutlinedTextField(
            value = comboName,
            onValueChange = { comboName = it },
            label = { Text("Combination Name (e.g. Rice & Beans)") },
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = textColor,
                unfocusedTextColor = textColor
            )
        )

        val times = listOf("Breakfast", "Lunch", "Dinner")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            times.forEach { time ->
                ElevatedFilterChip(
                    selected = selectedTime == time,
                    onClick = { selectedTime = time },
                    label = { Text(time) },
                    modifier = Modifier.weight(1.0f)
                )
            }
        }

        // Row for Special Occasion and Rare Meal Switches
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Column(Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Special Occasion", style = MaterialTheme.typography.bodyLarge, color = textColor, fontWeight = FontWeight.Bold)
                        Text("e.g. Weekends", style = MaterialTheme.typography.bodySmall, color = textColor.copy(alpha = 0.6f))
                    }
                    Switch(checked = isSpecial, onCheckedChange = { isSpecial = it })
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Rare Meal", style = MaterialTheme.typography.bodyLarge, color = textColor, fontWeight = FontWeight.Bold)
                        Text("Limit frequency", style = MaterialTheme.typography.bodySmall, color = textColor.copy(alpha = 0.6f))
                    }
                    Switch(checked = isRare, onCheckedChange = { isRare = it })
                }
            }
        }

        Button(
            onClick = {
                if (comboName.isNotBlank() && selectedIngredients.isNotEmpty()) {
                    viewModel.saveCombination(comboName, selectedIngredients.toList(), selectedTime, isSpecial, isRare)
                    comboName = ""; selectedIngredients = emptySet(); isSpecial = false; isRare = false
                }
            },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            Text("Create Combination")
        }

        Spacer(Modifier.height(24.dp))
        Text("Your Combinations (Tap ➹ to sync online)", style = MaterialTheme.typography.labelLarge, color = textColor)

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
            items(allCombinations) { combo ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkTheme) Color.Black.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.8f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(combo.name, fontWeight = FontWeight.Bold, color = textColor)
                                if (combo.isSpecial) {
                                    Spacer(Modifier.width(8.dp))
                                    Icon(Icons.Default.Star, contentDescription = "Special", tint = Color(0xFFFACC15), modifier = Modifier.size(16.dp))
                                }
                            }
                            Text(combo.baseNames.joinToString(", "), style = MaterialTheme.typography.bodySmall, color = textColor.copy(alpha = 0.7f))
                            Text(
                                text = "${combo.targetTime}${if(combo.isSpecial) " • Special" else " • Regular"}${if(combo.isRare) " • Rare" else ""}",
                                style = MaterialTheme.typography.labelSmall, 
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Row {
                            IconButton(onClick = { viewModel.uploadComboToFirebase(combo) }) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Sync", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { viewModel.deleteCombination(combo) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}
