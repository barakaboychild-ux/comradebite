package com.comradebite.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.comradebite.ui.theme.CyanAccent
import com.comradebite.viewmodel.MealViewModel
import com.comradebite.viewmodel.SyncStatus
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(viewModel: MealViewModel) {
    val allBaseMeals by viewModel.allBaseMeals.collectAsState(initial = emptyList())
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val groupCode by viewModel.groupCode.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val userGroups by viewModel.userGroups.collectAsState()
    
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val context = LocalContext.current
    
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var serves by remember { mutableStateOf("1") }
    var editingMealId by remember { mutableStateOf<Int?>(null) }

    val textColor = if (isDarkTheme) Color.White else Color.Black

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Compact Group Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Inventory Sync", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = CyanAccent)
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

        // Add/Edit Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = if(isDarkTheme) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f)),
            border = BorderStroke(1.dp, textColor.copy(alpha = 0.1f))
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(if (editingMealId != null) "Edit Variation" else "Add Variation", fontWeight = FontWeight.Bold, color = textColor)
                Spacer(Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item Name (e.g. Rice)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyanAccent)
                )
                
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("Price (KSh)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyanAccent)
                    )
                    OutlinedTextField(
                        value = serves,
                        onValueChange = { serves = it },
                        label = { Text("Serves") },
                        modifier = Modifier.weight(0.6f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyanAccent)
                    )
                }
                
                Button(
                    onClick = {
                        if (name.isNotBlank() && price.isNotBlank()) {
                            val p = price.toDoubleOrNull() ?: 0.0
                            val s = serves.toIntOrNull() ?: 1
                            viewModel.insertBaseMeal(name, p, s)
                            name = ""; price = ""; serves = "1"; editingMealId = null
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black)
                ) {
                    Text(if (editingMealId != null) "Update" else "Save Variation", fontWeight = FontWeight.ExtraBold)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("Stock & Variations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = textColor)

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 8.dp)) {
            items(allBaseMeals) { meal ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = if (isDarkTheme) Color.Black.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.6f)),
                    border = BorderStroke(0.5.dp, textColor.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(meal.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textColor)
                            Text("Serves ${meal.numPeople} • KSh ${meal.totalPrice.toInt()}", fontSize = 12.sp, color = Color.Gray)
                        }
                        Row {
                            if (syncStatus == SyncStatus.SYNCED) {
                                IconButton(onClick = { viewModel.uploadInventoryListToFirebase(listOf(meal)) }) {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Share", tint = CyanAccent, modifier = Modifier.size(20.dp))
                                }
                            }
                            IconButton(onClick = { 
                                editingMealId = meal.id
                                name = meal.name
                                price = meal.totalPrice.toString()
                                serves = meal.numPeople.toString()
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray, modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = { viewModel.deleteBaseMeal(meal) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
