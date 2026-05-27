package com.comradebite.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.comradebite.ui.theme.CyanAccent
import com.comradebite.viewmodel.MealViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

@Composable
fun LoginScreen(viewModel: MealViewModel, onLoginSuccess: () -> Unit) {
    var isRegisterMode by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseDatabase.getInstance("https://comradebite-c0265c98-default-rtdb.firebaseio.com")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isRegisterMode) "Register" else "Login",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = CyanAccent
        )
        
        Spacer(Modifier.height(8.dp))

        Text(
            text = if (isRegisterMode) "Create an account to sync with comrades" else "Welcome back, Comrade!",
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
        
        Spacer(Modifier.height(32.dp))

        if (isRegisterMode) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyanAccent, focusedLabelColor = CyanAccent)
            )
            Spacer(Modifier.height(12.dp))
        }

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email Address") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyanAccent, focusedLabelColor = CyanAccent)
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyanAccent, focusedLabelColor = CyanAccent)
        )

        Spacer(Modifier.height(32.dp))
        
        Button(
            onClick = {
                if (email.isBlank() || password.isBlank() || (isRegisterMode && name.isBlank())) {
                    Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                
                isLoading = true
                if (isRegisterMode) {
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnSuccessListener { result ->
                            val user = result.user
                            // Save profile details to Database
                            val profile = mapOf(
                                "name" to name,
                                "email" to email,
                                "createdAt" to System.currentTimeMillis()
                            )
                            db.getReference("users").child(user?.uid ?: "").child("profile")
                                .setValue(profile)
                                .addOnCompleteListener {
                                    isLoading = false
                                    onLoginSuccess()
                                }
                        }
                        .addOnFailureListener {
                            isLoading = false
                            Toast.makeText(context, "Registration Failed: ${it.message}", Toast.LENGTH_LONG).show()
                        }
                } else {
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnSuccessListener {
                            isLoading = false
                            onLoginSuccess()
                        }
                        .addOnFailureListener {
                            isLoading = false
                            Toast.makeText(context, "Login Failed: ${it.message}", Toast.LENGTH_LONG).show()
                        }
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black)
        ) {
            if (isLoading) CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
            else Text(if (isRegisterMode) "Create Account" else "Login", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        
        Spacer(Modifier.height(24.dp))

        TextButton(onClick = { isRegisterMode = !isRegisterMode }) {
            Text(
                text = if (isRegisterMode) "login instead if you have an account" else "register instead if you don't have an account",
                color = CyanAccent,
                textAlign = TextAlign.Center,
                fontSize = 14.sp
            )
        }
    }
}
