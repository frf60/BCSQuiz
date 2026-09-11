package com.faysal.bcsquiz.ui.contest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.faysal.bcsquiz.data.model.Question
import com.faysal.bcsquiz.data.repository.QuizRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ContestPlayViewModel(
    private val repository: QuizRepository,
    private val contestId: String,
    private val questionIds: List<String>
) : ViewModel() {

    private val _questions = MutableStateFlow<List<Question>>(emptyList())
    val questions: StateFlow<List<Question>> = _questions

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex

    private val _score = MutableStateFlow(0)
    val score: StateFlow<Int> = _score

    private val _timeLeft = MutableStateFlow(10) // 10 seconds per question
    val timeLeft: StateFlow<Int> = _timeLeft

    private val _isFinished = MutableStateFlow(false)
    val isFinished: StateFlow<Boolean> = _isFinished

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private var timerJob: Job? = null

    init {
        loadQuestions()
    }

    private fun loadQuestions() {
        viewModelScope.launch {
            _questions.value = repository.getQuestionsByIds(questionIds).shuffled().map { it.shuffled() }
            _isLoading.value = false
            if (_questions.value.isNotEmpty()) {
                startQuestionTimer()
            } else {
                _isFinished.value = true
            }
        }
    }

    private fun startQuestionTimer() {
        timerJob?.cancel()
        _timeLeft.value = 10
        timerJob = viewModelScope.launch {
            while (_timeLeft.value > 0 && !_isFinished.value) {
                delay(1000)
                _timeLeft.value -= 1
            }
            if (!_isFinished.value) {
                moveToNextQuestion()
            }
        }
    }

    fun submitAnswer(answerIndex: Int) {
        if (_isFinished.value) return
        
        val currentQuestion = _questions.value.getOrNull(_currentIndex.value) ?: return
        if (answerIndex == currentQuestion.correctIndex) {
            _score.value += 1
            // Update stats
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            viewModelScope.launch {
                repository.updateQuestionStats(uid, currentQuestion.subject, true)
            }
        } else {
            val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            viewModelScope.launch {
                repository.updateQuestionStats(uid, currentQuestion.subject, false)
            }
        }
        
        moveToNextQuestion()
    }

    private fun moveToNextQuestion() {
        if (_currentIndex.value + 1 < _questions.value.size) {
            _currentIndex.value += 1
            startQuestionTimer()
        } else {
            finishContest()
        }
    }

    private fun finishContest() {
        timerJob?.cancel()
        _isFinished.value = true
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            repository.submitContestResult(contestId, uid, _score.value)
        }
    }
}
