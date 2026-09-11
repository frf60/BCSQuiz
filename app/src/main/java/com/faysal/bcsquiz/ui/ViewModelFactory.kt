package com.faysal.bcsquiz.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.faysal.bcsquiz.data.repository.AuthRepository
import com.faysal.bcsquiz.data.repository.QuizRepository
import com.faysal.bcsquiz.ui.contest.ContestPlayViewModel
import com.faysal.bcsquiz.ui.contest.ContestViewModel
import com.faysal.bcsquiz.ui.contest.LeaderboardViewModel
import com.faysal.bcsquiz.ui.room.RoomViewModel
import com.faysal.bcsquiz.ui.subscription.SubscriptionViewModel
import com.faysal.bcsquiz.data.repository.BillingRepository
import com.faysal.bcsquiz.ui.home.HomeViewModel
import com.faysal.bcsquiz.ui.login.LoginViewModel
import com.faysal.bcsquiz.ui.quiz.QuizViewModel

class ViewModelFactory(
    private val quizRepository: QuizRepository? = null,
    private val authRepository: AuthRepository? = null,
    private val billingRepository: BillingRepository? = null,
    private val categoryId: String? = null,
    private val subCategoryId: String? = null,
    private val contestId: String? = null,
    private val questionIds: List<String>? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(quizRepository!!) as T
        }
        if (modelClass.isAssignableFrom(QuizViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return QuizViewModel(quizRepository!!, categoryId ?: "", subCategoryId) as T
        }
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LoginViewModel(authRepository!!) as T
        }
        if (modelClass.isAssignableFrom(ContestViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ContestViewModel(quizRepository!!) as T
        }
        if (modelClass.isAssignableFrom(ContestPlayViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ContestPlayViewModel(quizRepository!!, contestId!!, questionIds!!) as T
        }
        if (modelClass.isAssignableFrom(LeaderboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LeaderboardViewModel(quizRepository!!) as T
        }
        if (modelClass.isAssignableFrom(RoomViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RoomViewModel(quizRepository!!) as T
        }
        if (modelClass.isAssignableFrom(SubscriptionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SubscriptionViewModel(billingRepository!!) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
