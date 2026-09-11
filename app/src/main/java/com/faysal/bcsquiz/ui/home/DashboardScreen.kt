package com.faysal.bcsquiz.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: HomeViewModel,
    onNavigateToCategories: () -> Unit,
    onNavigateToContests: () -> Unit,
    onNavigateToLeaderboard: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSubscription: () -> Unit,
    onNavigateToWallet: () -> Unit,
    onNavigateToRoom: () -> Unit,
    onNavigateToAdmin: (Int) -> Unit,
    onNavigateToStaff: () -> Unit,
    onNavigateToFinance: () -> Unit,
    onNavigateToReviews: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onLogout: () -> Unit
) {
    val user by viewModel.user.collectAsState()
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.loadUserProfile() }
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { snackbarHostState.showSnackbar(it); snackbarMessage = null }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            val role = user?.role ?: "user"
            TopAppBar(
                title = { Text("BCS Quiz", fontWeight = FontWeight.Bold) },
                actions = {
                    if (role == "owner") {
                        TextButton(onClick = { com.faysal.bcsquiz.util.DatabaseSeeder.populateBcsDatabase { viewModel.loadUserProfile() } }) { Text("Seed DB") }
                    }
                    if (role != "owner") {
                        IconButton(onClick = onNavigateToProfile) { Icon(Icons.Default.AccountCircle, "Profile") }
                    }
                    IconButton(onClick = onLogout) { Icon(Icons.Default.ExitToApp, "Logout") }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            val role = user?.role ?: "user"
            val isAdmin = role == "admin"
            val isOwner = role == "owner"
            val isStaff = isOwner || isAdmin

            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        val greeting = when {
                            isOwner -> "Welcome, BOSS"
                            isAdmin -> "Welcome Back, ${user?.name?.split(" ")?.get(0)}"
                            else -> "Hi, ${user?.name?.split(" ")?.get(0) ?: "Learner"}!"
                        }
                        Text(text = greeting, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Badge(containerColor = when(role) { "owner" -> Color.Red; "admin" -> Color(0xFF6200EE); else -> if (user?.isSubscribed == true) Color(0xFFFFD700) else Color.Gray }) {
                            Text(if (isStaff) role.uppercase() else if (user?.isSubscribed == true) "PRO" else "FREE", modifier = Modifier.padding(4.dp), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (!isStaff) {
                        Spacer(modifier = Modifier.height(4.dp))
                        val formattedPoints = String.format("%.1f", user?.pointsBalance ?: 0.0)
                        Text(
                            text = "Balance: ৳ ${user?.earnedBalance ?: 0} | Points: $formattedPoints",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (!isStaff) {
                // User View
                Row(modifier = Modifier.fillMaxWidth().height(100.dp).padding(bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val streakText = if (user?.dailyStreak == 1) "1 Day" else "${user?.dailyStreak ?: 0} Days"
                    StatCard(label = "Streak", value = streakText, modifier = Modifier.weight(1f))
                    val acc = if ((user?.totalAnswers ?: 0) > 0) (user!!.correctAnswers * 100 / user!!.totalAnswers) else 0
                    StatCard(label = "Accuracy", value = "$acc%", modifier = Modifier.weight(1f))
                    StatCard(label = "ID Referred", value = "${user?.referralCount ?: 0}", modifier = Modifier.weight(1f))
                }
                if (user?.isSubscribed == false) {
                    Card(onClick = onNavigateToSubscription, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFD700))) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.WorkspacePremium, null, tint = Color.White)
                            Spacer(modifier = Modifier.width(12.dp)); Column { Text("GO PRO NOW", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black); Text("Ad-Free Experience! Only 199 TK", fontSize = 12.sp, color = Color.Black.copy(alpha = 0.7f)) }
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    DashboardCard(title = "Learn Topicwise", icon = Icons.Default.School, modifier = Modifier.weight(1f), onClick = onNavigateToCategories)
                    DashboardCard(title = "Contests", icon = Icons.Default.EmojiEvents, modifier = Modifier.weight(1f), onClick = onNavigateToContests)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    DashboardCard(title = "History", icon = Icons.Default.History, modifier = Modifier.weight(1f), onClick = onNavigateToHistory)
                    DashboardCard(title = "Wallet", icon = Icons.Default.AccountBalanceWallet, modifier = Modifier.weight(1f), onClick = onNavigateToWallet)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    DashboardCard(title = "Rankings", icon = Icons.Default.Leaderboard, modifier = Modifier.weight(1f), onClick = onNavigateToLeaderboard)
                    DashboardCard(title = "Challenge", icon = Icons.Default.Groups, modifier = Modifier.weight(1f), onClick = onNavigateToRoom)
                }
            } else {
                // Staff View
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    DashboardCard(title = "Users", icon = Icons.Default.People, modifier = Modifier.weight(1f), onClick = { onNavigateToAdmin(0) })
                    DashboardCard(title = "Questions", icon = Icons.Default.AddCircle, modifier = Modifier.weight(1f), onClick = { onNavigateToAdmin(1) })
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    DashboardCard(title = "Categories", icon = Icons.Default.Category, modifier = Modifier.weight(1f), onClick = { onNavigateToAdmin(2) })
                    DashboardCard(title = "App History", icon = Icons.Default.History, modifier = Modifier.weight(1f), onClick = { onNavigateToAdmin(3) })
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    DashboardCard(title = "Correction Requests", icon = Icons.Default.RateReview, modifier = Modifier.weight(1f), onClick = onNavigateToReviews)
                    DashboardCard(title = "Rankings", icon = Icons.Default.Leaderboard, modifier = Modifier.weight(1f), onClick = onNavigateToLeaderboard)
                }
                
                if (isOwner) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        DashboardCard(title = "Finance", icon = Icons.Default.Payments, modifier = Modifier.weight(1f), onClick = onNavigateToFinance)
                        DashboardCard(title = "Staff Management", icon = Icons.Default.Analytics, modifier = Modifier.weight(1f), onClick = onNavigateToStaff)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    DashboardCard(title = "Revenue Dashboard", icon = Icons.Default.AccountBalanceWallet, modifier = Modifier.fillMaxWidth(), onClick = onNavigateToWallet)
                }
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(8.dp).fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
fun DashboardCard(title: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = modifier.height(100.dp)) {
        Column(modifier = Modifier.padding(8.dp).fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = title, style = MaterialTheme.typography.labelSmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}
