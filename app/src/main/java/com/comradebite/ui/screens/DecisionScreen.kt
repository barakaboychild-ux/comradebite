package com.comradebite.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.comradebite.viewmodel.MealViewModel

@Composable
fun DecisionScreen(viewModel: MealViewModel) {
    var budgetStr by remember { mutableStateOf("") }
    val budget by viewModel.budgetPerPerson.collectAsState()
    val allCombinations by viewModel.allCombinations.collectAsState(initial = emptyList())
    val allBaseMeals by viewModel.allBaseMeals.collectAsState(initial = emptyList())
    val groupSize by viewModel.groupSize.collectAsState()
    val eatenIngredients by viewModel.eatenTodayIngredients.collectAsState()

    var selectedTime by remember { mutableStateOf("Lunch") }

    val filteredCombos = allCombinations.filter { combo ->
        val share = viewModel.getIndividualShare(combo, allBaseMeals, groupSize) ?: Double.MAX_VALUE
        val isAffordable = budget == null || share <= (budget!!)
        val isNotEaten = !combo.baseNames.any { eatenIngredients.contains(it.lowercase()) }
        combo.targetTime == selectedTime && isAffordable && isNotEaten
    }.sortedByDescending { combo ->
        // Direct score calculation for the list
        val share = viewModel.getIndividualShare(combo, allBaseMeals, groupSize) ?: 1.0
        val affordability = 2000.0 / (share + 1.0)
        val variety = 10.0 / (combo.frequency + 1.0)
        (affordability * 0.7) + (variety * 0.3)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = budgetStr,
            onValueChange = { 
                budgetStr = it
                viewModel.setBudget(it.toDoubleOrNull())
            },
            label = { Text("Budget per person (KSh)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(12.dp))

        val times = listOf("Breakfast", "Lunch", "Dinner")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            times.forEach { time ->
                FilterChip(
                    selected = selectedTime == time,
                    onClick = { selectedTime = time },
                    label = { Text(time) },
                    modifier = Modifier.weight(1.0f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        if (filteredCombos.isNotEmpty()) {
            Button(
                onClick = { 
                    val pick = filteredCombos.random()
                    viewModel.markAsEaten(pick)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(Icons.Default.Refresh, null)
                Spacer(Modifier.width(8.dp))
                Text("Quick Pick (Random from below)")
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 16.dp)) {
            items(filteredCombos) { combo ->
                val share = viewModel.getIndividualShare(combo, allBaseMeals, groupSize) ?: 0.0
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Column {
                            Text(combo.name, fontWeight = FontWeight.Bold)
                            Text("Your share: KSh ${share.toInt()}", color = MaterialTheme.colorScheme.primary)
                        }
                        Button(onClick = { viewModel.markAsEaten(combo) }) {
                            Text("Eat This")
                        }
                    }
                }
            }
        }
    }
}
