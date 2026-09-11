package com.faysal.bcsquiz.ui.wallet

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.faysal.bcsquiz.data.model.Transaction
import com.faysal.bcsquiz.data.model.User
import com.faysal.bcsquiz.data.model.WithdrawalRequest
import com.faysal.bcsquiz.data.repository.QuizRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    repository: QuizRepository,
    onNavigateToSubscription: () -> Unit,
    onBack: () -> Unit
) {
    var user by remember { mutableStateOf<User?>(null) }
    var transactions by remember { mutableStateOf<List<Transaction>>(emptyList()) }
    var selectedTab by remember { mutableStateOf("daily") }
    var revenueAmount by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    fun loadData() {
        scope.launch {
            user = repository.getUser(uid)
            if (user?.role == "owner") {
                revenueAmount = repository.getRevenue(selectedTab)
                transactions = repository.getTransactions("") 
            } else {
                transactions = repository.getTransactions(uid)
            }
            isLoading = false
        }
    }

    LaunchedEffect(selectedTab) {
        loadData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (user?.role == "owner") "Revenue Dashboard" else "My Wallet") },
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
            Column(modifier = Modifier.padding(padding)) {
                if (user?.role == "owner") {
                    TabRow(selectedTabIndex = when(selectedTab) { "daily" -> 0; "weekly" -> 1; else -> 2 }) {
                        Tab(selected = selectedTab == "daily", onClick = { selectedTab = "daily" }) { Text("Daily", modifier = Modifier.padding(12.dp)) }
                        Tab(selected = selectedTab == "weekly", onClick = { selectedTab = "weekly" }) { Text("Weekly", modifier = Modifier.padding(12.dp)) }
                        Tab(selected = selectedTab == "monthly", onClick = { selectedTab = "monthly" }) { Text("Monthly", modifier = Modifier.padding(12.dp)) }
                    }
                    
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${selectedTab.replaceFirstChar { it.uppercase() }} Revenue", style = MaterialTheme.typography.labelMedium)
                            Text("৳ $revenueAmount", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    BalanceCard(user?.earnedBalance ?: 0)
                    Spacer(modifier = Modifier.height(8.dp))
                    UserWalletActions(user, repository, uid, onNavigateToSubscription = onNavigateToSubscription, onRefresh = { loadData() })
                }

                Text("History", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                
                if (user?.role != "owner" && user?.role != "admin") {
                    QuickTopupCard(uid, repository, onRefresh = { loadData() })
                }

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(transactions) { transaction ->
                        TransactionItem(transaction)
                    }
                }
            }
        }
    }
}

@Composable
fun QuickTopupCard(uid: String, repository: QuizRepository, onRefresh: () -> Unit) {
    var showTopupDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9)),
        border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF4CAF50))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Quick Topup Instructions", fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20), fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Step 1: Send Money to 01622505105 (Bkash/Nagad)\nStep 2: Collect Transaction ID & Amount\nStep 3: Submit the proof below.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Black,
                lineHeight = 20.sp
            )
            Button(
                onClick = { showTopupDialog = true },
                modifier = Modifier.padding(top = 16.dp).fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
            ) { Text("Submit Topup Proof", color = Color.White, fontWeight = FontWeight.Bold) }
        }
    }

    if (showTopupDialog) {
        var amount by remember { mutableStateOf("") }
        var trxId by remember { mutableStateOf("") }
        var note by remember { mutableStateOf("") }
        
        AlertDialog(
            onDismissRequest = { showTopupDialog = false },
            title = { Text("Topup Request") },
            text = {
                Column {
                    TextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount (TK)") })
                    TextField(value = trxId, onValueChange = { trxId = it }, label = { Text("Transaction ID") }, modifier = Modifier.padding(top = 8.dp))
                    TextField(value = note, onValueChange = { note = it }, label = { Text("Note (Optional)") }, modifier = Modifier.padding(top = 8.dp))
                }
            },
            confirmButton = {
                Button(onClick = {
                    val request = com.faysal.bcsquiz.data.model.TopupRequest(
                        uid = uid,
                        amount = amount.toIntOrNull() ?: 0,
                        transactionId = trxId,
                        note = note
                    )
                    scope.launch {
                        repository.submitTopup(request)
                        showTopupDialog = false
                        onRefresh()
                    }
                }) { Text("Submit Request") }
            }
        )
    }
}

@Composable
fun UserWalletActions(user: User?, repository: QuizRepository, uid: String, onNavigateToSubscription: () -> Unit, onRefresh: () -> Unit) {
    val scope = rememberCoroutineScope()
    var showExchangeDialog by remember { mutableStateOf(false) }
    var showWithdrawDialog by remember { mutableStateOf(false) }

    Row(modifier = Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Button(
            onClick = { showWithdrawDialog = true }, 
            modifier = Modifier.weight(1f).heightIn(min = 80.dp), 
            enabled = (user?.earnedBalance ?: 0) >= 500,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Withdraw", fontWeight = FontWeight.Bold)
                Text("(Minimum 500 TK)", style = MaterialTheme.typography.labelSmall)
            }
        }
        OutlinedButton(
            onClick = { 
                if ((user?.earnedBalance ?: 0) < 10) {
                    onNavigateToSubscription()
                } else {
                    showExchangeDialog = true 
                }
            }, 
            modifier = Modifier.weight(1f).heightIn(min = 80.dp)
        ) {
            Text("Buy Points", textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontWeight = FontWeight.Bold)
        }
    }

    if (showExchangeDialog) {
        AlertDialog(
            onDismissRequest = { showExchangeDialog = false },
            title = { Text("TK to Points") },
            text = { Text("Convert 10 TK into 100 Points?") },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        repository.exchangeBalance(uid, 10)
                        showExchangeDialog = false
                        onRefresh()
                    }
                }) { Text("Exchange") }
            },
            dismissButton = { TextButton(onClick = { showExchangeDialog = false }) { Text("Cancel") } }
        )
    }

    if (showWithdrawDialog) {
        var phoneNumber by remember { mutableStateOf("") }
        var withdrawAmount by remember { mutableStateOf("") }
        var method by remember { mutableStateOf("Bkash") }
        
        AlertDialog(
            onDismissRequest = { showWithdrawDialog = false },
            title = { Text("Withdraw Money") },
            text = {
                Column {
                    Text("Select Method:", fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = method == "Bkash", onClick = { method = "Bkash" })
                        Text("Bkash")
                        Spacer(modifier = Modifier.width(16.dp))
                        RadioButton(selected = method == "Nagad", onClick = { method = "Nagad" })
                        Text("Nagad")
                    }
                    TextField(
                        value = withdrawAmount, 
                        onValueChange = { withdrawAmount = it }, 
                        label = { Text("Amount to Withdraw") },
                        modifier = Modifier.padding(top = 8.dp),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                    TextField(
                        value = phoneNumber, 
                        onValueChange = { phoneNumber = it }, 
                        label = { Text("Bkash/Nagad Number") },
                        modifier = Modifier.padding(top = 8.dp),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val amount = withdrawAmount.toIntOrNull() ?: 0
                    if (amount >= 500 && amount <= (user?.earnedBalance ?: 0)) {
                        scope.launch {
                            val request = WithdrawalRequest(
                                uid = uid,
                                amount = amount,
                                method = method,
                                phoneNumber = phoneNumber
                            )
                            repository.submitWithdrawal(request)
                            showWithdrawDialog = false
                            onRefresh()
                        }
                    }
                }) { Text("Submit Request") }
            }
        )
    }
}

@Composable
fun BalanceCard(balance: Int) {
    Card(modifier = Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.AccountBalanceWallet, null, modifier = Modifier.size(48.dp), tint = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Available Balance", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
            Text("৳ $balance", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun TransactionItem(transaction: com.faysal.bcsquiz.data.model.Transaction) {
    ListItem(
        headlineContent = { Text(transaction.description) },
        supportingContent = { 
            Column {
                if (transaction.userEmail.isNotEmpty()) {
                    Text("User: ${transaction.userEmail}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
                if (transaction.adminNote.isNotEmpty()) {
                    Text("Admin Note: ${transaction.adminNote}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                }
                Text("at ${transaction.timestamp?.toDate()?.toLocaleString()}", style = MaterialTheme.typography.labelSmall)
            }
        },
        trailingContent = {
            val color = if (transaction.type == "reward" || transaction.type == "exchange" || transaction.type == "topup") Color(0xFF4CAF50) else Color.Red
            Text("${if (color == Color.Red) "-" else "+"}৳${transaction.amount}", color = color, fontWeight = FontWeight.Bold)
        }
    )
}
