package com.faysal.bcsquiz.ui.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.faysal.bcsquiz.data.model.Question
import com.faysal.bcsquiz.data.repository.QuizRepository
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    repository: QuizRepository
) {
    var wrongAnswers by remember { mutableStateOf<List<Question>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@LaunchedEffect
        wrongAnswers = repository.getWrongAnswers(uid)
        isLoading = false
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Wrong Answer History") }) }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (wrongAnswers.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("No mistakes yet! Keep it up.")
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(wrongAnswers) { question ->
                    WrongAnswerItem(question)
                }
            }
        }
    }
}

@Composable
fun WrongAnswerItem(question: Question) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = question.question, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Correct Answer: ${question.options.getOrNull(question.correctIndex) ?: "N/A"}",
                color = MaterialTheme.colorScheme.error,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Explanation: ${question.explanation}", style = MaterialTheme.typography.bodySmall)
        }
    }
}
