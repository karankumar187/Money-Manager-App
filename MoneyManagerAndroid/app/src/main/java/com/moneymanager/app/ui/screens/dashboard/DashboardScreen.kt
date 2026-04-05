package com.moneymanager.app.ui.screens.dashboard

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.moneymanager.app.data.models.Transaction
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
    val recentTxs     = transactions.sortedByDescending { it.date }.take(5)
    val categories    by viewModel.categories.collectAsState()
    val dateFmt       = SimpleDateFormat("d MMM", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgPrimary)
            .verticalScroll(rememberScrollState())
            .systemBarsPadding()
    ) {
        // ── Header ── matches iOS: "Hello" + name + avatar circle ───────
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp, bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Hello",
                    fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextSecondary
                )
                Text(
                    profile?.displayName?.split(" ")?.firstOrNull() ?: "Friend",
                    fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextPrimary
                )
            }

            // Avatar circle — profile pic or initial
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(BgCardAlt)
                    .border(BorderStroke(0.8.dp, Color.White.copy(0.06f)), CircleShape)
                    .clickable { onNavigateProfile() },
                contentAlignment = Alignment.Center
            ) {
                if (!profile?.profileImageURL.isNullOrEmpty()) {
                    AsyncImage(
                        model = profile!!.profileImageURL,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        (profile?.displayName?.firstOrNull()?.uppercaseChar()?.toString() ?: "M"),
                        fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary
                    )
                }
            }
        }

        // ── Monthly Spend Card — matches iOS "SPENDINGS THIS MONTH" ─────
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Color.Black.copy(alpha = 0.2f))
                .border(BorderStroke(1.dp, Color.White.copy(0.05f)), RoundedCornerShape(22.dp))
        ) {
            Column(
                Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text(
                    "SPENDINGS THIS MONTH",
                    fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                    color = TextSecondary, letterSpacing = 1.4.sp
                )

                Text(
                    viewModel.formatted(viewModel.thisMonthTotal),
                    fontSize = 42.sp, fontWeight = FontWeight.Bold, color = TextPrimary
                )

                // Budget progress bar
                val fraction = viewModel.budgetFraction.toFloat().coerceIn(0f, 1f)
                val barColor = if (fraction > 0.85f) ExpenseRed else IncomeGreen
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
                        color = barColor,
                        trackColor = Color.White.copy(alpha = 0.07f)
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            "Budget ${viewModel.formatted(monthlyBudget)}",
                            fontSize = 11.sp, color = TextSecondary
                        )
                        Text(
                            "${(fraction * 100).toInt()}% used",
                            fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                            color = if (fraction > 0.85f) ExpenseRed else TextSecondary
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // ── Stat mini cards — TODAY / THIS WEEK / AVG PER DAY ───────────
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatMiniCard("TODAY", viewModel.formatted(viewModel.todayTotal), Accent1, Modifier.weight(1f))
            StatMiniCard("THIS WEEK", viewModel.formatted(viewModel.thisWeekTotal), AccentBlue, Modifier.weight(1f))
            StatMiniCard("AVG / DAY", viewModel.formatted(viewModel.averageDailySpend), IncomeGreen, Modifier.weight(1f))
        }

        Spacer(Modifier.height(22.dp))

        // ── Lent / Borrowed card ─────────────────────────────────────────
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(BgCard)
                .border(BorderStroke(0.6.dp, Color.White.copy(0.055f)), RoundedCornerShape(22.dp))
                .clickable { onNavigateLendBorrow() }
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                    Text("LENT OUT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary, letterSpacing = 1.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(viewModel.formatted(viewModel.totalLent), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = IncomeGreen)
                }
                Box(Modifier.width(1.dp).height(40.dp).background(Color.White.copy(0.08f)))
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text("BORROWED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextSecondary, letterSpacing = 1.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(viewModel.formatted(viewModel.totalBorrowed), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ExpenseRed)
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        // ── Categories row — "This Month" ────────────────────────────────
        if (categories.isNotEmpty() && transactions.isNotEmpty()) {
            Text(
                "This Month",
                fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(Modifier.height(12.dp))

            val catSpend = transactions
                .filter { tx ->
                    val cal = Calendar.getInstance().apply { time = tx.date }
                    val now = Calendar.getInstance()
                    cal.get(Calendar.MONTH) == now.get(Calendar.MONTH) &&
                    cal.get(Calendar.YEAR) == now.get(Calendar.YEAR)
                }
                .groupBy { it.categoryName }
                .mapValues { it.value.sumOf { t -> t.amount } }
                .entries.sortedByDescending { it.value }.take(6)

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(catSpend) { (catName, amount) ->
                    val cat = categories.firstOrNull { it.name == catName }
                    val color = hexColor(cat?.colorHex ?: "#FF9F0A")
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(72.dp)
                    ) {
                        Box(
                            Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(color.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(cat?.emoji ?: "📦", fontSize = 24.sp)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            catName.split(" ").first(),
                            fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                            color = TextPrimary, maxLines = 1
                        )
                        Text(
                            viewModel.formatted(amount),
                            fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            color = color, maxLines = 1
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        // ── Recent Transactions ──────────────────────────────────────────
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Recent Transactions", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            TextButton(onClick = onNavigateHistory) {
                Text("See all", color = Accent1, fontSize = 13.sp)
            }
        }

        if (recentTxs.isEmpty()) {
            Box(
                Modifier.fillMaxWidth().padding(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No transactions yet. Tap ₹ to get started.",
                    fontSize = 14.sp, color = TextSecondary
                )
            }
        } else {
            Column(
                Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                recentTxs.forEach { tx ->
                    TxRow(tx = tx, viewModel = viewModel, date = dateFmt.format(tx.date))
                }
            }
        }

        Spacer(Modifier.height(120.dp))
    }
}

// ── Stat Mini Card (matches iOS StatMini exactly) ───────────────────────────
@Composable
fun StatMiniCard(label: String, value: String, dot: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(BgCard)
            .border(BorderStroke(0.6.dp, Color.White.copy(0.055f)), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSecondary, letterSpacing = 1.2.sp)
            Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1)
            Box(Modifier.size(8.dp).clip(CircleShape).background(dot))
        }
    }
}

// ── Transaction Row (matches iOS TxRow exactly) ──────────────────────────────
@Composable
fun TxRow(tx: Transaction, viewModel: MainViewModel, date: String) {
    val avatarColor = avatarColor(tx.recipientName)
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 0.dp)
            .clip(RoundedCornerShape(18.dp))
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Avatar circle with initial
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(avatarColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    tx.recipientName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    fontSize = 20.sp, fontWeight = FontWeight.Bold, color = avatarColor
                )
            }

            // Name + note
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(tx.recipientName, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 1)
                Text(
                    tx.note.ifEmpty { tx.categoryName },
                    fontSize = 12.sp, color = TextSecondary, maxLines = 1
                )
                if (tx.upiAppUsed != null) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Accent1.copy(0.15f))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(tx.upiAppUsed, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Accent1)
                    }
                }
            }

            // Amount + date
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("−${viewModel.formatted(tx.amount)}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ExpenseRed)
                Text(date, fontSize = 11.sp, color = TextSecondary)
            }
        }
    }
    HorizontalDivider(
        Modifier.padding(start = 76.dp, end = 14.dp),
        color = Color.White.copy(alpha = 0.04f),
        thickness = 0.5.dp
    )
}
