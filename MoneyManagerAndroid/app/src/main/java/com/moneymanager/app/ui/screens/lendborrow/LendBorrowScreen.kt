package com.moneymanager.app.ui.screens.lendborrow

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.moneymanager.app.data.models.*
import com.moneymanager.app.ui.theme.*
import com.moneymanager.app.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LendBorrowScreen(
    viewModel: MainViewModel,
    onPersonTap: (name: String, phone: String?) -> Unit,
    onBack: () -> Unit
) {
    val lendBorrows by viewModel.lendBorrows.collectAsState()
    val contacts by viewModel.savedContacts.collectAsState()

    val people = lendBorrows
        .filter { !it.isPaid }
        .groupBy { it.personName }
        .entries
        .sortedByDescending { (_, entries) -> entries.sumOf { it.remainingAmount } }

    val totalLent     = viewModel.totalLent
    val totalBorrowed = viewModel.totalBorrowed

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lends & Borrows", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Accent1) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgPrimary, titleContentColor = TextPrimary)
            )
        },
        containerColor = BgPrimary
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = innerPadding.calculateTopPadding() + 8.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Summary card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = BgCard),
                    shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("LENT OUT", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text(viewModel.formatted(totalLent), fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = IncomeGreen)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("NET", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            val net = totalLent - totalBorrowed
                            Text(viewModel.formatted(kotlin.math.abs(net)), fontWeight = FontWeight.ExtraBold, fontSize = 20.sp,
                                color = if (net >= 0) IncomeGreen else ExpenseRed)
                            Text(if (net >= 0) "in your favor" else "you owe", fontSize = 10.sp, color = TextSecondary)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("BORROWED", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                            Text(viewModel.formatted(totalBorrowed), fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = ExpenseRed)
                        }
                    }
                }
            }

            if (people.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Balance, null, tint = TextTertiary, modifier = Modifier.size(48.dp))
                            Text("No outstanding balances", color = TextSecondary)
                        }
                    }
                }
            } else {
                items(people) { (name, entries) ->
                    val totalLentEntry     = entries.filter { it.type == LendBorrowType.LENT }.sumOf { it.remainingAmount }
                    val totalBorrowedEntry = entries.filter { it.type == LendBorrowType.BORROWED }.sumOf { it.remainingAmount }
                    val phone = contacts[name]
                    PersonCard(
                        name = name,
                        phone = phone,
                        totalLent = totalLentEntry,
                        totalBorrowed = totalBorrowedEntry,
                        hasSplit = entries.any { it.splitGroupId != null },
                        onClick = { onPersonTap(name, phone) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PersonCard(
    name: String, phone: String?,
    totalLent: Double, totalBorrowed: Double,
    hasSplit: Boolean, onClick: () -> Unit
) {
    val color = avatarColor(name)
    val net = totalLent - totalBorrowed

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = BgCard),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            // Avatar
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(color.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Text(name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = color)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary, maxLines = 1)
                    if (hasSplit) {
                        Surface(
                            color = AccentBlue.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("Split", color = AccentBlue, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
                if (!phone.isNullOrEmpty()) {
                    Text(phone, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                Text(
                    when {
                        net > 0 -> "owes you ${formatAmount(net)}"
                        net < 0 -> "you owe ${formatAmount(kotlin.math.abs(net))}"
                        else    -> "settled ✓"
                    },
                    fontSize = 12.sp,
                    color = if (net > 0) IncomeGreen else if (net < 0) ExpenseRed else TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
            Icon(Icons.Default.ChevronRight, null, tint = TextTertiary)
        }
    }
}
