package com.faysal.bcsquiz.ui.quiz

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.faysal.bcsquiz.data.model.Question

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizPlayScreen(
    viewModel: QuizViewModel,
    onQuizComplete: (Int, Int) -> Unit
) {
    val questions by viewModel.questions.collectAsState()
    val currentIndex by viewModel.currentQuestionIndex.collectAsState()
    val score by viewModel.score.collectAsState()
    val selectedIndex by viewModel.selectedAnswerIndex.collectAsState()
    val showFeedback by viewModel.showFeedback.collectAsState()

    // Load user to check for Pro status
    var isPro by remember { mutableStateOf(false) }
    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
    val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
    
    LaunchedEffect(Unit) {
        db.collection("users").document(uid).get().await().let {
            isPro = it.getBoolean("isSubscribed") ?: false
            if (!isPro) {
                // Wait 3 seconds to "show ad" for free users
                kotlinx.coroutines.delay(3000)
            }
        }
    }

    if (questions.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                if (!isPro) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Showing Ad... (Unlock PRO for Ad-free)", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        return
    }

    if (currentIndex >= questions.size) {
        onQuizComplete(score, questions.size)
        return
    }

    val currentQuestion = questions[currentIndex]

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Question ${currentIndex + 1}/${questions.size}") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = currentQuestion.question,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            currentQuestion.options.forEachIndexed { index, option ->
                val color = when {
                    showFeedback && index == currentQuestion.correctIndex -> androidx.compose.ui.graphics.Color.Green
                    showFeedback && index == selectedIndex -> androidx.compose.ui.graphics.Color.Red
                    else -> MaterialTheme.colorScheme.primaryContainer
                }

                val contentColor = if (showFeedback && (index == currentQuestion.correctIndex || index == selectedIndex)) {
                    androidx.compose.ui.graphics.Color.White
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                }

                Button(
                    onClick = { viewModel.selectAnswer(index) },
                    enabled = !showFeedback,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = color,
                        contentColor = contentColor,
                        disabledContainerColor = color,
                        disabledContentColor = contentColor
                    )
                ) {
                    Text(text = option)
                }
            }

            if (showFeedback) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Explanation: ${currentQuestion.explanation}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    var showReviewDialog by remember { mutableStateOf(false) }
                    TextButton(onClick = { showReviewDialog = true }) {
                        Text("Report Issue", color = MaterialTheme.colorScheme.error)
                    }
                    
                    if (showReviewDialog) {
                        var note by remember { mutableStateOf("") }
                        AlertDialog(
                            onDismissRequest = { showReviewDialog = false },
                            title = { Text("Report Question") },
                            text = {
                                TextField(value = note, onValueChange = { note = it }, label = { Text("What is wrong?") })
                            },
                            confirmButton = {
                                Button(onClick = {
                                    val request = com.faysal.bcsquiz.data.model.ReviewRequest(
                                        questionId = currentQuestion.id,
                                        questionText = currentQuestion.question,
                                        uid = uid,
                                        userNote = note
                                    )
                                    viewModel.submitReview(request) 
                                    showReviewDialog = false
                                }) { Text("Submit") }
                            }
                        )
                    }

                    Button(
                        onClick = { viewModel.nextQuestion() },
                    ) {
                        Text("Next Question")
                    }
                }
            }
        }
    }
}
