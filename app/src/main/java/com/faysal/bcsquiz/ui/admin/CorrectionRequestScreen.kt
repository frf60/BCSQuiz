package com.faysal.bcsquiz.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.faysal.bcsquiz.data.model.*
import com.faysal.bcsquiz.data.repository.QuizRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CorrectionRequestScreen(repository: QuizRepository, onCorrection: (String) -> Unit, onBack: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    var reviews by remember { mutableStateOf<List<ReviewRequest>>(emptyList()) }
    val scope = rememberCoroutineScope()
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    var adminProfile by remember { mutableStateOf<User?>(null) }

    LaunchedEffect(Unit) {
        adminProfile = repository.getUser(uid)
        db.collection("reviews").whereEqualTo("status", "pending").addSnapshotListener { s, _ ->
            reviews = s?.toObjects(ReviewRequest::class.java) ?: emptyList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Correction Requests") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).padding(16.dp)) {
            items(reviews) { r ->
                var adminNote by remember { mutableStateOf("") }
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Question: ${r.questionText}", fontWeight = FontWeight.Bold)
                        Text("User Report: ${r.userNote}", style = MaterialTheme.typography.bodySmall)
                        
                        TextField(
                            value = adminNote,
                            onValueChange = { adminNote = it },
                            label = { Text("Admin Feedback") },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        )

                        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                            Button(onClick = { 
                                scope.launch { 
                                    db.collection("reviews").document(r.id).update(
                                        "status", "accepted", 
                                        "handledBy", adminProfile?.name ?: "Admin", 
                                        "adminNote", adminNote
                                    ).await()
                                    // Log
                                    val logId = db.collection("admin_activities").document().id
                                    db.collection("admin_activities").document(logId).set(AdminActivity(
                                        logId, uid, adminProfile?.name ?: "Admin", "accept_review", r.id, 
                                        "Accepted & Fixing: ${r.questionText.take(20)}...", com.google.firebase.Timestamp.now()
                                    )).await()
                                    onCorrection(r.questionId) 
                                }
                            }, colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)) {
                                Text("Correct & Close")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = { 
                                scope.launch { 
                                    db.collection("reviews").document(r.id).update(
                                        "status", "rejected", 
                                        "handledBy", adminProfile?.name ?: "Admin", 
                                        "adminNote", adminNote
                                    ).await()
                                    // Log
                                    val logId = db.collection("admin_activities").document().id
                                    db.collection("admin_activities").document(logId).set(AdminActivity(
                                        logId, uid, adminProfile?.name ?: "Admin", "reject_review", r.id, 
                                        "Rejected correction request", com.google.firebase.Timestamp.now()
                                    )).await()
                                }
                            }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) {
                                Text("Reject")
                            }
                        }
                    }
                }
            }
        }
    }
}
