package com.moneymanager.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.moneymanager.app.ui.screens.auth.AuthScreen
import com.moneymanager.app.ui.screens.dashboard.DashboardScreen
import com.moneymanager.app.ui.screens.history.HistoryScreen
import com.moneymanager.app.ui.screens.lendborrow.LendBorrowScreen
import com.moneymanager.app.ui.screens.lendborrow.PersonDetailScreen
import com.moneymanager.app.ui.screens.payment.PaymentScreen
import com.moneymanager.app.ui.screens.profile.ProfileScreen
import com.moneymanager.app.ui.viewmodel.MainViewModel

object Routes {
    const val AUTH          = "auth"
    const val DASHBOARD     = "dashboard"
    const val HISTORY       = "history"
    const val LEND_BORROW   = "lend_borrow"
    const val PERSON_DETAIL = "person/{name}?phone={phone}"
    const val PAYMENT       = "payment?upi={upi}&name={name}&amount={amount}"
    const val PROFILE       = "profile"

    fun personDetail(name: String, phone: String? = null) =
        "person/${android.net.Uri.encode(name)}?phone=${phone ?: ""}"
    fun payment(upi: String = "", name: String = "", amount: Double = 0.0) =
        "payment?upi=${android.net.Uri.encode(upi)}&name=${android.net.Uri.encode(name)}&amount=$amount"
}

/** Base URL for App Links / Universal Deep Links */
private const val BASE_URL = "https://moneymanager.web.app"

@Composable
fun NavGraph(
    navController: NavHostController,
    viewModel: MainViewModel,
    isSignedIn: Boolean,
    startDestination: String = if (isSignedIn) Routes.DASHBOARD else Routes.AUTH
) {
    NavHost(navController = navController, startDestination = startDestination) {

        // ── Auth ────────────────────────────────────────────────────────
        composable(Routes.AUTH) {
            AuthScreen(
                onAuthSuccess = {
                    viewModel.startListening()
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.AUTH) { inclusive = true }
                    }
                }
            )
        }

        // ── Dashboard ───────────────────────────────────────────────────
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                viewModel = viewModel,
                onNavigateHistory    = { navController.navigate(Routes.HISTORY) },
                onNavigateLendBorrow = { navController.navigate(Routes.LEND_BORROW) },
                onNavigatePayment    = { navController.navigate(Routes.payment()) },
                onNavigateProfile    = { navController.navigate(Routes.PROFILE) }
            )
        }

        // ── History ─────────────────────────────────────────────────────
        composable(Routes.HISTORY) {
            HistoryScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }

        // ── Lend / Borrow ───────────────────────────────────────────────
        composable(Routes.LEND_BORROW) {
            LendBorrowScreen(
                viewModel = viewModel,
                onPersonTap = { name, phone ->
                    navController.navigate(Routes.personDetail(name, phone))
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ── Person Detail ───────────────────────────────────────────────
        composable(
            route = Routes.PERSON_DETAIL,
            arguments = listOf(
                navArgument("name")  { type = NavType.StringType },
                navArgument("phone") { type = NavType.StringType; defaultValue = "" }
            ),
            deepLinks = listOf(
                navDeepLink { uriPattern = "$BASE_URL/contact?name={name}&phone={phone}" }
            )
        ) { back ->
            val name  = back.arguments?.getString("name").orEmpty()
            val phone = back.arguments?.getString("phone")?.takeIf { it.isNotEmpty() }
            PersonDetailScreen(
                personName = name,
                phone      = phone,
                viewModel  = viewModel,
                onNavigatePayment = { upi, n, amt ->
                    navController.navigate(Routes.payment(upi ?: "", n, amt))
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ── Payment ─────────────────────────────────────────────────────
        // Handles both in-app navigation AND:
        // 1. UPI universal deep links:  upi://pay?pa=...&pn=...&am=...
        // 2. App Links:                 https://moneymanager.web.app/pay?pa=...
        composable(
            route = Routes.PAYMENT,
            arguments = listOf(
                navArgument("upi")    { type = NavType.StringType; defaultValue = "" },
                navArgument("name")   { type = NavType.StringType; defaultValue = "" },
                navArgument("amount") { type = NavType.StringType; defaultValue = "0" }
            ),
            deepLinks = listOf(
                // Standard UPI scheme — auto fills from any QR or link
                navDeepLink { uriPattern = "upi://pay?pa={upi}&pn={name}&am={amount}&cu=INR" },
                navDeepLink { uriPattern = "upi://pay?pa={upi}&pn={name}&am={amount}" },
                // App Link deep link
                navDeepLink { uriPattern = "$BASE_URL/pay?pa={upi}&pn={name}&am={amount}" }
            )
        ) { back ->
            val upi    = back.arguments?.getString("upi").orEmpty()
            val name   = back.arguments?.getString("name").orEmpty()
            val amount = back.arguments?.getString("amount")?.toDoubleOrNull() ?: 0.0
            PaymentScreen(
                prefilledUpi    = upi,
                prefilledName   = name,
                prefilledAmount = amount,
                viewModel       = viewModel,
                onBack          = { navController.popBackStack() }
            )
        }

        // ── Profile ─────────────────────────────────────────────────────
        composable(Routes.PROFILE) {
            ProfileScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
    }
}
