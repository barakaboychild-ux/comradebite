package com.comradebite.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.comradebite.R
import com.comradebite.ui.theme.CyanAccent
import com.comradebite.viewmodel.MealViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MealViewModel) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf("Dashboard") }
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    
    val auth = remember { FirebaseAuth.getInstance() }
    var isLoggedIn by remember { mutableStateOf(auth.currentUser != null) }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener {
            isLoggedIn = it.currentUser != null
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    val backgroundIndex by viewModel.currentBackground.collectAsState(initial = 0)
    val backgroundRes = when (backgroundIndex) {
        0 -> R.drawable.bg_1
        1 -> R.drawable.bg_2
        else -> R.drawable.bg_3
    }

    val menuItems = listOf(
        "Home Dashboard" to "Dashboard",
        "Weekly Timetable" to "Timetable",
        "Meal Inventory" to "Inventory",
        "Combo Builder" to "Combos",
        "Smart Decisions" to "Decisions",
        "Grocery Assistant" to "Grocery",
        "Join/Switch Group" to "JoinGroup",
        "App Guide" to "Guide",
        (if (isLoggedIn) "Admin Profile" else "Admin Login") to "Login"
    )

    var showJoinGroupDialog by remember { mutableStateOf(false) }
    var joinCodeText by remember { mutableStateOf("") }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxHeight().width(300.dp),
                drawerContainerColor = if (isDarkTheme) Color(0xFF1E293B).copy(alpha = 0.95f) else Color.White.copy(alpha = 0.95f)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Spacer(Modifier.height(32.dp))
                    Text(
                        text = buildAnnotatedString {
                            withStyle(style = SpanStyle(fontWeight = FontWeight.ExtraBold, color = if (isDarkTheme) Color.White else Color.Black)) {
                                append("Comrade")
                            }
                            withStyle(style = SpanStyle(fontWeight = FontWeight.ExtraBold, color = CyanAccent)) {
                                append("Bite")
                            }
                        },
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp),
                        style = MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp)
                    )
                    
                    Spacer(Modifier.height(24.dp))
                    
                    menuItems.forEach { (label, screen) ->
                        NavigationDrawerItem(
                            label = { 
                                Text(
                                    text = label, 
                                    fontWeight = if (currentScreen == screen) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 18.sp
                                ) 
                            },
                            icon = {
                                when(screen) {
                                    "Login" -> Icon(Icons.Default.Lock, contentDescription = null)
                                    "Guide" -> Icon(Icons.Default.Info, contentDescription = null)
                                    "Dashboard" -> Icon(Icons.Default.Home, contentDescription = null)
                                }
                            },
                            selected = currentScreen == screen,
                            onClick = { 
                                if (screen == "JoinGroup") {
                                    showJoinGroupDialog = true
                                } else {
                                    currentScreen = screen
                                }
                                scope.launch { drawerState.close() } 
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = CyanAccent,
                                selectedTextColor = Color.Black,
                                unselectedTextColor = if (screen == "Login" || screen == "Guide") CyanAccent else (if (isDarkTheme) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.7f)),
                                unselectedContainerColor = Color.Transparent,
                                unselectedIconColor = if (screen == "Login" || screen == "Guide") CyanAccent else (if (isDarkTheme) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.7f))
                            ),
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                .height(56.dp)
                        )
                    }

                    NavigationDrawerItem(
                        label = { Text("Share with Friends", fontSize = 18.sp) },
                        icon = { Icon(Icons.Default.Share, contentDescription = null) },
                        selected = false,
                        onClick = {
                            val sendIntent: Intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, "Hey! Check out ComradeBite to manage your meals perfectly: https://comradebite.vercel.app/")
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, null)
                            context.startActivity(shareIntent)
                            scope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedTextColor = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.7f),
                            unselectedIconColor = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.7f)
                        ),
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .height(56.dp)
                    )

                    Spacer(Modifier.weight(1f))
                    
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 20.dp), 
                        color = if (isDarkTheme) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.1f)
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "Dark", 
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isDarkTheme) Color.White else Color.Gray
                        )
                        Switch(
                            checked = !isDarkTheme,
                            onCheckedChange = { viewModel.toggleTheme() },
                            modifier = Modifier.padding(horizontal = 12.dp),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = CyanAccent,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color.Gray
                            )
                        )
                        Text(
                            "Light", 
                            style = MaterialTheme.typography.labelLarge,
                            color = if (!isDarkTheme) Color.Black else Color.Gray
                        )
                    }
                }
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = backgroundRes,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = if (isDarkTheme) {
                                listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent, Color.Black.copy(alpha = 0.8f))
                            } else {
                                listOf(Color.White.copy(alpha = 0.7f), Color.Transparent, Color.White.copy(alpha = 0.7f))
                            }
                        )
                    )
            )

            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    CenterAlignedTopAppBar(
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = Color.Transparent,
                            titleContentColor = if (isDarkTheme) Color.White else Color.Black
                        ),
                        title = {
                            if (currentScreen == "Dashboard") {
                                Text(
                                    text = buildAnnotatedString {
                                        append("Comrade")
                                        withStyle(style = SpanStyle(color = CyanAccent)) {
                                            append("Bite")
                                        }
                                    },
                                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                )
                            } else {
                                val title = menuItems.find { it.second == currentScreen }?.first ?: if (currentScreen == "Login") "Admin Access" else currentScreen
                                Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(
                                    Icons.Default.Menu, 
                                    contentDescription = "Menu", 
                                    tint = if (isDarkTheme) Color.White else Color.Black
                                )
                            }
                        }
                    )
                }
            ) { padding ->
                Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                    when (currentScreen) {
                        "Dashboard" -> DashboardScreen(viewModel)
                        "Inventory" -> InventoryScreen(viewModel)
                        "Combos" -> ComboBuilderScreen(viewModel)
                        "Decisions" -> DecisionScreen(viewModel)
                        "Grocery" -> GroceryAssistantScreen(viewModel)
                        "Timetable" -> TimetableScreen(viewModel)
                        "Guide" -> UserGuideScreen(isDarkTheme)
                        "Login" -> LoginScreen(viewModel, onLoginSuccess = {
                            currentScreen = "Dashboard"
                        })
                    }

                    if (showJoinGroupDialog) {
                        AlertDialog(
                            onDismissRequest = { showJoinGroupDialog = false },
                            title = { Text("Join Group") },
                            text = {
                                Column {
                                    Text("Enter the 6-digit code to join an existing group.")
                                    Spacer(Modifier.height(16.dp))
                                    OutlinedTextField(
                                        value = joinCodeText,
                                        onValueChange = { if (it.length <= 6) joinCodeText = it.uppercase() },
                                        label = { Text("Group Code") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = CyanAccent,
                                            focusedLabelColor = CyanAccent
                                        )
                                    )
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        if (joinCodeText.length == 6) {
                                            viewModel.updateGroupCode(joinCodeText)
                                            showJoinGroupDialog = false
                                            joinCodeText = ""
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black)
                                ) {
                                    Text("Join")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showJoinGroupDialog = false }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
