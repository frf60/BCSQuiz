package com.faysal.bcsquiz.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.faysal.bcsquiz.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _user = MutableStateFlow<FirebaseUser?>(repository.getCurrentUser())
    val user: StateFlow<FirebaseUser?> = _user

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun signIn() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            repository.signInWithGoogle().fold(
                onSuccess = { _user.value = it },
                onFailure = { _error.value = it.message ?: "Unknown error" }
            )
            _loading.value = false
        }
    }

    fun signOut() {
        viewModelScope.launch {
            repository.signOut()
            _user.value = null
        }
    }
}
