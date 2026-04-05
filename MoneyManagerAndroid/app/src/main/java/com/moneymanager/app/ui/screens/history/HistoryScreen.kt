package com.moneymanager.app.ui.screens.history

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moneymanager.app.data.models.*
import com.moneymanager.app.ui.theme.*
import com.moneymanager.app.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val transactions by viewModel.transactions.collectAsState()
    val categories   by viewModel.categories.collectAsState()
    var search       by remember { mutableStateOf("") }
    var showFilters  by remember { mutableStateOf(false) }
    var filterCat    by remember { mutableStateOf<String?>(null) }

    val filtered = transactions
        .filter { tx ->
            val matchSearch = search.isEmpty()
                || tx.recipientName.contains(search, true)
                || tx.note.contains(search, true)
                || tx.categoryName.contains(search, true)
            val matchCat = filterCat == null || tx.categoryName == filterCat
            matchSearch && matchCat
        }
        .sortedByDescending { it.date }

    // Group by day — Today / Yesterday / date string
    val dayFmt = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
    val cal = Calendar.getInstance()

    data class Group(val label: String, val sortDate: Date, val items: List<Transaction>)

    val groups = filtered.groupBy { tx ->
        val c = Calendar.getInstance().apply { time = tx.date }
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
        c.time
    }.entries.sortedByDescending { it.key }.map { (day, txs) ->
        val isToday = run {
            val d = Calendar.getInstance().apply { time = day }
            val n = Calendar.getInstance()
            d.get(Calendar.DAY_OF_YEAR) == n.get(Calendar.DAY_OF_YEAR) && d.get(Calendar.YEAR) == n.get(Calendar.YEAR)
        }
        val isYesterday = run {
            val d = Calendar.getInstance().apply { time = day }
            val n = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
            d.get(Calendar.DAY_OF_YEAR) == n.get(Calendar.DAY_OF_YEAR) && d.get(Calendar.YEAR) == n.get(Calendar.YEAR)
        }
        val label = when { isToday -> "Today"; isYesterday -> "Yesterday"; else -> dayFmt.format(day) }
        Group(label, day, txs)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(BgPrimary)
            .systemBarsPadding()
    ) {
        // ── Header — matches iOS ──────────────────────────────────────────
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("History", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            IconButton(onClick = { showFilters = !showFilters }) {
                Icon(
                    if (filterCat != null) Icons.Default.FilterAlt else Icons.Default.FilterList,
                    null, tint = Accent1, modifier = Modifier.size(22.dp)
                )
            }
        }

        // ── Search bar — matches iOS glass card style ─────────────────────
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 10.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(BgCard)
                .border(BorderStroke(0.6.dp, Color.White.copy(0.055f)), RoundedCornerShape(16.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Default.Search, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
            androidx.compose.foundation.text.BasicTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = TextPrimary, fontSize = 15.sp
                ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(Accent1),
                decorationBox = { inner ->
                    if (search.isEmpty()) Text("Search by name, note, category…", color = TextSecondary, fontSize = 15.sp)
                    inner()
                }
            )
            if (search.isNotEmpty()) {
                IconButton(onClick = { search = "" }, modifier = Modifier.size(20.dp)) {
                    Icon(Icons.Default.Cancel, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
            }
        }

        // ── Filter pills — matches iOS scroll pill style ──────────────────
        AnimatedVisibility(visible = showFilters) {
            Row(
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (filterCat != null) {
                    Box(
                        Modifier
                            .clip(CircleShape)
                            .background(ExpenseRed.copy(0.15f))
                            .clickable { filterCat = null }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Close, null, tint = ExpenseRed, modifier = Modifier.size(12.dp))
                            Text("Clear", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = ExpenseRed)
                        }
                    }
                }
                categories.forEach { cat ->
                    val isSelected = filterCat == cat.name
                    val color = hexColor(cat.colorHex)
                    Box(
                        Modifier
                            .clip(CircleShape)
                            .background(if (isSelected) color.copy(0.7f) else BgCard)
                            .clickable { filterCat = if (isSelected) null else cat.name }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(cat.emoji, fontSize = 12.sp)
                            Text(
                                cat.name.split(" ").first(),
                                fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) Color.White else TextSecondary
                            )
                        }
                    }
                }
            }
        }

        // ── List ──────────────────────────────────────────────────────────
        when {
            transactions.isEmpty() -> {
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.CreditCard, null, tint = TextSecondary.copy(0.3f), modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("No expenses yet", fontSize = 15.sp, color = TextSecondary)
                    Text("Tap ₹ to log your first payment", fontSize = 13.sp, color = TextSecondary.copy(0.6f))
                }
            }
            groups.isEmpty() -> {
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.SearchOff, null, tint = TextSecondary.copy(0.3f), modifier = Modifier.size(44.dp))
                    Spacer(Modifier.height(14.dp))
                    Text("No transactions found", fontSize = 15.sp, color = TextSecondary)
                }
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    groups.forEach { group ->
                        // Section header — matches iOS section header style
                        item {
                            Text(
                                group.label,
                                fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                color = TextSecondary, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                            )
                        }
                        items(group.items) { tx ->
                            HistoryTxRow(tx = tx, viewModel = viewModel,
                                onDelete = { viewModel.deleteTransaction(tx.id) })
                        }
                    }
                    item { Spacer(Modifier.height(20.dp)) }
                }
            }
        }
    }
}

@Composable
private fun HistoryTxRow(tx: Transaction, viewModel: MainViewModel, onDelete: () -> Unit) {
    var showDelete by remember { mutableStateOf(false) }
    val isIncoming = tx.note.contains("[ip:")
    val avatarColor = avatarColor(tx.recipientName)

    Column(
        Modifier
            .fillMaxWidth()
            .clickable { showDelete = !showDelete }
            .padding(horizontal = 20.dp, vertical = 0.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Avatar — matches iOS TxRow exactly
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(avatarColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    tx.recipientName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    fontSize = 20.sp, fontWeight = FontWeight.Bold, color = avatarColor
                )
            }

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(tx.recipientName.ifEmpty { tx.categoryName }, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 1)
                Text(
                    tx.note.substringBefore("[ip:").trim().ifEmpty { tx.categoryName },
                    fontSize = 12.sp, color = TextSecondary, maxLines = 1
                )
                if (tx.splitGroupId != null) {
                    Box(
                        Modifier.clip(CircleShape).background(AccentBlue.copy(0.15f)).padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text("Split", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = AccentBlue)
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    "${if (isIncoming) "+" else "−"}${viewModel.formatted(tx.amount)}",
                    fontSize = 15.sp, fontWeight = FontWeight.Bold,
                    color = if (isIncoming) IncomeGreen else ExpenseRed
                )
                Text(
                    SimpleDateFormat("d MMM", Locale.getDefault()).format(tx.date),
                    fontSize = 11.sp, color = TextSecondary
                )
            }
        }

        AnimatedVisibility(visible = showDelete) {
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, null, tint = ExpenseRed, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Delete", color = ExpenseRed, fontSize = 12.sp)
                }
            }
        }

        HorizontalDivider(
            Modifier.padding(start = 62.dp),
            color = Color.White.copy(alpha = 0.04f),
            thickness = 0.5.dp
        )
    }
}
