package com.faysal.bcsquiz.ui.contest

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.faysal.bcsquiz.data.model.Contest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContestListScreen(
    viewModel: ContestViewModel,
    onNavigateToSubscription: () -> Unit,
    onContestClick: (Contest) -> Unit
) {
    val contests by viewModel.contests.collectAsState()
    val user by viewModel.user.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(title = { Text("Live Contests") })
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            user?.let {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Your Balance: ${it.pointsBalance} Points", style = MaterialTheme.typography.titleMedium)
                        if (it.isSubscribed) {
                            Text("PRO MEMBER", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            if (contests.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No live contests available right now.")
                }
            } else {
                LazyColumn {
                    items(contests) { contest ->
                        ContestItem(contest) {
                            viewModel.joinContest(contest) { success, message ->
                                if (success) {
                                    onContestClick(contest)
                                } else {
                                    if (message.contains("points", ignoreCase = true)) {
                                        // Auto-navigate to subscription if points are missing
                                        onNavigateToSubscription()
                                    } else {
                                        scope.launch {
                                            snackbarHostState.showSnackbar(message)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ContestItem(contest: Contest, onJoin: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = contest.title, style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Entry Fee: ${contest.entryCost} Points")
                Text("Prize: ${contest.prizePool} TK", color = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onJoin, modifier = Modifier.align(Alignment.End)) {
                Text("Join Contest")
            }
        }
    }
}
