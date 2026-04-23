package com.comradebite.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.comradebite.viewmodel.MealViewModel

@Composable
fun MealInputScreen(viewModel: MealViewModel) {
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var people by remember { mutableStateOf("1") }

    val allBaseMeals by viewModel.allBaseMeals.collectAsState(initial = emptyList())

    Column(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Ingredient Name (e.g. Rice)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = price,
            onValueChange = { price = it },
            label = { Text("Total Price") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = people,
            onValueChange = { people = it },
            label = { Text("Number of People") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                val p = price.toDoubleOrNull() ?: 0.0
                val n = people.toIntOrNull() ?: 1
                if (name.isNotBlank() && p > 0) {
                    viewModel.insertBaseMeal(name, p, n)
                    name = ""
                    price = ""
                    people = "1"
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Ingredient")
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text("Recorded Ingredients", style = MaterialTheme.typography.titleLarge)
        HorizontalDivider()
        
        LazyColumn {
            items(allBaseMeals) { meal ->
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(meal.name, style = MaterialTheme.typography.bodyLarge)
                    Text("Total: KSh ${meal.totalPrice} for ${meal.numPeople} people (KSh ${"%.2f".format(meal.pricePerPerson)} each)", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
