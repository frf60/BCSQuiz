package com.faysal.bcsquiz.ui.room

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import com.faysal.bcsquiz.data.model.Room
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomScreen(
    viewModel: RoomViewModel,
    quizRepository: com.faysal.bcsquiz.data.repository.QuizRepository, // Need this to fetch categories
    onStartGame: (String, List<String>) -> Unit
) {
    val room by viewModel.room.collectAsState()
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    
    var roomCodeInput by remember { mutableStateOf("") }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Topic Selection State
    var categories by remember { mutableStateOf<List<com.faysal.bcsquiz.data.model.Category>>(emptyList()) }
    var selectedCategories by remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(Unit) {
        categories = quizRepository.getCategories()
    }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            snackbarMessage = null
        }
    }

    LaunchedEffect(room?.status) {
        if (room?.status == "playing") {
            onStartGame(room!!.id, room!!.questionIds)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { TopAppBar(title = { Text("Secret Room") }) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (room == null) {
                // Initial State: Create or Join
                Text("Start a private contest with friends.", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(24.dp))

                Text("Select 2 Topics for the room:", fontWeight = FontWeight.Bold)
                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                    items(categories) { category ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = selectedCategories.contains(category.id),
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        if (selectedCategories.size < 2) selectedCategories = selectedCategories + category.id
                                    } else {
                                        selectedCategories = selectedCategories - category.id
                                    }
                                }
                            )
                            Text(category.name)
                        }
                    }
                }

                Button(
                    onClick = { 
                        if (selectedCategories.size == 2) {
                            viewModel.createRoom(selectedCategories.toList()) { }
                        } else {
                            snackbarMessage = "Please select exactly 2 topics"
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = selectedCategories.size == 2
                ) {
                    Text("Create New Room (20 pts)")
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("OR", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(16.dp))

                TextField(
                    value = roomCodeInput,
                    onValueChange = { roomCodeInput = it },
                    label = { Text("Enter Room Code") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { 
                        if (roomCodeInput.isNotEmpty()) {
                            viewModel.joinRoom(roomCodeInput) { success, msg ->
                                if (!success) snackbarMessage = msg
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("Join Friend's Room")
                }
            } else {
                // Lobby State
                val clipboardManager = LocalClipboardManager.current
                val context = LocalContext.current
                
                Text("Room Code", style = MaterialTheme.typography.labelSmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        room!!.id,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = { 
                        clipboardManager.setText(AnnotatedString(room!!.id))
                        snackbarMessage = "Code copied to clipboard"
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                    }
                    IconButton(onClick = {
                        val sendIntent = android.content.Intent().apply {
                            action = android.content.Intent.ACTION_SEND
                            putExtra(android.content.Intent.EXTRA_TEXT, "Join my Secret Room on BCS Quiz! Code: ${room!!.id}")
                            type = "text/plain"
                        }
                        context.startActivity(android.content.Intent.createChooser(sendIntent, null))
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Players (${room!!.players.size}/5)", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        room!!.players.forEach { playerUid ->
                            Text("- User: ${playerUid.take(8)}...", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                if (room!!.creatorUid == uid) {
                    Button(
                        onClick = { viewModel.startGame() },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        enabled = room!!.players.size >= 2 // Min 2 players to start
                    ) {
                        Text("Start Contest Now")
                    }
                } else {
                    Text("Waiting for creator to start...", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                }
            }
        }
    }
}
