package com.faysal.bcsquiz.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.faysal.bcsquiz.data.model.*
import com.faysal.bcsquiz.data.repository.QuizRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    repository: QuizRepository,
    onBack: () -> Unit
) {
    var user by remember { mutableStateOf<User?>(null) }
    var activities by remember { mutableStateOf<List<AdminActivity>>(emptyList()) }
    var userRequests by remember { mutableStateOf<List<Any>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    LaunchedEffect(uid) {
        user = repository.getUser(uid)
        if (user?.role == "admin") {
            activities = repository.getAdminHistory(uid, isOwner = false)
        } else {
            val db = FirebaseFirestore.getInstance()
            // Observe changes in reviews, topups, withdrawals for the user
            db.collection("reviews").whereEqualTo("uid", uid).addSnapshotListener { s, _ ->
                val reviews = s?.toObjects(ReviewRequest::class.java) ?: emptyList()
                db.collection("topups").whereEqualTo("uid", uid).addSnapshotListener { s2, _ ->
                    val topups = s2?.toObjects(TopupRequest::class.java) ?: emptyList()
                    db.collection("withdrawals").whereEqualTo("uid", uid).addSnapshotListener { s3, _ ->
                        val withdrawals = s3?.toObjects(WithdrawalRequest::class.java) ?: emptyList()
                        userRequests = (reviews + topups + withdrawals).sortedByDescending { 
                            when(it) {
                                is ReviewRequest -> it.timestamp
                                is TopupRequest -> it.timestamp
                                is WithdrawalRequest -> it.timestamp
                                else -> com.google.firebase.Timestamp(0, 0)
                            }
                        }
                    }
                }
            }
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile & History") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            user?.let { u ->
                LazyColumn(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = u.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                                Text(text = u.email, color = Color.Gray)
                                Spacer(modifier = Modifier.height(12.dp))
                                Badge(containerColor = when(u.role) {
                                    "owner" -> Color.Red
                                    "admin" -> Color(0xFF6200EE)
                                    else -> Color(0xFFFFD700)
                                }) {
                                    Text(u.role.uppercase(), modifier = Modifier.padding(4.dp), color = Color.White)
                                }
                            }
                        }
                        
                        if (u.role == "user") {
                            UserDashboardSection(u)
                        } else if (u.role == "admin") {
                            AdminActivitySection(activities)
                        }
                    }
                    
                    if (u.role == "user") {
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            Text("My Support Requests", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        items(userRequests) { req ->
                            UserRequestItem(req)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserDashboardSection(u: User) {
    val context = LocalContext.current
    Spacer(modifier = Modifier.height(16.dp))
    val refCode = u.uid.take(8).uppercase()
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Referral Code", style = MaterialTheme.typography.labelSmall)
                Text(refCode, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Button(onClick = {
                val sendIntent = android.content.Intent().apply {
                    action = android.content.Intent.ACTION_SEND
                    putExtra(android.content.Intent.EXTRA_TEXT, "Join BCS Quiz using code $refCode and win rewards!")
                    type = "text/plain"
                }
                context.startActivity(android.content.Intent.createChooser(sendIntent, null))
            }) { Text("Share") }
        }
    }
    
    Spacer(modifier = Modifier.height(24.dp))
    Text("Topic Performance", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(8.dp))
    u.topicTotalAnswers.keys.forEach { topicId ->
        val total = u.topicTotalAnswers[topicId] ?: 0
        val correct = u.topicCorrectAnswers[topicId] ?: 0
        val percentage = if (total > 0) (correct * 100 / total) else 0
        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(topicId.replaceFirstChar { it.uppercase() }, modifier = Modifier.weight(1f))
                Text("$percentage%", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun UserRequestItem(req: Any) {
    val (title, status, note, timestamp) = when(req) {
        is ReviewRequest -> Quadruple("Question Report", req.status, req.adminNote, req.timestamp)
        is TopupRequest -> Quadruple("Topup Request", req.status, req.adminNote, req.timestamp)
        is WithdrawalRequest -> Quadruple("Withdrawal Request", req.status, req.adminNote, req.timestamp)
        else -> Quadruple("Unknown", "", "", null)
    }

    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(
                    status.uppercase(), 
                    color = when(status) {
                        "approved", "accepted" -> Color(0xFF4CAF50)
                        "rejected" -> Color.Red
                        else -> Color.Gray
                    },
                    style = MaterialTheme.typography.labelSmall
                )
            }
            if (note.isNotEmpty()) {
                Text("Admin Note: $note", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(top = 4.dp))
            }
            Text("Date: ${timestamp?.toDate()?.toLocaleString() ?: "N/A"}", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

data class Quadruple<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

@Composable
fun AdminActivitySection(activities: List<AdminActivity>) {
    Spacer(modifier = Modifier.height(24.dp))
    Text("Task History", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(12.dp))
    if (activities.isEmpty()) {
        Text("No tasks found.", color = Color.Gray)
    } else {
        activities.forEach { activity ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(activity.description, fontWeight = FontWeight.Bold)
                    Text("Time: ${activity.timestamp?.toDate()?.toLocaleString()}", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
