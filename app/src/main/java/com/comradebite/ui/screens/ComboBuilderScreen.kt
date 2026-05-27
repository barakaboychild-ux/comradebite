package com.comradebite.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.sp
import com.comradebite.data.FoodDatabase
import com.comradebite.ui.theme.CyanAccent
import com.comradebite.viewmodel.MealViewModel

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ComboBuilderScreen(viewModel: MealViewModel) {
    val allBaseMeals by viewModel.allBaseMeals.collectAsState(initial = emptyList())
    val allCombinations by viewModel.allCombinations.collectAsState(initial = emptyList())
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()

    // terminology: Meals instead of ingredients
    val uniqueMeals = allBaseMeals.map { it.name.lowercase() }.distinct()
    var selectedMeals by remember { mutableStateOf(setOf<String>()) }
    var comboName by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf("Breakfast") }
    var isSpecial by remember { mutableStateOf(false) }
    var isRare by remember { mutableStateOf(false) }

    val textColor = if (isDarkTheme) Color.White else Color.Black

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Combine Your Meals", style = MaterialTheme.typography.titleMedium, color = textColor)
        
        Text(
            "Select meals from your inventory to create a combination:",
            style = MaterialTheme.typography.bodySmall,
            color = textColor.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            uniqueMeals.forEach { mealName ->
                val isSelected = selectedMeals.contains(mealName)
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        selectedMeals = if (isSelected) selectedMeals - mealName else selectedMeals + mealName
                    },
                    label = { Text(mealName) }
                )
            }
        }

        // Live Chef AI Nutrition Preview
        if (selectedMeals.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = if(isDarkTheme) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text("CHEF AI'S PREVIEW", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = CyanAccent)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        val groups = selectedMeals.mapNotNull { FoodDatabase.getFoodData(it) }.distinctBy { it.group }
                        val totalKcal = selectedMeals.sumOf { FoodDatabase.getFoodData(it)?.kcal ?: 0 }
                        
                        groups.forEach { data ->
                            SuggestionChip(
                                onClick = {},
                                label = { Text("${data.emoji} ${data.label}") },
                                colors = SuggestionChipDefaults.suggestionChipColors(labelColor = CyanAccent)
                            )
                        }
                        if (totalKcal > 0) {
                            SuggestionChip(
                                onClick = {},
                                label = { Text("🔥 ~$totalKcal kcal") },
                                colors = SuggestionChipDefaults.suggestionChipColors(labelColor = Color(0xFFF43F5E))
                            )
                        }
                    }
                }
            }
        }

        OutlinedTextField(
            value = comboName,
            onValueChange = { comboName = it },
            label = { Text("Combo Name (e.g. Rice & Beans)") },
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

        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Column(Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Special Occasion", style = MaterialTheme.typography.bodyLarge, color = textColor, fontWeight = FontWeight.Bold)
                        Text("For weekends or treats", style = MaterialTheme.typography.bodySmall, color = textColor.copy(alpha = 0.6f))
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
                        Text("Rare Choice", style = MaterialTheme.typography.bodyLarge, color = textColor, fontWeight = FontWeight.Bold)
                        Text("Keep it infrequent", style = MaterialTheme.typography.bodySmall, color = textColor.copy(alpha = 0.6f))
                    }
                    Switch(checked = isRare, onCheckedChange = { isRare = it })
                }
            }
        }

        Button(
            onClick = {
                if (comboName.isNotBlank() && selectedMeals.isNotEmpty()) {
                    viewModel.saveCombination(comboName, selectedMeals.toList(), selectedTime, isSpecial, isRare)
                    comboName = ""; selectedMeals = emptySet(); isSpecial = false; isRare = false
                }
            },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black)
        ) {
            Text("Create Combo", fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(24.dp))
        Text("Your Custom Combos", style = MaterialTheme.typography.labelLarge, color = textColor)

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
                            Text(combo.baseNames.joinToString(" + "), style = MaterialTheme.typography.bodySmall, color = textColor.copy(alpha = 0.7f))
                            Text(
                                text = "${combo.targetTime}${if(combo.isSpecial) " • Special" else " • Regular"}${if(combo.isRare) " • Rare" else ""}",
                                style = MaterialTheme.typography.labelSmall, 
                                color = CyanAccent
                            )
                        }
                        Row {
                            IconButton(onClick = { viewModel.uploadComboToFirebase(combo) }) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Sync", tint = CyanAccent)
                            }
                            IconButton(onClick = { viewModel.deleteCombination(combo) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
            }
        }
    }
}
