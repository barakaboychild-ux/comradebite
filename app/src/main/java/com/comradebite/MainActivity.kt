package com.comradebite

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.comradebite.data.MealDatabase
import com.comradebite.data.MealRepository
import com.comradebite.notifications.NotificationHelper
import com.comradebite.ui.theme.ComradeBiteTheme
import com.comradebite.ui.screens.MainScreen
import com.comradebite.viewmodel.MealViewModel
import com.comradebite.viewmodel.ViewModelFactory

class MainActivity : ComponentActivity() {
    private val database by lazy { MealDatabase.getDatabase(this) }
    private val repository by lazy { MealRepository(database.mealDao()) }
    private val viewModel: MealViewModel by viewModels {
        val prefs = getSharedPreferences("comrade_bite_prefs", Context.MODE_PRIVATE)
        ViewModelFactory(repository, prefs)
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            NotificationHelper.scheduleMealReminders(this)
        } else {
            Toast.makeText(this, "Notifications disabled. You won't get meal reminders.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        NotificationHelper.createNotificationChannel(this)
        checkNotificationPermission()

        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()
            
            ComradeBiteTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(viewModel)
                }
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                NotificationHelper.scheduleMealReminders(this)
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            NotificationHelper.scheduleMealReminders(this)
        }
    }
}
