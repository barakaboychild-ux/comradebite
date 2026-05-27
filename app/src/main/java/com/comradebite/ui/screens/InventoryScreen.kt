package com.comradebite.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.comradebite.ui.theme.CyanAccent
import com.comradebite.viewmodel.MealViewModel
import com.comradebite.viewmodel.SyncStatus

data class VariationInput(
    val price: String = "",
    val serves: String = "1"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    viewModel: MealViewModel,
    onNamePositioned: (Rect) -> Unit = {},
    onPlusPositioned: (Rect) -> Unit = {},
    onSavePositioned: (Rect) -> Unit = {},
    onPlusClicked: () -> Unit = {},
    onSaveClicked: () -> Unit = {}
) {
    val allBaseMeals by viewModel.allBaseMeals.collectAsState(initial = emptyList())
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val groupCode by viewModel.groupCode.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    
    var name by remember { mutableStateOf("") }
    var variationInputs by remember { mutableStateOf(listOf(VariationInput())) }
    var editingMealId by remember { mutableStateOf<Int?>(null) }

    val textColor = if (isDarkTheme) Color.White else Color.Black

    val groupedMeals = allBaseMeals.groupBy { it.name.trim().lowercase() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Meal Inventory", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = CyanAccent)
            if (groupCode.isNotBlank()) {
                Surface(
                    color = CyanAccent.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.3f))
                ) {
                    Text(
                        "Code: $groupCode", 
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = CyanAccent
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = if(isDarkTheme) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f)),
            border = BorderStroke(1.dp, textColor.copy(alpha = 0.1f))
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = if (editingMealId != null) "Edit Item" else "Add New Item", 
                    fontWeight = FontWeight.Bold, 
                    color = textColor
                )
                Spacer(Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item Name (e.g. Rice)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coords ->
                            onNamePositioned(coords.positionInRoot().run { Rect(this, androidx.compose.ui.geometry.Size(coords.size.width.toFloat(), coords.size.height.toFloat())) })
                        },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyanAccent)
                )

                variationInputs.forEachIndexed { index, variation ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = variation.price,
                            onValueChange = { newPrice ->
                                variationInputs = variationInputs.toMutableList().apply {
                                    this[index] = this[index].copy(price = newPrice)
                                }
                            },
                            label = { Text("Price (Optional)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyanAccent)
                        )
                        OutlinedTextField(
                            value = variation.serves,
                            onValueChange = { newServes ->
                                variationInputs = variationInputs.toMutableList().apply {
                                    this[index] = this[index].copy(serves = newServes)
                                }
                            },
                            label = { Text("Serves") },
                            modifier = Modifier.weight(0.6f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyanAccent)
                        )
                        if (variationInputs.size > 1) {
                            IconButton(onClick = {
                                variationInputs = variationInputs.toMutableList().apply { removeAt(index) }
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.Red.copy(alpha = 0.6f))
                            }
                        }
                    }
                }

                if (editingMealId == null) {
                    IconButton(
                        onClick = { 
                            variationInputs = variationInputs + VariationInput()
                            onPlusClicked()
                        },
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .onGloballyPositioned { coords ->
                                onPlusPositioned(coords.positionInRoot().run { Rect(this, androidx.compose.ui.geometry.Size(coords.size.width.toFloat(), coords.size.height.toFloat())) })
                            }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Variation", tint = CyanAccent)
                    }
                }
                
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            variationInputs.forEach { v ->
                                val p = v.price.toDoubleOrNull() ?: 0.0
                                val s = v.serves.toIntOrNull() ?: 1
                                viewModel.insertBaseMeal(name, p, s, editingMealId ?: 0)
                            }
                            name = ""
                            variationInputs = listOf(VariationInput())
                            editingMealId = null
                            onSaveClicked()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .onGloballyPositioned { coords ->
                            onSavePositioned(coords.positionInRoot().run { Rect(this, androidx.compose.ui.geometry.Size(coords.size.width.toFloat(), coords.size.height.toFloat())) })
                        },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black)
                ) {
                    Text(if (editingMealId != null) "Update Variation" else "Save to Inventory", fontWeight = FontWeight.ExtraBold)
                }
                
                if (editingMealId != null) {
                    TextButton(
                        onClick = { 
                            name = ""
                            variationInputs = listOf(VariationInput())
                            editingMealId = null 
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Cancel Edit", color = Color.Gray)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("Current Inventory", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = textColor)

        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 8.dp)) {
            groupedMeals.forEach { (mealName, variations) ->
                item(key = mealName) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = if (isDarkTheme) Color.Black.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.4f)),
                        border = BorderStroke(1.dp, CyanAccent.copy(alpha = 0.2f))
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = mealName.replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = CyanAccent
                                )
                                IconButton(
                                    onClick = {
                                        name = mealName.replaceFirstChar { it.uppercase() }
                                        variationInputs = listOf(VariationInput())
                                        editingMealId = null
                                    },
                                    modifier = Modifier.background(CyanAccent.copy(alpha = 0.1f), CircleShape).size(32.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add Variation", tint = CyanAccent, modifier = Modifier.size(20.dp))
                                }
                            }
                            
                            Spacer(Modifier.height(12.dp))
                            
                            variations.sortedBy { it.numPeople }.forEach { variation ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            text = if (variation.totalPrice > 0) "KSh ${variation.totalPrice.toInt()} - ${variation.numPeople} ppl" else "Price not set - ${variation.numPeople} ppl",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium,
                                            color = textColor
                                        )
                                        if (variation.totalPrice > 0) {
                                            Text(
                                                text = "KSh ${variation.pricePerPerson.toInt()} per head",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                    Row {
                                        if (syncStatus == SyncStatus.SYNCED) {
                                            IconButton(onClick = { viewModel.uploadInventoryListToFirebase(listOf(variation)) }) {
                                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Sync", tint = CyanAccent, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                        IconButton(onClick = { 
                                            editingMealId = variation.id
                                            name = variation.name
                                            variationInputs = listOf(VariationInput(if(variation.totalPrice > 0) variation.totalPrice.toInt().toString() else "", variation.numPeople.toString()))
                                        }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray, modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(onClick = { viewModel.deleteBaseMeal(variation) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                                if (variation != variations.last()) {
                                    HorizontalDivider(thickness = 0.5.dp, color = textColor.copy(alpha = 0.1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
