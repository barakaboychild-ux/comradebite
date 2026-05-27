package com.comradebite.ui.screens

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.core.text.HtmlCompat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MealViewModel) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var currentScreen by remember { mutableStateOf("Dashboard") }
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val userName by viewModel.userName.collectAsState()
    
    val auth = remember { FirebaseAuth.getInstance() }
    var isLoggedIn by remember { mutableStateOf(auth.currentUser != null) }

    var showJoinGroupDialog by remember { mutableStateOf(false) }
    var joinCodeText by remember { mutableStateOf("") }
    
    var showChat by remember { mutableStateOf(false) }

    BackHandler(enabled = currentScreen != "Dashboard") {
        currentScreen = "Dashboard"
    }

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

    Box(modifier = Modifier.fillMaxSize()) {
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
                                withStyle(style = SpanStyle(fontWeight = FontWeight.ExtraBold, color = if (isDarkTheme) Color.White else Color.Black)) { append("Comrade") }
                                withStyle(style = SpanStyle(fontWeight = FontWeight.ExtraBold, color = CyanAccent)) { append("Bite") }
                            },
                            modifier = Modifier.padding(horizontal = 28.dp),
                            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp)
                        )
                        
                        Text(
                            text = if (isLoggedIn) "Welcome, $userName" else "Welcome, Comrade",
                            modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = CyanAccent,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        )
                        
                        Spacer(Modifier.height(16.dp))
                        
                        val menuItems = listOf(
                            "Home Dashboard" to "Dashboard",
                            "Weekly Timetable" to "Timetable",
                            "Meal Inventory" to "Inventory",
                            "Combo Builder" to "Combos",
                            "Smart Decisions" to "Decisions",
                            "Grocery Assistant" to "Grocery",
                            "Join/Switch Group" to "JoinGroup",
                            "App Guide" to "Guide",
                            "Share with Comrades" to "Share",
                            (if (isLoggedIn) "Admin Profile" else "Admin Login") to "Login"
                        )

                        menuItems.forEach { (label, screen) ->
                            NavigationDrawerItem(
                                label = { Text(text = label, fontWeight = if (currentScreen == screen) FontWeight.Bold else FontWeight.Medium, fontSize = 18.sp) },
                                icon = {
                                    val icon = when(screen) {
                                        "Login" -> Icons.Default.Lock
                                        "Guide" -> Icons.Default.Info
                                        "Dashboard" -> Icons.Default.Home
                                        "Inventory" -> Icons.AutoMirrored.Filled.List
                                        "Combos" -> Icons.Default.Build
                                        "Decisions" -> Icons.Default.Face
                                        "Grocery" -> Icons.Default.ShoppingCart
                                        "JoinGroup" -> Icons.Default.Person
                                        "Share" -> Icons.Default.Share
                                        else -> Icons.Default.Menu
                                    }
                                    Icon(icon, contentDescription = null)
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
                                    unselectedTextColor = if (screen == "Login" || screen == "Guide" || screen == "Share") CyanAccent else (if (isDarkTheme) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.7f)),
                                    unselectedContainerColor = Color.Transparent,
                                    unselectedIconColor = if (screen == "Login" || screen == "Guide" || screen == "Share") CyanAccent else (if (isDarkTheme) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.7f))
                                ),
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                                    .height(56.dp)
                            )
                        }
                        
                        Spacer(Modifier.weight(1f))
                        
                        // Theme Switch and Credits
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                Text("Dark", color = if (isDarkTheme) Color.White else Color.Gray)
                                Switch(checked = !isDarkTheme, onCheckedChange = { viewModel.toggleTheme() }, modifier = Modifier.padding(horizontal = 12.dp))
                                Text("Light", color = if (!isDarkTheme) Color.Black else Color.Gray)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Designed By BarakaBoychild",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyanAccent.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Global background color - Plain background
                Box(modifier = Modifier.fillMaxSize().background(if(isDarkTheme) Color(0xFF080C14) else Color(0xFFF1F5F9)))

                Scaffold(
                    containerColor = Color.Transparent,
                    topBar = {
                        CenterAlignedTopAppBar(
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
                            title = {
                                Text(
                                    text = if (currentScreen == "Dashboard") "ComradeBite" else currentScreen,
                                    color = if (isDarkTheme) Color.White else Color.Black,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            navigationIcon = {
                                IconButton(
                                    onClick = { 
                                        scope.launch { drawerState.open() }
                                    }
                                ) {
                                    Icon(Icons.Default.Menu, "Menu", tint = if (isDarkTheme) Color.White else Color.Black)
                                }
                            }
                        )
                    },
                    floatingActionButton = {
                        if (currentScreen == "Dashboard") {
                            FloatingActionButton(
                                onClick = { showChat = !showChat },
                                containerColor = CyanAccent,
                                contentColor = Color.Black,
                                shape = CircleShape
                            ) {
                                Icon(if (showChat) Icons.Default.Close else Icons.Default.Face, contentDescription = "Chef AI")
                            }
                        }
                    }
                ) { padding ->
                    Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                        when (currentScreen) {
                            "Dashboard" -> DashboardScreen(viewModel, backgroundRes)
                            "Inventory" -> InventoryScreen(viewModel = viewModel)
                            "Timetable" -> TimetableScreen(viewModel)
                            "Combos" -> ComboBuilderScreen(viewModel)
                            "Decisions" -> DecisionScreen(viewModel)
                            "Grocery" -> GroceryAssistantScreen(viewModel)
                            "Guide" -> UserGuideScreen(isDarkTheme)
                            "Share" -> ShareAppScreen(isDarkTheme)
                            "Login" -> LoginScreen(viewModel, onLoginSuccess = { currentScreen = "Dashboard" })
                        }

                        if (showJoinGroupDialog) {
                            AlertDialog(
                                onDismissRequest = { showJoinGroupDialog = false },
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
                                    ) { Text("Join") }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showJoinGroupDialog = false }) { Text("Cancel") }
                                },
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
                                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyanAccent, focusedLabelColor = CyanAccent)
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
        
        // Chef AI Chat Overlay
        if (showChat && currentScreen == "Dashboard") {
            ChefAiChatOverlay(
                viewModel = viewModel,
                isDarkTheme = isDarkTheme,
                onClose = { showChat = false }
            )
        }
    }
}

@Composable
fun ChefAiChatOverlay(viewModel: MealViewModel, isDarkTheme: Boolean, onClose: () -> Unit) {
    val messages by viewModel.chatMessages.collectAsState()
    var text by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)).clickable { onClose() }) {
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.6f)
                .padding(16.dp)
                .clickable(enabled = false) { },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
            colors = CardDefaults.cardColors(containerColor = if (isDarkTheme) Color(0xFF1E293B) else Color.White)
        ) {
            Column(Modifier.fillMaxSize()) {
                // Top Bar
                Row(
                    Modifier.fillMaxWidth().background(CyanAccent.copy(alpha = 0.1f)).padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = CyanAccent, modifier = Modifier.size(32.dp)) {
                            Box(contentAlignment = Alignment.Center) { Text("👨‍🍳", fontSize = 18.sp) }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Chef AI", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Your personal kitchen comrade", fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                    IconButton(onClick = onClose) { Icon(Icons.Default.Close, null) }
                }
                
                // Messages
                LazyColumn(modifier = Modifier.weight(1f).padding(16.dp), state = listState, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(messages) { msg ->
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = if (msg.isUser) Alignment.CenterEnd else Alignment.CenterStart) {
                            Surface(
                                color = if (msg.isUser) CyanAccent else (if (isDarkTheme) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.widthIn(max = 260.dp)
                            ) {
                                Text(
                                    text = HtmlCompat.fromHtml(msg.text, HtmlCompat.FROM_HTML_MODE_LEGACY).toString(),
                                    modifier = Modifier.padding(12.dp),
                                    fontSize = 13.sp,
                                    color = if (msg.isUser) Color.Black else (if (isDarkTheme) Color.White else Color.Black)
                                )
                            }
                        }
                    }
                }
                
                // Quick Chips
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val quicks = listOf("Balance?", "Calories?", "Protein?")
                    quicks.forEach { q ->
                        SuggestionChip(onClick = { viewModel.sendChatMessage(q) }, label = { Text(q, fontSize = 10.sp) })
                    }
                }

                // Input
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        placeholder = { Text("Ask Chef AI...") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyanAccent)
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = { if (text.isNotBlank()) { viewModel.sendChatMessage(text); text = "" } },
                        enabled = text.isNotBlank(),
                        modifier = Modifier.background(CyanAccent, CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color.Black)
                    }
                }
            }
        }
    }
}
