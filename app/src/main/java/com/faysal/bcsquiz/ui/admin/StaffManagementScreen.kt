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
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffManagementScreen(onBack: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    var admins by remember { mutableStateOf<List<User>>(emptyList()) }
    var selectedAdminLogs by remember { mutableStateOf<List<AdminActivity>>(emptyList()) }
    var selectedAdminName by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        db.collection("users").whereEqualTo("role", "admin").get().await().let { admins = it.toObjects(User::class.java) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Staff Tracking") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text("Active Admins", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                items(admins) { admin ->
                    ListItem(
                        headlineContent = { Text(admin.name) },
                        supportingContent = { Text(admin.email) },
                        trailingContent = {
                            Row {
                                IconButton(onClick = {
                                    scope.launch {
                                        selectedAdminName = admin.name
                                        // Safe fetch: get all and sort on client to avoid index crash
                                        val snapshot = db.collection("admin_activities")
                                            .whereEqualTo("adminUid", admin.uid)
                                            .get().await()
                                        selectedAdminLogs = snapshot.toObjects(AdminActivity::class.java).sortedByDescending { it.timestamp }
                                    }
                                }) { Icon(Icons.Default.History, null) }
                                IconButton(onClick = { 
                                    scope.launch { db.collection("users").document(admin.uid).update("role", "user").await() }
                                }) { Icon(Icons.Default.PersonRemove, null, tint = Color.Red) }
                            }
                        }
                    )
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            
            if (selectedAdminName.isNotEmpty()) {
                Text("Activities of $selectedAdminName:", fontWeight = FontWeight.Bold)
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (selectedAdminLogs.isEmpty()) {
                        item { Text("No actions found.", color = Color.Gray) }
                    }
                    items(selectedAdminLogs) { log ->
                        ListItem(
                            headlineContent = { Text(log.description) },
                            supportingContent = { Text("Time: ${log.timestamp?.toDate()?.toLocaleString()}") }
                        )
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text("Select an admin history icon to view details", color = Color.Gray)
                }
            }
        }
    }
}
