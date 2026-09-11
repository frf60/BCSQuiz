package com.faysal.bcsquiz.ui.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.faysal.bcsquiz.data.model.Question
import com.faysal.bcsquiz.data.repository.QuizRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class QuizViewModel(
    private val repository: QuizRepository,
    private val categoryId: String,
    private val subCategoryId: String? = null
) : ViewModel() {

    private val _questions = MutableStateFlow<List<Question>>(emptyList())
    val questions: StateFlow<List<Question>> = _questions

    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex

    private val _score = MutableStateFlow(0)
    val score: StateFlow<Int> = _score

    private val _selectedAnswerIndex = MutableStateFlow<Int?>(null)
    val selectedAnswerIndex: StateFlow<Int?> = _selectedAnswerIndex

    private val _showFeedback = MutableStateFlow(false)
    val showFeedback: StateFlow<Boolean> = _showFeedback

    init {
        loadQuestions()
    }

    private fun loadQuestions() {
        viewModelScope.launch {
            val allQuestions = repository.getQuestions(categoryId, subCategoryId)
            val uniqueQuestions = allQuestions.distinctBy { it.question }
            _questions.value = uniqueQuestions.shuffled().take(20).map { it.shuffled() }
        }
    }

    fun selectAnswer(index: Int) {
        if (_showFeedback.value) return
        
        val questionsList = _questions.value
        val currentIndex = _currentQuestionIndex.value
        
        if (currentIndex >= questionsList.size) return
        
        val currentQuestion = questionsList[currentIndex]
        
        _selectedAnswerIndex.value = index
        _showFeedback.value = true
        
        if (index == currentQuestion.correctIndex) {
            _score.value += 1
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            uid?.let {
                viewModelScope.launch {
                    repository.updateGlobalScore(it, 10)
                    repository.updateQuestionStats(it, categoryId, true)
                }
            }
        } else {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
            val questionId = if (currentQuestion.id.isNotEmpty()) currentQuestion.id else currentQuestion.hashCode().toString()
            
            viewModelScope.launch {
                try {
                    repository.updateQuestionStats(uid, categoryId, false)
                    val firestore = FirebaseFirestore.getInstance()
                    firestore.collection("users").document(uid)
                        .collection("wrongAnswers")
                        .document(questionId)
                        .set(currentQuestion.copy(id = questionId))
                } catch (e: Exception) {
                    // Prevent crash on firestore failure
                }
            }
        }
    }

    fun nextQuestion() {
        _selectedAnswerIndex.value = null
        _showFeedback.value = false
        _currentQuestionIndex.value += 1
    }

    fun submitReview(request: com.faysal.bcsquiz.data.model.ReviewRequest) {
        viewModelScope.launch {
            repository.submitReview(request)
        }
    }
}
