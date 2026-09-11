package com.faysal.bcsquiz.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.faysal.bcsquiz.data.model.Category
import com.faysal.bcsquiz.data.repository.QuizRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

import com.faysal.bcsquiz.data.model.User
import com.google.firebase.auth.FirebaseAuth

class HomeViewModel(private val repository: QuizRepository) : ViewModel() {

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user

    init {
        loadCategories()
        loadUserProfile()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            _categories.value = repository.getCategories()
        }
    }

    fun loadUserProfile() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            val firestoreUser = repository.getUser(uid)
            if (firestoreUser == null) {
                // Emergency user doc creation handled by AuthRepository mostly
            } else {
                _user.value = firestoreUser
            }
        }
    }

    fun claimDailyReward(onResult: (String) -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            repository.claimDailyReward(uid).fold(
                onSuccess = { 
                    loadUserProfile()
                    onResult("Congratulations! You've claimed 10 points.")
                },
                onFailure = { onResult(it.message ?: "Failed to claim reward") }
            )
        }
    }

    fun applyReferral(code: String, onResult: (String) -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            repository.applyReferral(uid, code).fold(
                onSuccess = {
                    loadUserProfile()
                    onResult("Referral applied! Both received 50 points.")
                },
                onFailure = { onResult(it.message ?: "Failed to apply referral") }
            )
        }
    }
}
