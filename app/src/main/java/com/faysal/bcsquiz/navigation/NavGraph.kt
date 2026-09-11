package com.faysal.bcsquiz.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.faysal.bcsquiz.data.remote.FirestoreService
import com.faysal.bcsquiz.data.repository.AuthRepository
import com.faysal.bcsquiz.data.repository.QuizRepository
import com.faysal.bcsquiz.ui.ViewModelFactory
import com.faysal.bcsquiz.ui.contest.ContestListScreen
import com.faysal.bcsquiz.ui.contest.ContestPlayScreen
import com.faysal.bcsquiz.ui.contest.ContestPlayViewModel
import com.faysal.bcsquiz.ui.contest.ContestViewModel
import com.faysal.bcsquiz.ui.contest.LeaderboardScreen
import com.faysal.bcsquiz.ui.contest.LeaderboardViewModel
import com.faysal.bcsquiz.ui.home.DashboardScreen
import com.faysal.bcsquiz.ui.home.HomeViewModel
import com.faysal.bcsquiz.ui.login.LoginScreen
import com.faysal.bcsquiz.ui.login.LoginViewModel
import com.faysal.bcsquiz.ui.quiz.QuizPlayScreen
import com.faysal.bcsquiz.ui.quiz.QuizViewModel
import com.faysal.bcsquiz.ui.history.HistoryScreen
import com.faysal.bcsquiz.ui.profile.ProfileScreen
import com.faysal.bcsquiz.ui.room.RoomScreen
import com.faysal.bcsquiz.ui.room.RoomViewModel
import com.faysal.bcsquiz.ui.wallet.WalletScreen
import com.google.firebase.auth.FirebaseAuth

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Categories : Screen("categories")
    object SubCategories : Screen("subcategories/{categoryId}") {
        fun createRoute(categoryId: String) = "subcategories/$categoryId"
    }
    object Quiz : Screen("quiz/{categoryId}/{subCategoryId}") {
        fun createRoute(categoryId: String, subCategoryId: String? = null) = 
            "quiz/$categoryId" + (if (subCategoryId != null) "/$subCategoryId" else "/none")
    }
    object Result : Screen("result/{score}/{total}") {
        fun createRoute(score: Int, total: Int) = "result/$score/$total"
    }
    object Login : Screen("login")
    object Contest : Screen("contest")
    object ContestPlay : Screen("contest_play/{contestId}/{questionIds}") {
        fun createRoute(contestId: String, questionIds: List<String>) = 
            "contest_play/$contestId/${questionIds.joinToString(",")}"
    }
    object Leaderboard : Screen("leaderboard")
    object History : Screen("history")
    object Room : Screen("room")
    object Wallet : Screen("wallet")
    object Profile : Screen("profile")
    object AdminPanel : Screen("admin_panel/{tab}?editId={editId}") {
        fun createRoute(tab: Int, editId: String? = null) = 
            "admin_panel/$tab" + if (editId != null) "?editId=$editId" else ""
    }
    object StaffManagement : Screen("staff_management")
    object FinanceManagement : Screen("finance_management")
    object CorrectionRequests : Screen("correction_requests")
    object Subscription : Screen("subscription")
}

@Composable
fun NavGraph(navController: NavHostController) {
    val context = LocalContext.current
    val firestoreService = FirestoreService()
    val quizRepository = QuizRepository(firestoreService)
    val authRepository = AuthRepository(context)
    
    val currentUser = FirebaseAuth.getInstance().currentUser
    val startDestination = if (currentUser == null) Screen.Login.route else Screen.Dashboard.route

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            val viewModel: LoginViewModel = viewModel(factory = ViewModelFactory(authRepository = authRepository))
            LoginScreen(viewModel = viewModel) {
                navController.navigate(Screen.Dashboard.route) { popUpTo(Screen.Login.route) { inclusive = true } }
            }
        }
        composable(Screen.Dashboard.route) {
            val viewModel: HomeViewModel = viewModel(factory = ViewModelFactory(quizRepository = quizRepository))
            val loginViewModel: LoginViewModel = viewModel(factory = ViewModelFactory(authRepository = authRepository))
            DashboardScreen(
                viewModel = viewModel,
                onNavigateToCategories = { navController.navigate(Screen.Categories.route) },
                onNavigateToContests = { navController.navigate(Screen.Contest.route) },
                onNavigateToLeaderboard = { navController.navigate(Screen.Leaderboard.route) },
                onNavigateToHistory = { navController.navigate(Screen.History.route) },
                onNavigateToSubscription = { navController.navigate(Screen.Subscription.route) },
                onNavigateToWallet = { navController.navigate(Screen.Wallet.route) },
                onNavigateToRoom = { navController.navigate(Screen.Room.route) },
                onNavigateToAdmin = { tab -> navController.navigate(Screen.AdminPanel.createRoute(tab)) },
                onNavigateToStaff = { navController.navigate(Screen.StaffManagement.route) },
                onNavigateToFinance = { navController.navigate(Screen.FinanceManagement.route) },
                onNavigateToReviews = { navController.navigate(Screen.CorrectionRequests.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onLogout = {
                    loginViewModel.signOut()
                    navController.navigate(Screen.Login.route) { popUpTo(Screen.Dashboard.route) { inclusive = true } }
                }
            )
        }
        composable(Screen.Categories.route) {
            val viewModel: HomeViewModel = viewModel(factory = ViewModelFactory(quizRepository = quizRepository))
            com.faysal.bcsquiz.ui.home.HomeScreen(viewModel = viewModel, onContestClick = { navController.navigate(Screen.Contest.route) }) {
                navController.navigate(Screen.SubCategories.createRoute(it))
            }
        }
        composable(Screen.SubCategories.route) { backStackEntry ->
            val catId = backStackEntry.arguments?.getString("categoryId") ?: ""
            com.faysal.bcsquiz.ui.quiz.SubCategoryScreen(categoryId = catId, repository = quizRepository, onBack = { navController.popBackStack() }) { subId ->
                navController.navigate(Screen.Quiz.createRoute(catId, subId))
            }
        }
        composable(Screen.Quiz.route) { backStackEntry ->
            val catId = backStackEntry.arguments?.getString("categoryId") ?: ""
            val subId = backStackEntry.arguments?.getString("subCategoryId").let { if (it == "none") null else it }
            val viewModel: QuizViewModel = viewModel(factory = ViewModelFactory(quizRepository = quizRepository, categoryId = catId, subCategoryId = subId))
            QuizPlayScreen(viewModel = viewModel) { s, t ->
                navController.navigate(Screen.Result.createRoute(s, t)) { popUpTo(Screen.Dashboard.route) { inclusive = false } }
            }
        }
        composable(Screen.History.route) {
            HistoryScreen(repository = quizRepository)
        }
        composable(Screen.Profile.route) {
            ProfileScreen(repository = quizRepository) { navController.popBackStack() }
        }
        composable(Screen.Result.route) { backStackEntry ->
            val s = backStackEntry.arguments?.getString("score") ?: "0"
            val t = backStackEntry.arguments?.getString("total") ?: "0"
            Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("Quiz Complete!", style = MaterialTheme.typography.headlineMedium)
                Text("Your Score: $s / $t", style = MaterialTheme.typography.bodyLarge)
                Button(onClick = { navController.navigate(Screen.Dashboard.route) { popUpTo(Screen.Dashboard.route) { inclusive = true } } }, modifier = Modifier.padding(top = 16.dp)) { Text("Back to Home") }
            }
        }
        composable(Screen.Contest.route) {
            val viewModel: ContestViewModel = viewModel(factory = ViewModelFactory(quizRepository = quizRepository))
            ContestListScreen(viewModel = viewModel, onNavigateToSubscription = { navController.navigate(Screen.Subscription.route) }) {
                navController.navigate(Screen.ContestPlay.createRoute(it.id, it.questionIds))
            }
        }
        composable(Screen.ContestPlay.route) { backStackEntry ->
            val cId = backStackEntry.arguments?.getString("contestId") ?: ""
            val qIds = backStackEntry.arguments?.getString("questionIds")?.split(",") ?: emptyList()
            val viewModel: ContestPlayViewModel = viewModel(factory = ViewModelFactory(quizRepository = quizRepository, contestId = cId, questionIds = qIds))
            ContestPlayScreen(viewModel = viewModel) { _, _ ->
                navController.navigate(Screen.Leaderboard.route) { popUpTo(Screen.Dashboard.route) { inclusive = false } }
            }
        }
        composable(Screen.Leaderboard.route) {
            val viewModel: LeaderboardViewModel = viewModel(factory = ViewModelFactory(quizRepository = quizRepository))
            LeaderboardScreen(viewModel = viewModel)
        }
        composable(Screen.Room.route) {
            val viewModel: RoomViewModel = viewModel(factory = ViewModelFactory(quizRepository = quizRepository))
            RoomScreen(viewModel = viewModel, quizRepository = quizRepository) { rId, qIds ->
                navController.navigate(Screen.ContestPlay.createRoute(rId, qIds)) { popUpTo(Screen.Room.route) { inclusive = true } }
            }
        }
        composable(Screen.Wallet.route) {
            WalletScreen(
                repository = quizRepository,
                onNavigateToSubscription = { navController.navigate(Screen.Subscription.route) }
            ) { navController.popBackStack() }
        }
        composable(Screen.AdminPanel.route) { backStackEntry ->
            val tab = backStackEntry.arguments?.getString("tab")?.toIntOrNull() ?: 0
            val editId = backStackEntry.arguments?.getString("editId")
            com.faysal.bcsquiz.ui.admin.AdminPanelScreen(initialTab = tab, initialEditId = editId) { 
                navController.popBackStack() 
            }
        }
        composable(Screen.StaffManagement.route) {
            com.faysal.bcsquiz.ui.admin.StaffManagementScreen { navController.popBackStack() }
        }
        composable(Screen.FinanceManagement.route) {
            com.faysal.bcsquiz.ui.admin.FinanceManagementScreen(repository = quizRepository) { navController.popBackStack() }
        }
        composable(Screen.CorrectionRequests.route) {
            com.faysal.bcsquiz.ui.admin.CorrectionRequestScreen(repository = quizRepository, onCorrection = { qId ->
                navController.navigate(Screen.AdminPanel.createRoute(1)) // Navigate to Questions tab
            }) { navController.popBackStack() }
        }
        composable(Screen.Subscription.route) { 
            val billingRepository = com.faysal.bcsquiz.data.repository.BillingRepository(context, quizRepository)
            val viewModel: com.faysal.bcsquiz.ui.subscription.SubscriptionViewModel = viewModel(factory = ViewModelFactory(billingRepository = billingRepository))
            com.faysal.bcsquiz.ui.subscription.SubscriptionScreen(viewModel = viewModel) { navController.popBackStack() }
        }
    }
}
