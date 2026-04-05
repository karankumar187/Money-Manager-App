package com.moneymanager.app.navigation

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.moneymanager.app.ui.screens.auth.AuthScreen
import com.moneymanager.app.ui.screens.dashboard.DashboardScreen
import com.moneymanager.app.ui.screens.history.HistoryScreen
import com.moneymanager.app.ui.screens.lendborrow.LendBorrowScreen
import com.moneymanager.app.ui.screens.lendborrow.PersonDetailScreen
import com.moneymanager.app.ui.screens.payment.PaymentScreen
import com.moneymanager.app.ui.screens.profile.ProfileScreen
import com.moneymanager.app.ui.theme.*
import com.moneymanager.app.ui.viewmodel.MainViewModel

object Routes {
    const val AUTH          = "auth"
    const val DASHBOARD     = "dashboard"
    const val ANALYTICS     = "analytics"
    const val LEND_BORROW   = "lend_borrow"
    const val HISTORY       = "history"
    const val PERSON_DETAIL = "person/{name}?phone={phone}"
    const val PAYMENT       = "payment?upi={upi}&name={name}&amount={amount}"
    const val PROFILE       = "profile"

    fun personDetail(name: String, phone: String? = null) =
        "person/${android.net.Uri.encode(name)}?phone=${phone ?: ""}"
    fun payment(upi: String = "", name: String = "", amount: Double = 0.0) =
        "payment?upi=${android.net.Uri.encode(upi)}&name=${android.net.Uri.encode(name)}&amount=$amount"
}

private const val BASE_URL = "https://moneymanager.web.app"

// Tabs matching iOS exactly: house | chart.bar | [₹ FAB] | person.2 | clock
private val TAB_ROUTES = listOf(Routes.DASHBOARD, Routes.ANALYTICS, Routes.LEND_BORROW, Routes.HISTORY)

private val TAB_SCREENS = setOf(
    Routes.DASHBOARD, Routes.ANALYTICS, Routes.LEND_BORROW, Routes.HISTORY
)

@Composable
fun NavGraph(
    navController: NavHostController,
    viewModel: MainViewModel,
    isSignedIn: Boolean,
    startDestination: String = if (isSignedIn) Routes.DASHBOARD else Routes.AUTH
) {
    val backStack     by navController.currentBackStackEntryAsState()
    val currentRoute  = backStack?.destination?.route?.substringBefore("?")
    var showPayment   by remember { mutableStateOf(false) }

    // Only show tab bar on main screens
    val isTabScreen = currentRoute in TAB_SCREENS

    Box(Modifier.fillMaxSize().background(BgPrimary)) {
        NavHost(
            navController    = navController,
            startDestination = startDestination,
            modifier         = Modifier.fillMaxSize()
        ) {

            // ── Auth ─────────────────────────────────────────────────────
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

            // ── Dashboard ────────────────────────────────────────────────
            composable(Routes.DASHBOARD) {
                DashboardScreen(
                    viewModel            = viewModel,
                    onNavigateHistory    = { navController.navigate(Routes.HISTORY) },
                    onNavigateLendBorrow = { navController.navigate(Routes.LEND_BORROW) },
                    onNavigatePayment    = { showPayment = true },
                    onNavigateProfile    = { navController.navigate(Routes.PROFILE) }
                )
            }

            // ── Analytics (stub — same as iOS placeholder) ───────────────
            composable(Routes.ANALYTICS) {
                // Analytics screen placeholder
                Box(Modifier.fillMaxSize().background(BgPrimary), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.BarChart, null, tint = TextSecondary, modifier = Modifier.size(56.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("Analytics", fontSize = 24.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = TextPrimary)
                        Text("Coming soon", fontSize = 14.sp, color = TextSecondary)
                    }
                }
            }

            // ── Lend / Borrow ────────────────────────────────────────────
            composable(Routes.LEND_BORROW) {
                LendBorrowScreen(
                    viewModel   = viewModel,
                    onPersonTap = { name, phone -> navController.navigate(Routes.personDetail(name, phone)) },
                    onBack      = { navController.popBackStack() }
                )
            }

            // ── History ──────────────────────────────────────────────────
            composable(Routes.HISTORY) {
                HistoryScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
            }

            // ── Person Detail ────────────────────────────────────────────
            composable(
                route     = Routes.PERSON_DETAIL,
                arguments = listOf(
                    navArgument("name")  { type = NavType.StringType },
                    navArgument("phone") { type = NavType.StringType; defaultValue = "" }
                ),
                deepLinks = listOf(navDeepLink { uriPattern = "$BASE_URL/contact?name={name}&phone={phone}" })
            ) { back ->
                val name  = back.arguments?.getString("name").orEmpty()
                val phone = back.arguments?.getString("phone")?.takeIf { it.isNotEmpty() }
                PersonDetailScreen(
                    personName        = name, phone = phone, viewModel = viewModel,
                    onNavigatePayment = { upi, n, amt -> navController.navigate(Routes.payment(upi ?: "", n, amt)) },
                    onBack            = { navController.popBackStack() }
                )
            }

            // ── Payment ──────────────────────────────────────────────────
            composable(
                route     = Routes.PAYMENT,
                arguments = listOf(
                    navArgument("upi")    { type = NavType.StringType; defaultValue = "" },
                    navArgument("name")   { type = NavType.StringType; defaultValue = "" },
                    navArgument("amount") { type = NavType.StringType; defaultValue = "0" }
                ),
                deepLinks = listOf(
                    navDeepLink { uriPattern = "upi://pay?pa={upi}&pn={name}&am={amount}&cu=INR" },
                    navDeepLink { uriPattern = "upi://pay?pa={upi}&pn={name}&am={amount}" },
                    navDeepLink { uriPattern = "$BASE_URL/pay?pa={upi}&pn={name}&am={amount}" }
                )
            ) { back ->
                val upi    = back.arguments?.getString("upi").orEmpty()
                val name   = back.arguments?.getString("name").orEmpty()
                val amount = back.arguments?.getString("amount")?.toDoubleOrNull() ?: 0.0
                PaymentScreen(
                    prefilledUpi = upi, prefilledName = name, prefilledAmount = amount,
                    viewModel = viewModel, onBack = { navController.popBackStack() }
                )
            }

            // ── Profile ──────────────────────────────────────────────────
            composable(Routes.PROFILE) {
                ProfileScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
            }
        }

        // ── iOS-style floating capsule tab bar ───────────────────────────
        if (isTabScreen) {
            IosTabBar(
                currentRoute = currentRoute ?: Routes.DASHBOARD,
                modifier     = Modifier.align(Alignment.BottomCenter),
                onTabSelected = { route ->
                    if (route != currentRoute) {
                        navController.navigate(route) {
                            popUpTo(Routes.DASHBOARD) { saveState = true }
                            launchSingleTop = true
                            restoreState    = true
                        }
                    }
                },
                onPayTap = { showPayment = true }
            )
        }

        // Payment bottom sheet (modal, like iOS)
        if (showPayment) {
            PaymentScreen(
                prefilledUpi = "", prefilledName = "", prefilledAmount = 0.0,
                viewModel = viewModel,
                onBack = { showPayment = false }
            )
        }
    }
}

// ── iOS MinimalTabBar — floating capsule with center ₹ FAB ──────────────────
@Composable
fun IosTabBar(
    currentRoute: String,
    modifier: Modifier = Modifier,
    onTabSelected: (String) -> Unit,
    onPayTap: () -> Unit
) {
    Row(
        modifier
            .padding(horizontal = 24.dp)
            .padding(bottom = 16.dp)
            .shadow(20.dp, RoundedCornerShape(50), spotColor = Color.Black.copy(0.5f))
            .clip(RoundedCornerShape(50))
            .background(BgCardAlt.copy(alpha = 0.85f))
            .border(BorderStroke(1.dp, Color.White.copy(0.1f)), RoundedCornerShape(50))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left side: house + chart.bar
        TabItem(Icons.Default.Home, "house", currentRoute == Routes.DASHBOARD) {
            onTabSelected(Routes.DASHBOARD)
        }
        TabItem(Icons.Default.BarChart, "analytics", currentRoute == Routes.ANALYTICS) {
            onTabSelected(Routes.ANALYTICS)
        }

        // Center ₹ FAB — offset upward like iOS
        Box(Modifier.padding(horizontal = 8.dp)) {
            Box(
                Modifier
                    .size(54.dp)
                    .offset(y = (-14).dp)
                    .clip(CircleShape)
                    .background(Accent1)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onPayTap() },
                contentAlignment = Alignment.Center
            ) {
                Text("₹", fontSize = 22.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, color = Color.White)
            }
        }

        // Right side: person.2 + clock
        TabItem(Icons.Default.People, "lend", currentRoute == Routes.LEND_BORROW) {
            onTabSelected(Routes.LEND_BORROW)
        }
        TabItem(Icons.Default.AccessTime, "history", currentRoute == Routes.HISTORY) {
            onTabSelected(Routes.HISTORY)
        }
    }
}

@Composable
private fun RowScope.TabItem(
    icon: ImageVector,
    contentDesc: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        Modifier
            .weight(1f)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(
            icon, contentDescription = contentDesc,
            tint = if (isSelected) Color.White else Color(0xFF3A3A4A),
            modifier = Modifier.size(21.dp)
        )
        // Red dot indicator — matches iOS exactly
        Box(
            Modifier
                .size(4.dp)
                .clip(CircleShape)
                .background(if (isSelected) Accent1 else Color.Transparent)
        )
    }
}
