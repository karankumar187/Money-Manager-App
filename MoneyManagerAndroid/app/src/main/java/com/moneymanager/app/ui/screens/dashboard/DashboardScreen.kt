package com.moneymanager.app.ui.screens.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moneymanager.app.data.models.*
import com.moneymanager.app.ui.theme.*
import com.moneymanager.app.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigateHistory: () -> Unit,
    onNavigateLendBorrow: () -> Unit,
    onNavigatePayment: () -> Unit,
    onNavigateProfile: () -> Unit
) {
    val transactions  by viewModel.transactions.collectAsState()
    val lendBorrows   by viewModel.lendBorrows.collectAsState()
    val profile       by viewModel.userProfile.collectAsState()
    val monthlyBudget by viewModel.monthlyBudget.collectAsState()
    val currency      by viewModel.currencySymbol.collectAsState()
    val recentTxs = transactions.take(5)
    val fmt = SimpleDateFormat("d MMM", Locale.getDefault())

    Box(Modifier.fillMaxSize().background(BgPrimary)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .systemBarsPadding()
        ) {
            // ── Header ──────────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Good ${greeting()},", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Text(
                        profile?.displayName?.split(" ")?.firstOrNull() ?: "Friend",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = TextPrimary
                    )
                }
                IconButton(
                    onClick = onNavigateProfile,
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(BgCard)
                ) {
                    Icon(Icons.Default.Person, contentDescription = "Profile", tint = Accent1)
                }
            }

            // ── Budget Card ─────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                colors = CardDefaults.cardColors(containerColor = BgCard),
                shape = RoundedCornerShape(24.dp)
            ) {
                Box(
                    Modifier.fillMaxWidth().background(
                        Brush.linearGradient(listOf(Color(0xFF2A1215), BgCard))
                    ).padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Column {
                                Text("THIS MONTH", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text(viewModel.formatted(viewModel.thisMonthTotal),
                                    fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("BUDGET", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text(viewModel.formatted(monthlyBudget), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                            }
                        }

                        // Budget progress bar
                        val progress = viewModel.budgetFraction.toFloat()
                        val barColor = when {
                            progress >= 0.9f -> ExpenseRed
                            progress >= 0.7f -> CatOrange
                            else -> IncomeGreen
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                                color = barColor,
                                trackColor = BgCardAlt
                            )
                            Text(
                                "${(progress * 100).toInt()}% of budget used",
                                style = MaterialTheme.typography.bodySmall,
                                color = barColor
                            )
                        }

                        // Quick stats row
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatChip("Today",  viewModel.formatted(viewModel.todayTotal), IncomeGreen)
                            StatChip("Lent",   viewModel.formatted(viewModel.totalLent), AccentBlue)
                            StatChip("Owed",   viewModel.formatted(viewModel.totalBorrowed), ExpenseRed)
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Quick Actions ───────────────────────────────────────
            Text("QUICK ACTIONS", style = MaterialTheme.typography.labelSmall, color = TextSecondary,
                modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionButton(Icons.Default.Payment, "Pay", Accent1, Modifier.weight(1f), onNavigatePayment)
                QuickActionButton(Icons.Default.History, "History", AccentBlue, Modifier.weight(1f), onNavigateHistory)
                QuickActionButton(Icons.Default.PeopleAlt, "Lends", IncomeGreen, Modifier.weight(1f), onNavigateLendBorrow)
            }

            Spacer(Modifier.height(24.dp))

            // ── Lend / Borrow Summary ─────────────────────────────
            if (viewModel.totalLent > 0 || viewModel.totalBorrowed > 0) {
                Text("BALANCES", style = MaterialTheme.typography.labelSmall, color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 20.dp))
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (viewModel.totalLent > 0)
                        BalanceCard("You Lent", viewModel.formatted(viewModel.totalLent), IncomeGreen, Modifier.weight(1f))
                    if (viewModel.totalBorrowed > 0)
                        BalanceCard("You Owe", viewModel.formatted(viewModel.totalBorrowed), ExpenseRed, Modifier.weight(1f))
                }
                Spacer(Modifier.height(24.dp))
            }

            // ── Recent Transactions ──────────────────────────────
            if (recentTxs.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("RECENT", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    TextButton(onClick = onNavigateHistory) {
                        Text("See all", color = Accent1, fontSize = 13.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Column(
                    Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    recentTxs.forEach { tx ->
                        RecentTxRow(tx, viewModel.formatted(tx.amount), fmt.format(tx.date))
                    }
                }
            }

            Spacer(Modifier.height(100.dp))
        }
    }
}

@Composable
private fun StatChip(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = color)
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontSize = 10.sp)
    }
}

@Composable
private fun QuickActionButton(icon: ImageVector, label: String, color: Color, modifier: Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier.height(72.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = TextPrimary)
        }
    }
}

@Composable
private fun BalanceCard(label: String, value: String, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.7f))
            Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = color)
        }
    }
}

@Composable
private fun RecentTxRow(tx: com.moneymanager.app.data.models.Transaction, amount: String, date: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = BgCard),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(38.dp).clip(RoundedCornerShape(10.dp))
                    .background(hexColor(tx.categoryHex).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(tx.categoryEmoji.ifEmpty { "💳" }, fontSize = 18.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(tx.recipientName.ifEmpty { tx.categoryName }, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimary, maxLines = 1)
                Text(tx.note.ifEmpty { date }, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("−$amount", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ExpenseRed)
                Text(date, style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontSize = 11.sp)
            }
        }
    }
}

private fun greeting(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 5..11  -> "Morning"
    in 12..17 -> "Afternoon"
    else      -> "Evening"
}
