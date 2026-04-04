package com.moneymanager.app.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moneymanager.app.data.models.*
import com.moneymanager.app.ui.theme.*
import com.moneymanager.app.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val transactions by viewModel.transactions.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf<String?>(null) }
    val fmt = SimpleDateFormat("d MMM yyyy · HH:mm", Locale.getDefault())
    val dayFmt = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())

    val filtered = transactions.filter { tx ->
        (searchQuery.isEmpty() || tx.recipientName.contains(searchQuery, true) || tx.note.contains(searchQuery, true)) &&
        (selectedFilter == null || tx.categoryName == selectedFilter)
    }

    val categories = transactions.map { it.categoryName }.distinct()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transaction History", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Accent1) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgPrimary, titleContentColor = TextPrimary)
            )
        },
        containerColor = BgPrimary
    ) { innerPadding ->
        Column(Modifier.padding(innerPadding)) {
            // Search bar
            OutlinedTextField(
                value = searchQuery, onValueChange = { searchQuery = it },
                placeholder = { Text("Search transactions...", color = TextTertiary) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = TextSecondary) },
                trailingIcon = if (searchQuery.isNotEmpty()) {{
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, null, tint = TextSecondary)
                    }
                }} else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Accent1, unfocusedBorderColor = BgCardAlt,
                    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                    cursorColor = Accent1, focusedContainerColor = BgCard, unfocusedContainerColor = BgCard
                ),
                shape = RoundedCornerShape(14.dp)
            )

            // Category filter chips
            if (categories.isNotEmpty()) {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == null,
                        onClick = { selectedFilter = null },
                        label = { Text("All") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Accent1.copy(0.2f), selectedLabelColor = Accent1,
                            containerColor = BgCard, labelColor = TextSecondary
                        )
                    )
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedFilter == cat,
                            onClick = { selectedFilter = if (selectedFilter == cat) null else cat },
                            label = { Text(cat, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Accent1.copy(0.2f), selectedLabelColor = Accent1,
                                containerColor = BgCard, labelColor = TextSecondary
                            )
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            if (filtered.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.SearchOff, null, tint = TextTertiary, modifier = Modifier.size(48.dp))
                        Text("No transactions found", color = TextSecondary)
                    }
                }
            } else {
                // Group by day
                val grouped = filtered.groupBy { tx ->
                    val cal = Calendar.getInstance().apply { time = tx.date }
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    cal.time
                }.entries.sortedByDescending { it.key }

                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    grouped.forEach { (day, txsOnDay) ->
                        item {
                            val dayText = when {
                                isToday(day) -> "Today"
                                isYesterday(day) -> "Yesterday"
                                else -> dayFmt.format(day)
                            }
                            val dayTotal = txsOnDay.sumOf { it.amount }
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(dayText, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text(viewModel.formatted(dayTotal), style = MaterialTheme.typography.labelSmall, color = ExpenseRed.copy(0.8f))
                            }
                        }
                        items(txsOnDay) { tx ->
                            HistoryTxCard(tx = tx, time = fmt.format(tx.date).substringAfter("· "), viewModel = viewModel,
                                onDelete = { viewModel.deleteTransaction(tx.id) })
                        }
                    }
                    item { Spacer(Modifier.height(40.dp)) }
                }
            }
        }
    }
}

@Composable
private fun HistoryTxCard(tx: Transaction, time: String, viewModel: MainViewModel, onDelete: () -> Unit) {
    var showDelete by remember { mutableStateOf(false) }
    val isIncoming = tx.note.contains("[ip:")
    val color = hexColor(tx.categoryHex)

    Card(
        onClick = { showDelete = !showDelete },
        colors = CardDefaults.cardColors(containerColor = BgCard),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column {
            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(tx.categoryEmoji.ifEmpty { "💳" }, fontSize = 20.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(tx.recipientName.ifEmpty { tx.categoryName }, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimary, maxLines = 1)
                    Text(tx.note.substringBefore("[ip:").trim().ifEmpty { tx.categoryName }, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1)
                    if (tx.splitGroupId != null) {
                        Surface(color = AccentBlue.copy(0.15f), shape = RoundedCornerShape(4.dp)) {
                            Text("Split", color = AccentBlue, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "${if (isIncoming) "+" else "−"}${viewModel.formatted(tx.amount)}",
                        fontWeight = FontWeight.ExtraBold, fontSize = 15.sp,
                        color = if (isIncoming) IncomeGreen else ExpenseRed
                    )
                    Text(time, style = MaterialTheme.typography.bodySmall, color = TextSecondary, fontSize = 11.sp)
                }
            }
            if (showDelete) {
                HorizontalDivider(color = BgCardAlt)
                Row(Modifier.fillMaxWidth().padding(4.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, null, tint = ExpenseRed, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Delete", color = ExpenseRed, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

private fun isToday(date: Date): Boolean {
    val c1 = Calendar.getInstance(); val c2 = Calendar.getInstance().apply { time = date }
    return c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR) && c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
}
private fun isYesterday(date: Date): Boolean {
    val c1 = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }; val c2 = Calendar.getInstance().apply { time = date }
    return c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR) && c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
}
