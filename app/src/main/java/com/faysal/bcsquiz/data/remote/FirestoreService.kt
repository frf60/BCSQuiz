package com.faysal.bcsquiz.data.remote

import com.faysal.bcsquiz.data.model.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.FieldPath
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await
import java.util.Calendar

class FirestoreService {
    private val db = FirebaseFirestore.getInstance()

    suspend fun getCategories(): List<Category> {
        return try {
            db.collection("categories").get().await().toObjects(Category::class.java)
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getSubCategories(categoryId: String): List<SubCategory> {
        return try {
            db.collection("categories").document(categoryId)
                .collection("subcategories")
                .get().await().toObjects(SubCategory::class.java)
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getQuestions(categoryId: String, subCategoryId: String? = null): List<Question> {
        return try {
            val query = if (subCategoryId != null) {
                db.collection("questions").whereEqualTo("categoryId", categoryId).whereEqualTo("subCategoryId", subCategoryId)
            } else {
                db.collection("questions").whereEqualTo("categoryId", categoryId)
            }
            
            var result = query.limit(100).get().await().documents.mapNotNull { it.toObject(Question::class.java)?.copy(id = it.id) }
            
            if (result.isEmpty() && subCategoryId == null) {
                val oldResult = db.collection("questions").whereEqualTo("subject", categoryId).limit(100).get().await()
                result = oldResult.documents.mapNotNull { it.toObject(Question::class.java)?.copy(id = it.id) }
            }
            result
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getQuestionsByCategory(categoryId: String): List<Question> = getQuestions(categoryId)

    suspend fun createCategory(category: Category, adminUid: String, adminName: String): Result<Unit> {
        return try {
            db.collection("categories").document(category.id).set(category).await()
            logAdminActivity(adminUid, adminName, "add_category", category.id, "Added category: ${category.name}")
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun updateCategory(oldId: String, category: Category, adminUid: String, adminName: String): Result<Unit> {
        return try {
            if (oldId != category.id) {
                db.collection("categories").document(oldId).delete().await()
            }
            db.collection("categories").document(category.id).set(category).await()
            logAdminActivity(adminUid, adminName, "edit_category", category.id, "Edited category: ${category.name}")
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun deleteCategory(id: String, adminUid: String, adminName: String): Result<Unit> {
        return try {
            db.collection("categories").document(id).delete().await()
            logAdminActivity(adminUid, adminName, "delete_category", id, "Deleted category ID: $id")
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun searchQuestions(query: String): List<Question> {
        return try {
            db.collection("questions")
                .orderBy("question")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .limit(20).get().await()
                .documents.mapNotNull { it.toObject(Question::class.java)?.copy(id = it.id) }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getContests(): List<Contest> {
        return try {
            db.collection("contests").get().await().toObjects(Contest::class.java)
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getUser(uid: String): User? {
        return try {
            val userRef = db.collection("users").document(uid)
            var user = userRef.get().await().toObject(User::class.java)
            if (user != null) {
                val now = Calendar.getInstance()
                val today = now.get(Calendar.DAY_OF_YEAR)
                val currentYear = now.get(Calendar.YEAR)
                val lastActive = Calendar.getInstance()
                user.lastActiveDate?.let { lastActive.time = it.toDate() }
                val lastActiveDay = lastActive.get(Calendar.DAY_OF_YEAR)
                val lastActiveYear = lastActive.get(Calendar.YEAR)

                if (user.lastActiveDate == null) {
                    userRef.update("dailyStreak", 1, "lastActiveDate", Timestamp.now()).await()
                    user = user.copy(dailyStreak = 1)
                } else if (currentYear == lastActiveYear && today == lastActiveDay) {
                    if (user.dailyStreak == 0) {
                        userRef.update("dailyStreak", 1).await()
                        user = user.copy(dailyStreak = 1)
                    }
                } else {
                    val isYesterday = if (currentYear == lastActiveYear) today == lastActiveDay + 1 else today == 1
                    if (isYesterday) {
                        userRef.update("dailyStreak", FieldValue.increment(1), "lastActiveDate", Timestamp.now()).await()
                        user = user.copy(dailyStreak = user.dailyStreak + 1)
                    } else {
                        userRef.update("dailyStreak", 1, "lastActiveDate", Timestamp.now()).await()
                        user = user.copy(dailyStreak = 1)
                    }
                }
            }
            user
        } catch (e: Exception) { null }
    }

    suspend fun getAllUsers(limit: Int = 50): List<User> {
        return try {
            db.collection("users")
                .orderBy("joinedAt", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
                .toObjects(User::class.java)
        } catch (e: Exception) { emptyList() }
    }

    suspend fun searchUserByEmail(email: String): List<User> {
        return try {
            db.collection("users")
                .whereEqualTo("email", email.lowercase().trim())
                .get()
                .await()
                .toObjects(User::class.java)
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getAdmins(): List<User> {
        return try {
            db.collection("users")
                .whereEqualTo("role", "admin")
                .get()
                .await()
                .toObjects(User::class.java)
        } catch (e: Exception) { emptyList() }
    }

    suspend fun updateUserPoints(uid: String, points: Double) {
        try { db.collection("users").document(uid).update("pointsBalance", points).await() } catch (e: Exception) { }
    }

    suspend fun updateUserRole(uid: String, role: String) {
        try { db.collection("users").document(uid).update("role", role).await() } catch (e: Exception) { }
    }

    suspend fun incrementContestCount(uid: String, currentMonth: Int) {
        try {
            val userRef = db.collection("users").document(uid)
            val user = userRef.get().await().toObject(User::class.java) ?: return
            if (user.lastContestMonth != currentMonth) {
                userRef.update("contestCountThisMonth", 1, "lastContestMonth", currentMonth).await()
            } else {
                userRef.update("contestCountThisMonth", FieldValue.increment(1)).await()
            }
        } catch (e: Exception) { }
    }

    suspend fun getQuestionsByIds(questionIds: List<String>): List<Question> {
        return try {
            if (questionIds.isEmpty()) return emptyList()
            db.collection("questions")
                .whereIn(FieldPath.documentId(), questionIds)
                .get().await()
                .documents.mapNotNull { it.toObject(Question::class.java)?.copy(id = it.id) }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun submitContestResult(contestId: String, uid: String, score: Int) {
        try {
            val participant = mapOf("uid" to uid, "score" to score, "submittedAt" to Timestamp.now())
            db.collection("contests").document(contestId).collection("participants").document(uid).set(participant).await()
            updateGlobalScore(uid, score)
        } catch (e: Exception) { }
    }

    suspend fun getGlobalLeaderboard(type: String): List<User> {
        val field = when(type) { "daily" -> "dailyScore"; "weekly" -> "weeklyScore"; else -> "monthlyScore" }
        return try {
            db.collection("users").orderBy(field, Query.Direction.DESCENDING).limit(20).get().await().toObjects(User::class.java)
        } catch (e: Exception) { emptyList() }
    }

    suspend fun getRevenueStats(type: String): Int {
        val calendar = Calendar.getInstance()
        val startTime = when(type) {
            "daily" -> { 
                calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
                Timestamp(calendar.time) 
            }
            "weekly" -> { 
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek); calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
                Timestamp(calendar.time) 
            }
            else -> { 
                calendar.set(Calendar.DAY_OF_MONTH, 1); calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
                Timestamp(calendar.time) 
            }
        }
        return try {
            val snapshot = db.collection("transactions")
                .whereGreaterThanOrEqualTo("timestamp", startTime)
                .get()
                .await()
            
            val transactions = snapshot.toObjects(Transaction::class.java)
            transactions.filter { it.type in listOf("exchange", "subscription", "topup") }.sumOf { it.amount }
        } catch (e: Exception) { 0 }
    }

    suspend fun getTransactions(uid: String): List<Transaction> {
        return try {
            val query = if (uid.isEmpty()) db.collection("transactions") else db.collection("transactions").whereEqualTo("uid", uid)
            val snapshot = query.get().await()
            snapshot.toObjects(Transaction::class.java).sortedByDescending { it.timestamp }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun submitWithdrawalRequest(request: WithdrawalRequest): Result<Unit> {
        return try {
            val userRef = db.collection("users").document(request.uid)
            db.runBatch { batch ->
                val id = db.collection("withdrawals").document().id
                batch.set(db.collection("withdrawals").document(id), request.copy(id = id, timestamp = Timestamp.now()))
                batch.update(userRef, "earnedBalance", FieldValue.increment(-request.amount.toLong()))
            }.await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun handleWithdrawalRequest(requestId: String, adminUid: String, adminName: String, status: String, note: String): Result<Unit> {
        return try {
            val reqRef = db.collection("withdrawals").document(requestId)
            val req = reqRef.get().await().toObject(WithdrawalRequest::class.java) ?: return Result.failure(Exception("Request not found"))
            val userRef = db.collection("users").document(req.uid)
            val userProfile = db.collection("users").document(req.uid).get().await().toObject(User::class.java)
            
            db.runBatch { batch ->
                batch.update(reqRef, "status", status, "adminNote", note)
                if (status == "rejected") {
                    batch.update(userRef, "earnedBalance", FieldValue.increment(req.amount.toLong()))
                }
                
                val transId = db.collection("transactions").document().id
                batch.set(db.collection("transactions").document(transId), Transaction(
                    id = transId, uid = req.uid, userEmail = userProfile?.email ?: "", 
                    amount = req.amount, type = "withdraw", description = "Withdrawal $status: ${req.method}", 
                    adminNote = note, timestamp = Timestamp.now()
                ))
            }.await()
            logAdminActivity(adminUid, adminName, "handle_withdrawal", requestId, "Handled withdrawal: $status for ${userProfile?.email}")
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun handleReviewRequest(requestId: String, adminUid: String, adminName: String, status: String): Result<Unit> {
        return try {
            db.collection("reviews").document(requestId).update("status", status, "handledBy", adminName).await()
            logAdminActivity(adminUid, adminName, "handle_review", requestId, "Handled review request: $status")
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun handleTopupRequest(requestId: String, adminUid: String, adminName: String, status: String, note: String): Result<Unit> {
        return try {
            val reqRef = db.collection("topups").document(requestId)
            val req = reqRef.get().await().toObject(TopupRequest::class.java) ?: return Result.failure(Exception("Request not found"))
            val userRef = db.collection("users").document(req.uid)
            val userProfile = db.collection("users").document(req.uid).get().await().toObject(User::class.java)
            
            db.runBatch { batch ->
                batch.update(reqRef, "status", status, "adminNote", note)
                if (status == "approved") {
                    batch.update(userRef, "earnedBalance", FieldValue.increment(req.amount.toLong()))
                }
                
                val transId = db.collection("transactions").document().id
                batch.set(db.collection("transactions").document(transId), Transaction(
                    id = transId, uid = req.uid, userEmail = userProfile?.email ?: "", 
                    amount = req.amount, type = "topup", description = "Topup $status", 
                    adminNote = note, timestamp = Timestamp.now()
                ))
            }.await()
            logAdminActivity(adminUid, adminName, "handle_topup", requestId, "Handled topup: $status for ${userProfile?.email}")
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun exchangeBalanceForPoints(uid: String, amountTk: Int): Result<Unit> {
        return try {
            val userRef = db.collection("users").document(uid)
            val userProfile = db.collection("users").document(uid).get().await().toObject(User::class.java)
            val points = amountTk.toDouble() * 10.0 
            db.runBatch { batch ->
                batch.update(userRef, "earnedBalance", FieldValue.increment(-amountTk.toLong()))
                batch.update(userRef, "pointsBalance", FieldValue.increment(points))
                val transId = db.collection("transactions").document().id
                batch.set(db.collection("transactions").document(transId), Transaction(
                    id = transId, uid = uid, userEmail = userProfile?.email ?: "", 
                    amount = amountTk, type = "exchange", description = "Exchanged for $points points", timestamp = Timestamp.now()
                ))
            }.await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun claimDailyReward(uid: String): Result<Int> {
        return try {
            val userRef = db.collection("users").document(uid)
            val user = userRef.get().await().toObject(User::class.java) ?: return Result.failure(Exception("User not found"))
            val now = Calendar.getInstance()
            val lastClaim = Calendar.getInstance()
            user.lastDailyRewardClaimed?.let { lastClaim.time = it.toDate() }
            if (user.lastDailyRewardClaimed != null && now.get(Calendar.DAY_OF_YEAR) == lastClaim.get(Calendar.DAY_OF_YEAR) && now.get(Calendar.YEAR) == lastClaim.get(Calendar.YEAR)) {
                return Result.failure(Exception("Already claimed today"))
            }
            val reward = 5
            userRef.update("pointsBalance", FieldValue.increment(reward.toLong()), "lastDailyRewardClaimed", Timestamp.now()).await()
            Result.success(reward)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun applyReferral(uid: String, referralCode: String): Result<Unit> {
        return try {
            val referrerRef = db.collection("users").document(referralCode)
            val referrerDoc = referrerRef.get().await()
            if (!referrerDoc.exists()) return Result.failure(Exception("Invalid referral code"))
            val userRef = db.collection("users").document(uid)
            val user = userRef.get().await().toObject(User::class.java) ?: return Result.failure(Exception("User not found"))
            if (user.referredBy != null) return Result.failure(Exception("Already used a referral"))
            db.runBatch { batch ->
                batch.update(referrerRef, "pointsBalance", FieldValue.increment(50.0))
                batch.update(referrerRef, "referralCount", FieldValue.increment(1))
                batch.update(userRef, "pointsBalance", FieldValue.increment(50.0))
                batch.update(userRef, "referredBy", referralCode)
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addNewQuestion(question: Question, adminUid: String, adminName: String): Result<Unit> {
        return try {
            val docId = question.question.hashCode().toString()
            val doc = db.collection("questions").document(docId).get().await()
            if (doc.exists()) return Result.failure(Exception("Question already exists!"))
            db.collection("questions").document(docId).set(question.copy(id = docId, addedBy = adminUid)).await()
            logAdminActivity(adminUid, adminName, "add_question", docId, "Added question: ${question.question.take(30)}...")
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun updateQuestion(question: Question, adminUid: String, adminName: String): Result<Unit> {
        return try {
            db.collection("questions").document(question.id).set(question).await()
            logAdminActivity(adminUid, adminName, "edit_question", question.id, "Edited question: ${question.question.take(30)}...")
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun getWrongAnswers(uid: String): List<Question> {
        return try {
            db.collection("users").document(uid).collection("wrongAnswers").get().await().toObjects(Question::class.java)
        } catch (e: Exception) { emptyList() }
    }

    suspend fun createRoom(uid: String, questionIds: List<String>): String {
        val roomCode = (100000..999999).random().toString()
        val room = Room(id = roomCode, creatorUid = uid, players = listOf(uid), entryFee = 20, questionIds = questionIds)
        db.collection("rooms").document(roomCode).set(room).await()
        return roomCode
    }

    suspend fun joinRoom(uid: String, roomCode: String): Result<Unit> {
        return try {
            val roomRef = db.collection("rooms").document(roomCode)
            val room = roomRef.get().await().toObject(Room::class.java) ?: return Result.failure(Exception("Room not found"))
            if (room.players.size >= 5) return Result.failure(Exception("Room full"))
            val userRef = db.collection("users").document(uid)
            val user = userRef.get().await().toObject(User::class.java) ?: return Result.failure(Exception("User not found"))
            if (user.pointsBalance < room.entryFee) return Result.failure(Exception("Insufficient points"))
            db.runBatch { batch ->
                batch.update(roomRef, "players", FieldValue.arrayUnion(uid))
                batch.update(userRef, "pointsBalance", FieldValue.increment(-room.entryFee.toDouble()))
            }.await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    private fun isLeaderboardFrozen(type: String): Boolean {
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        val maxDayOfMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        if (type == "weekly" && dayOfWeek == Calendar.FRIDAY && hour == 23 && minute >= 30) return true
        if (type == "monthly" && dayOfMonth == maxDayOfMonth && hour == 23 && minute >= 30) return true
        return false
    }

    suspend fun updateGlobalScore(uid: String, score: Int) {
        try {
            val userRef = db.collection("users").document(uid)
            val user = userRef.get().await().toObject(User::class.java) ?: return
            val now = Timestamp.now(); val calendar = Calendar.getInstance()
            val today = calendar.get(Calendar.DAY_OF_YEAR); val thisWeek = calendar.get(Calendar.WEEK_OF_YEAR); val thisMonth = calendar.get(Calendar.MONTH)
            val updates = mutableMapOf<String, Any>()
            val lastDailyCal = Calendar.getInstance()
            user.lastDailyReset?.let { lastDailyCal.time = it.toDate() }
            if (user.lastDailyReset == null || lastDailyCal.get(Calendar.DAY_OF_YEAR) != today) {
                updates["dailyScore"] = score; updates["lastDailyReset"] = now
            } else { updates["dailyScore"] = FieldValue.increment(score.toLong()) }
            if (!isLeaderboardFrozen("weekly")) {
                val lastWeeklyCal = Calendar.getInstance()
                user.lastWeeklyReset?.let { lastWeeklyCal.time = it.toDate() }
                if (user.lastWeeklyReset == null || lastWeeklyCal.get(Calendar.WEEK_OF_YEAR) != thisWeek) {
                    updates["weeklyScore"] = score; updates["lastWeeklyReset"] = now
                } else { updates["weeklyScore"] = FieldValue.increment(score.toLong()) }
            }
            if (!isLeaderboardFrozen("monthly")) {
                val lastMonthlyCal = Calendar.getInstance()
                user.lastMonthlyReset?.let { lastMonthlyCal.time = it.toDate() }
                if (user.lastMonthlyReset == null || lastMonthlyCal.get(Calendar.MONTH) != thisMonth) {
                    updates["monthlyScore"] = score; updates["lastMonthlyReset"] = now
                } else { updates["monthlyScore"] = FieldValue.increment(score.toLong()) }
            }
            userRef.update(updates).await()
        } catch (e: Exception) { }
    }

    suspend fun claimRankReward(uid: String, type: String): Result<Int> {
        return try {
            val calendar = Calendar.getInstance(); val now = calendar.timeInMillis; val fiveDaysMs = 5L * 24 * 60 * 60 * 1000
            val lastResetTime = when(type) {
                "weekly" -> { calendar.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY); calendar.set(Calendar.HOUR_OF_DAY, 23); calendar.set(Calendar.MINUTE, 30)
                    if (calendar.timeInMillis > now) calendar.add(Calendar.WEEK_OF_YEAR, -1); calendar.timeInMillis }
                else -> { calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH)); calendar.set(Calendar.HOUR_OF_DAY, 23); calendar.set(Calendar.MINUTE, 30)
                    if (calendar.timeInMillis > now) calendar.add(Calendar.MONTH, -1); calendar.timeInMillis }
            }
            if (now - lastResetTime > fiveDaysMs) return Result.failure(Exception("Claim period expired!"))
            val field = if (type == "weekly") "weeklyScore" else "monthlyScore"
            val topUsers = db.collection("users").orderBy(field, Query.Direction.DESCENDING).limit(1).get().await().toObjects(User::class.java)
            if (topUsers.isEmpty() || topUsers[0].uid != uid) return Result.failure(Exception("You are not the Rank #1 winner"))
            val amount = if (type == "weekly") 100 else 250
            val userRef = db.collection("users").document(uid)
            db.runBatch { batch ->
                batch.update(userRef, "earnedBalance", FieldValue.increment(amount.toLong()))
                val periodId = if (type == "weekly") Calendar.getInstance().get(Calendar.WEEK_OF_YEAR) else Calendar.getInstance().get(Calendar.MONTH)
                val claimId = "claim_${uid}_${type}_${periodId}"
                batch.set(db.collection("transactions").document(claimId), Transaction(id = claimId, uid = uid, userEmail = topUsers[0].email, amount = amount, type = "reward", description = "${type.replaceFirstChar { it.uppercase() }} Top Reward", timestamp = Timestamp.now()))
            }.await()
            Result.success(amount)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun logAdminActivity(adminUid: String, adminName: String, type: String, targetId: String, desc: String) {
        try {
            if (type == "revenue" || type == "topup" || type == "withdraw") return // Skip revenue-related logs for non-owners
            val id = db.collection("admin_activities").document().id
            val activity = AdminActivity(id, adminUid, adminName, type, targetId, desc, Timestamp.now())
            db.collection("admin_activities").document(id).set(activity).await()
        } catch (e: Exception) { }
    }

    suspend fun getAdminActivities(adminUid: String, isOwner: Boolean): List<AdminActivity> {
        return try {
            val query = if (adminUid.isEmpty()) db.collection("admin_activities") else db.collection("admin_activities").whereEqualTo("adminUid", adminUid)
            // Client-side sort to avoid index requirement
            val snapshot = query.get().await()
            var list = snapshot.toObjects(AdminActivity::class.java)
            if (!isOwner) {
                list = list.filter { it.actionType !in listOf("handle_topup", "handle_withdrawal") }
            }
            list.sortedByDescending { it.timestamp }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun submitReviewRequest(request: ReviewRequest): Result<Unit> {
        return try {
            val id = db.collection("reviews").document().id
            db.collection("reviews").document(id).set(request.copy(id = id, timestamp = Timestamp.now())).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun submitTopupRequest(request: TopupRequest): Result<Unit> {
        return try {
            val id = db.collection("topups").document().id
            db.collection("topups").document(id).set(request.copy(id = id, timestamp = Timestamp.now())).await()
            Result.success(Unit)
        } catch (e: Exception) { Result.failure(e) }
    }

    suspend fun updateQuestionStats(uid: String, categoryId: String, isCorrect: Boolean) {
        try {
            val userRef = db.collection("users").document(uid)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(userRef)
                val user = snapshot.toObject(User::class.java) ?: return@runTransaction
                val updates = mutableMapOf<String, Any>(
                    "totalAnswers" to FieldValue.increment(1),
                    "topicTotalAnswers.$categoryId" to FieldValue.increment(1)
                )
                if (isCorrect) {
                    updates["correctAnswers"] = FieldValue.increment(1)
                    updates["topicCorrectAnswers.$categoryId"] = FieldValue.increment(1)
                    if ((user.correctAnswers + 1) % 10 == 0) {
                        updates["pointsBalance"] = FieldValue.increment(1.0)
                    }
                }
                transaction.update(userRef, updates)
            }.await()
        } catch (e: Exception) { }
    }
}
