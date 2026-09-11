package com.faysal.bcsquiz.data.model

import com.google.firebase.Timestamp

data class Question(
    val id: String = "",
    val subject: String = "",
    val bcsRelated: Boolean = false,
    val question: String = "",
    val options: List<String> = emptyList(),
    val correctIndex: Int = -1,
    val explanation: String = "",
    val categoryId: String = "", // Required: Main Category Slug
    val subCategoryId: String? = null, // Optional: Sub-Category Slug
    val difficulty: String = "medium",
    val addedBy: String = ""
) {
    fun shuffled(): Question {
        if (options.isEmpty() || correctIndex !in options.indices) return this
        val correctOption = options[correctIndex]
        val shuffledOptions = options.shuffled()
        val newCorrectIndex = shuffledOptions.indexOf(correctOption)
        return this.copy(options = shuffledOptions, correctIndex = newCorrectIndex)
    }
}

data class Category(
    val id: String = "",
    val name: String = "",
    val iconUrl: String = ""
)

data class SubCategory(
    val id: String = "",
    val parentCategoryId: String = "",
    val name: String = ""
)

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val pointsBalance: Double = 0.0,
    val isSubscribed: Boolean = false,
    val subscriptionExpiry: Timestamp? = null,
    val contestCountThisMonth: Int = 0,
    val lastContestMonth: Int = -1,
    val quizzesCompleted: Int = 0,
    val contestsCompleted: Int = 0,
    val correctAnswers: Int = 0,
    val totalAnswers: Int = 0,
    val dailyStreak: Int = 0,
    val lastActiveDate: com.google.firebase.Timestamp? = null,
    val dailyScore: Int = 0,
    val weeklyScore: Int = 0,
    val monthlyScore: Int = 0,
    val lastDailyReset: com.google.firebase.Timestamp? = null,
    val lastWeeklyReset: com.google.firebase.Timestamp? = null,
    val lastMonthlyReset: com.google.firebase.Timestamp? = null,
    val lastDailyRewardClaimed: com.google.firebase.Timestamp? = null,
    val referredBy: String? = null,
    val deviceId: String = "",
    val earnedBalance: Int = 0, // TK
    val role: String = "user", // "owner", "admin", "moderator", "user"
    val topicCorrectAnswers: Map<String, Int> = emptyMap(),
    val topicTotalAnswers: Map<String, Int> = emptyMap(),
    val referralCount: Int = 0,
    val joinedAt: com.google.firebase.Timestamp? = null,
    val gender: String = "N/A" // "male", "female", "N/A"
)

data class Transaction(
    val id: String = "",
    val uid: String = "",
    val userEmail: String = "", // Added for owner visibility
    val amount: Int = 0,
    val type: String = "", // "reward", "withdraw", "exchange", "topup"
    val description: String = "",
    val adminNote: String = "",
    val timestamp: com.google.firebase.Timestamp? = null
)

data class Contest(
    val id: String = "",
    val title: String = "",
    val startTime: Timestamp? = null,
    val entryCost: Int = 0,
    val prizePool: Int = 0,
    val questionIds: List<String> = emptyList()
)

data class Room(
    val id: String = "", // Room code
    val creatorUid: String = "",
    val players: List<String> = emptyList(), // Max 5 UIDs
    val status: String = "open", // open, playing, finished
    val entryFee: Int = 20,
    val questionIds: List<String> = emptyList(),
    val scores: Map<String, Int> = emptyMap()
)

data class WithdrawalRequest(
    val id: String = "",
    val uid: String = "",
    val amount: Int = 0,
    val method: String = "", // Bkash, Nagad
    val phoneNumber: String = "",
    val status: String = "pending",
    val adminNote: String = "",
    val timestamp: com.google.firebase.Timestamp? = null
)

data class ReviewRequest(
    val id: String = "",
    val questionId: String = "",
    val questionText: String = "",
    val uid: String = "",
    val userNote: String = "",
    val adminNote: String = "",
    val status: String = "pending", // pending, accepted, rejected
    val handledBy: String = "",
    val timestamp: com.google.firebase.Timestamp? = null
)

data class TopupRequest(
    val id: String = "",
    val uid: String = "",
    val amount: Int = 0,
    val transactionId: String = "",
    val note: String = "",
    val adminNote: String = "",
    val status: String = "pending", // pending, approved, rejected
    val timestamp: com.google.firebase.Timestamp? = null
)

data class AdminActivity(
    val id: String = "",
    val adminUid: String = "",
    val adminName: String = "",
    val actionType: String = "", // "add_question", "edit_question", "add_category", etc.
    val targetId: String = "", // ID of the question or category
    val description: String = "",
    val timestamp: com.google.firebase.Timestamp? = null
)
