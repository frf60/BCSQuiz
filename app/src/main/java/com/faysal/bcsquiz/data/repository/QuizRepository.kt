package com.faysal.bcsquiz.data.repository

import com.faysal.bcsquiz.data.model.*
import com.faysal.bcsquiz.data.remote.FirestoreService

class QuizRepository(private val firestoreService: FirestoreService) {

    suspend fun getCategories(): List<Category> = firestoreService.getCategories()

    suspend fun getQuestions(categoryId: String, subCategoryId: String? = null): List<Question> = 
        firestoreService.getQuestions(categoryId, subCategoryId)

    suspend fun getContests(): List<Contest> = firestoreService.getContests()

    suspend fun getUser(uid: String): User? = firestoreService.getUser(uid)

    suspend fun updateUserPoints(uid: String, points: Double) = 
        firestoreService.updateUserPoints(uid, points)

    suspend fun incrementContestCount(uid: String, month: Int) = 
        firestoreService.incrementContestCount(uid, month)

    suspend fun getQuestionsByIds(ids: List<String>): List<Question> = 
        firestoreService.getQuestionsByIds(ids)

    suspend fun submitContestResult(contestId: String, uid: String, score: Int) = 
        firestoreService.submitContestResult(contestId, uid, score)

    suspend fun getGlobalLeaderboard(type: String) = 
        firestoreService.getGlobalLeaderboard(type)

    suspend fun updateGlobalScore(uid: String, score: Int) =
        firestoreService.updateGlobalScore(uid, score)

    suspend fun claimDailyReward(uid: String) =
        firestoreService.claimDailyReward(uid)

    suspend fun applyReferral(uid: String, code: String) =
        firestoreService.applyReferral(uid, code)

    suspend fun getWrongAnswers(uid: String) =
        firestoreService.getWrongAnswers(uid)

    suspend fun createRoom(uid: String, questionIds: List<String>) =
        firestoreService.createRoom(uid, questionIds)

    suspend fun joinRoom(uid: String, code: String) =
        firestoreService.joinRoom(uid, code)

    suspend fun getTransactions(uid: String) =
        firestoreService.getTransactions(uid)

    suspend fun exchangeBalance(uid: String, amount: Int) =
        firestoreService.exchangeBalanceForPoints(uid, amount)

    suspend fun searchUser(email: String) = firestoreService.searchUserByEmail(email)
    suspend fun updateRole(uid: String, role: String) = firestoreService.updateUserRole(uid, role)
    suspend fun getRevenue(type: String) = firestoreService.getRevenueStats(type)
    suspend fun searchQuestions(query: String) = firestoreService.searchQuestions(query)
    
    suspend fun addNewQuestion(q: Question, adminUid: String, adminName: String) = firestoreService.addNewQuestion(q, adminUid, adminName)
    suspend fun updateQuestion(q: Question, adminUid: String, adminName: String) = firestoreService.updateQuestion(q, adminUid, adminName)
    suspend fun createCategory(c: Category, adminUid: String, adminName: String) = firestoreService.createCategory(c, adminUid, adminName)
    suspend fun updateCategory(oldId: String, c: Category, adminUid: String, adminName: String) = firestoreService.updateCategory(oldId, c, adminUid, adminName)
    suspend fun deleteCategory(id: String, adminUid: String, adminName: String) = firestoreService.deleteCategory(id, adminUid, adminName)
    
    suspend fun getAllUsers() = firestoreService.getAllUsers()
    suspend fun getAdmins() = firestoreService.getAdmins()
    suspend fun getAdminHistory(uid: String, isOwner: Boolean) = firestoreService.getAdminActivities(uid, isOwner)

    suspend fun updateQuestionStats(uid: String, categoryId: String, isCorrect: Boolean) =
        firestoreService.updateQuestionStats(uid, categoryId, isCorrect)

    suspend fun claimRankReward(uid: String, type: String) = 
        firestoreService.claimRankReward(uid, type)

    suspend fun submitReview(request: ReviewRequest) = firestoreService.submitReviewRequest(request)
    suspend fun submitTopup(request: TopupRequest) = firestoreService.submitTopupRequest(request)
    
    suspend fun handleWithdrawal(id: String, adminUid: String, adminName: String, status: String, note: String) =
        firestoreService.handleWithdrawalRequest(id, adminUid, adminName, status, note)

    suspend fun handleTopup(id: String, adminUid: String, adminName: String, status: String, note: String) =
        firestoreService.handleTopupRequest(id, adminUid, adminName, status, note)

    suspend fun handleReview(id: String, adminUid: String, adminName: String, status: String) =
        firestoreService.handleReviewRequest(id, adminUid, adminName, status)

    suspend fun submitWithdrawal(request: WithdrawalRequest) =
        firestoreService.submitWithdrawalRequest(request)
}
