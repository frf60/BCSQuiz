package com.faysal.bcsquiz.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.faysal.bcsquiz.data.model.*
import com.faysal.bcsquiz.data.repository.QuizRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceManagementScreen(repository: QuizRepository, onBack: () -> Unit) {
    var selectedTab by remember { mutableStateOf(0) }
    val db = FirebaseFirestore.getInstance()
    val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
    var userProfile by remember { mutableStateOf<User?>(null) }

    LaunchedEffect(Unit) {
        userProfile = db.collection("users").document(uid).get().await().toObject(User::class.java)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Finance Management") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0 }, icon = { Icon(Icons.Default.AccountBalance, null) }, label = { Text("Topups") })
                NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1 }, icon = { Icon(Icons.Default.Payments, null) }, label = { Text("Withdrawals") })
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            if (selectedTab == 0) TopupTab(repository, userProfile?.name ?: "Owner", uid)
            else WithdrawalTab(repository, userProfile?.name ?: "Owner", uid)
        }
    }
}

@Composable
fun TopupTab(repository: QuizRepository, adminName: String, adminUid: String) {
    val db = FirebaseFirestore.getInstance()
    var requests by remember { mutableStateOf<List<TopupRequest>>(emptyList()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        db.collection("topups").whereEqualTo("status", "pending").addSnapshotListener { s, _ ->
            requests = s?.toObjects(TopupRequest::class.java) ?: emptyList()
        }
    }

    LazyColumn {
        items(requests) { r ->
            var note by remember { mutableStateOf("") }
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("User ID: ${r.uid}")
                    Text("Amount: ৳${r.amount}", fontWeight = FontWeight.Bold)
                    Text("TrxID: ${r.transactionId}")
                    TextField(value = note, onValueChange = { note = it }, label = { Text("Admin Note") }, modifier = Modifier.fillMaxWidth())
                    Row(modifier = Modifier.padding(top = 8.dp)) {
                        Button(onClick = { scope.launch { repository.handleTopup(r.id, adminUid, adminName, "approved", note) } }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) { Text("Approve") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = { scope.launch { repository.handleTopup(r.id, adminUid, adminName, "rejected", note) } }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Reject") }
                    }
                }
            }
        }
    }
}

@Composable
fun WithdrawalTab(repository: QuizRepository, adminName: String, adminUid: String) {
    val db = FirebaseFirestore.getInstance()
    var requests by remember { mutableStateOf<List<WithdrawalRequest>>(emptyList()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        db.collection("withdrawals").whereEqualTo("status", "pending").addSnapshotListener { s, _ ->
            requests = s?.toObjects(WithdrawalRequest::class.java) ?: emptyList()
        }
    }

    LazyColumn {
        items(requests) { r ->
            var note by remember { mutableStateOf("") }
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("User ID: ${r.uid}")
                    Text("Amount: ৳${r.amount}", fontWeight = FontWeight.Bold)
                    Text("${r.method}: ${r.phoneNumber}")
                    TextField(value = note, onValueChange = { note = it }, label = { Text("Admin Note") }, modifier = Modifier.fillMaxWidth())
                    Row(modifier = Modifier.padding(top = 8.dp)) {
                        Button(onClick = { scope.launch { repository.handleWithdrawal(r.id, adminUid, adminName, "approved", note) } }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))) { Text("Paid") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = { scope.launch { repository.handleWithdrawal(r.id, adminUid, adminName, "rejected", note) } }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Reject") }
                    }
                }
            }
        }
    }
}
