package com.faysal.bcsquiz.ui.contest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.faysal.bcsquiz.data.model.Contest
import com.faysal.bcsquiz.data.model.User
import com.faysal.bcsquiz.data.repository.QuizRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ContestViewModel(private val repository: QuizRepository) : ViewModel() {

    private val _contests = MutableStateFlow<List<Contest>>(emptyList())
    val contests: StateFlow<List<Contest>> = _contests

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user

    init {
        loadContests()
        loadUserProfile()
    }

    private fun loadContests() {
        viewModelScope.launch {
            _contests.value = repository.getContests()
        }
    }

    private fun loadUserProfile() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            _user.value = repository.getUser(uid)
        }
    }

    fun joinContest(contest: Contest, onResult: (Boolean, String) -> Unit) {
        val currentUser = _user.value
        if (currentUser == null) {
            onResult(false, "User not found")
            return
        }

        val currentMonth = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH)
        val currentCount = if (currentUser.lastContestMonth == currentMonth) currentUser.contestCountThisMonth else 0
        val limit = if (currentUser.isSubscribed) 10 else 5

        if (currentCount >= limit) {
            onResult(false, "Monthly limit reached ($limit contests). Upgrade or wait until next month.")
            return
        }

        if (currentUser.isSubscribed || currentUser.pointsBalance >= contest.entryCost) {
            viewModelScope.launch {
                if (!currentUser.isSubscribed) {
                    repository.updateUserPoints(currentUser.uid, currentUser.pointsBalance - contest.entryCost.toDouble())
                }
                repository.incrementContestCount(currentUser.uid, currentMonth)
                onResult(true, "Joined successfully")
                loadUserProfile() // Refresh data
            }
        } else {
            onResult(false, "Not enough points. Please subscribe or earn points.")
        }
    }
}
