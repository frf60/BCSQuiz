package com.faysal.bcsquiz.ui.contest

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    viewModel: LeaderboardViewModel
) {
    val scores by viewModel.leaderboard.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Rankings") })
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = when(selectedTab) {
                "daily" -> 0
                "weekly" -> 1
                else -> 2
            }) {
                Tab(selected = selectedTab == "daily", onClick = { viewModel.onTabSelected("daily") }) {
                    Text("Daily", modifier = Modifier.padding(16.dp))
                }
                Tab(selected = selectedTab == "weekly", onClick = { viewModel.onTabSelected("weekly") }) {
                    Text("Weekly", modifier = Modifier.padding(16.dp))
                }
                Tab(selected = selectedTab == "monthly", onClick = { viewModel.onTabSelected("monthly") }) {
                    Text("Monthly", modifier = Modifier.padding(16.dp))
                }
            }

            if (scores.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No rankings yet for this period.")
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    itemsIndexed(scores) { index, user ->
                        val isWinner = index == 0
                        val currentUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                        
                        LeaderboardItem(rank = index + 1, user = user, type = selectedTab)
                        
                        if (isWinner && user.uid == currentUid && (selectedTab == "weekly" || selectedTab == "monthly")) {
                            ClaimRankRewardSection(viewModel = viewModel, type = selectedTab)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ClaimRankRewardSection(viewModel: LeaderboardViewModel, type: String) {
    val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
    var statusMessage by remember { mutableStateOf("") }
    
    Column(modifier = Modifier.padding(16.dp)) {
        if (statusMessage.isNotEmpty()) {
            Text(statusMessage, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelSmall)
        }
        Button(
            onClick = {
                viewModel.claimWinnerReward(uid, type) { msg ->
                    statusMessage = msg
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFF4CAF50))
        ) {
            val amount = if (type == "weekly") 100 else 250
            Text("CLAIM YOUR ৳$amount REWARD!", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        }
        Text(
            "Note: Claims only available after 11:30 PM on reward days.",
            style = MaterialTheme.typography.labelSmall,
            color = androidx.compose.ui.graphics.Color.Gray,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun LeaderboardItem(rank: Int, user: com.faysal.bcsquiz.data.model.User, type: String) {
    val score = when(type) {
        "daily" -> user.dailyScore
        "weekly" -> user.weeklyScore
        else -> user.monthlyScore
    }
    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("#$rank", style = MaterialTheme.typography.headlineSmall)
            Column(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                Text(text = user.name.ifEmpty { "User" }, style = MaterialTheme.typography.bodyLarge)
                if (user.isSubscribed) {
                    Text("PRO MEMBER", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            Text("Score: $score", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
        }
    }
}
