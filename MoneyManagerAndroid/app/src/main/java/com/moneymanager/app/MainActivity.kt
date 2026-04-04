package com.moneymanager.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.moneymanager.app.navigation.NavGraph
import com.moneymanager.app.navigation.Routes
import com.moneymanager.app.ui.theme.BgPrimary
import com.moneymanager.app.ui.theme.MoneyManagerTheme
import com.moneymanager.app.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = BgPrimary.toArgb()

        val isSignedIn = auth.currentUser != null
        if (isSignedIn) viewModel.startListening()

        setContent {
            MoneyManagerTheme {
                val navController = rememberNavController()

                // Handle deep links on cold start
                LaunchedEffect(Unit) {
                    handleDeepLink(intent, navController)
                }

                NavGraph(
                    navController    = navController,
                    viewModel        = viewModel,
                    isSignedIn       = isSignedIn
                )
            }
        }
    }

    // Handle deep links when app is already running (single task mode)
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun handleDeepLink(intent: Intent?, navController: androidx.navigation.NavController) {
        val data = intent?.data ?: return
        when {
            // UPI deep link: upi://pay?pa=...&pn=...&am=...
            data.scheme == "upi" -> {
                val upi    = data.getQueryParameter("pa") ?: return
                val name   = data.getQueryParameter("pn") ?: ""
                val amount = data.getQueryParameter("am")?.toDoubleOrNull() ?: 0.0
                navController.navigate(Routes.payment(upi, name, amount))
            }
            // App Link: https://moneymanager.web.app/pay?pa=...
            data.host == "moneymanager.web.app" && data.path?.startsWith("/pay") == true -> {
                val upi    = data.getQueryParameter("pa") ?: return
                val name   = data.getQueryParameter("pn") ?: ""
                val amount = data.getQueryParameter("am")?.toDoubleOrNull() ?: 0.0
                navController.navigate(Routes.payment(upi, name, amount))
            }
            // App Link: https://moneymanager.web.app/contact?name=...&phone=...
            data.host == "moneymanager.web.app" && data.path?.startsWith("/contact") == true -> {
                val name  = data.getQueryParameter("name") ?: return
                val phone = data.getQueryParameter("phone")
                navController.navigate(Routes.personDetail(name, phone))
            }
        }
    }
}
