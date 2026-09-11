package com.faysal.bcsquiz.ui.room

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.faysal.bcsquiz.data.model.Room
import com.faysal.bcsquiz.data.repository.QuizRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RoomViewModel(private val repository: QuizRepository) : ViewModel() {

    private val _room = MutableStateFlow<Room?>(null)
    val room: StateFlow<Room?> = _room

    private val db = FirebaseFirestore.getInstance()
    private val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    fun createRoom(categoryIds: List<String>, onComplete: (String) -> Unit) {
        viewModelScope.launch {
            val allQuestions = mutableListOf<String>()
            categoryIds.forEach { catId ->
                val questions = repository.getQuestions(catId).shuffled().take(15).map { it.id }
                allQuestions.addAll(questions)
            }
            
            if (allQuestions.isNotEmpty()) {
                val code = repository.createRoom(uid, allQuestions)
                listenToRoom(code)
                onComplete(code)
            }
        }
    }

    fun joinRoom(code: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            repository.joinRoom(uid, code).fold(
                onSuccess = {
                    listenToRoom(code)
                    onResult(true, "Joined successfully")
                },
                onFailure = { onResult(false, it.message ?: "Failed to join") }
            )
        }
    }

    private fun listenToRoom(code: String) {
        db.collection("rooms").document(code)
            .addSnapshotListener { snapshot, _ ->
                snapshot?.toObject(Room::class.java)?.let {
                    _room.value = it
                }
            }
    }

    fun startGame() {
        val currentRoom = _room.value ?: return
        if (currentRoom.creatorUid == uid) {
            db.collection("rooms").document(currentRoom.id).update("status", "playing")
        }
    }
}
