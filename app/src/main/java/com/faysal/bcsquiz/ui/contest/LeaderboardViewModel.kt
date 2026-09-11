package com.faysal.bcsquiz.ui.contest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.faysal.bcsquiz.data.repository.QuizRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

import com.faysal.bcsquiz.data.model.User

class LeaderboardViewModel(
    val repository: QuizRepository
) : ViewModel() {

    private val _leaderboard = MutableStateFlow<List<User>>(emptyList())
    val leaderboard: StateFlow<List<User>> = _leaderboard

    private val _selectedTab = MutableStateFlow("daily")
    val selectedTab: StateFlow<String> = _selectedTab

    init {
        loadLeaderboard("daily")
    }

    fun onTabSelected(type: String) {
        _selectedTab.value = type
        loadLeaderboard(type)
    }

    private fun loadLeaderboard(type: String) {
        viewModelScope.launch {
            _leaderboard.value = repository.getGlobalLeaderboard(type)
        }
    }

    fun claimWinnerReward(uid: String, type: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            repository.claimRankReward(uid, type).fold(
                onSuccess = { onResult("Success! ৳$it added to your wallet.") },
                onFailure = { onResult(it.message ?: "Failed to claim") }
            )
        }
    }
}
