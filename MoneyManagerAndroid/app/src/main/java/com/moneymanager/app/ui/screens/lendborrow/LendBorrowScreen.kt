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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val contacts    by viewModel.savedContacts.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }

    val grandTotalLent     = lendBorrows.filter { it.type == LendBorrowType.LENT     && !it.isPaid }.sumOf { it.remainingAmount }
    val grandTotalBorrowed = lendBorrows.filter { it.type == LendBorrowType.BORROWED && !it.isPaid }.sumOf { it.remainingAmount }

    // Group by person — active (outstanding) + settled
    val grouped = lendBorrows.groupBy { it.personName }
    val activeGroups   = grouped.entries.filter { (_, entries) ->
        entries.any { !it.isPaid }
    }.sortedByDescending { (_, entries) -> entries.sumOf { it.remainingAmount } }
    val settledGroups  = grouped.entries.filter { (_, entries) ->
        entries.all { it.isPaid }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(BgPrimary)
            .systemBarsPadding()
    ) {
        // ── Header — matches iOS exactly ────────────────────────────────
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Payments", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Add lend/borrow record
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Accent1)
                        .clickable { showAddSheet = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }

        // ── Summary pills — LENT OUT | BORROWED ─────────────────────────
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryPill("LENT OUT", viewModel.formatted(grandTotalLent), IncomeGreen, Modifier.weight(1f))
            SummaryPill("BORROWED", viewModel.formatted(grandTotalBorrowed), ExpenseRed, Modifier.weight(1f))
        }

        // ── Person list ─────────────────────────────────────────────────
        LazyColumn(
            contentPadding = PaddingValues(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            if (activeGroups.isEmpty() && settledGroups.isEmpty()) {
                item {
                    Column(
                        Modifier.fillMaxWidth().padding(60.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(Icons.Default.Balance, null, tint = TextTertiary, modifier = Modifier.size(56.dp))
                        Text("No lend/borrow records", fontSize = 15.sp, color = TextSecondary)
                        Text("Tap + to add a record", fontSize = 13.sp, color = TextTertiary)
                    }
                }
            }

            if (activeGroups.isNotEmpty()) {
                item { SectionLabel("OUTSTANDING") }
                items(activeGroups) { (name, entries) ->
                    val phone = contacts[name]
                    PersonCard(
                        name = name, phone = phone,
                        totalLent = entries.filter { it.type == LendBorrowType.LENT }.sumOf { it.remainingAmount },
                        totalBorrowed = entries.filter { it.type == LendBorrowType.BORROWED }.sumOf { it.remainingAmount },
                        hasSplit = entries.any { it.splitGroupId != null },
                        onClick = { onPersonTap(name, phone) }
                    )
                }
            }

            if (settledGroups.isNotEmpty()) {
                item { SectionLabel("OTHER CONTACTS") }
                items(settledGroups) { (name, entries) ->
                    val phone = contacts[name]
                    PersonCard(
                        name = name, phone = phone,
                        totalLent = 0.0, totalBorrowed = 0.0,
                        hasSplit = entries.any { it.splitGroupId != null },
                        onClick = { onPersonTap(name, phone) }
                    )
                }
            }
        }
    }

    if (showAddSheet) {
        AddLendSheet(viewModel = viewModel, onDismiss = { showAddSheet = false })
    }
}

@Composable
private fun SummaryPill(label: String, amount: String, color: Color, modifier: Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.1f))
            .border(BorderStroke(1.dp, color.copy(alpha = 0.2f)), RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color.copy(0.7f), letterSpacing = 1.sp)
            Text(amount, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
    )
}

@Composable
private fun PersonCard(
    name: String, phone: String?,
    totalLent: Double, totalBorrowed: Double,
    hasSplit: Boolean, onClick: () -> Unit
) {
    val color = avatarColor(name)
    val net = totalLent - totalBorrowed

    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Avatar
        Box(
            Modifier.size(48.dp).clip(CircleShape).background(color.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color
            )
        }

        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 1)
                if (hasSplit) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(AccentBlue.copy(0.15f))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text("Split", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = AccentBlue)
                    }
                }
            }
            Text(
                when {
                    net > 0  -> "owes you ${formatAmount(net)}"
                    net < 0  -> "you owe ${formatAmount(-net)}"
                    else     -> "all settled ✓"
                },
                fontSize = 12.sp,
                color = when {
                    net > 0  -> IncomeGreen
                    net < 0  -> ExpenseRed
                    else     -> TextSecondary
                },
                fontWeight = FontWeight.Medium
            )
            if (!phone.isNullOrEmpty()) {
                Text(phone, fontSize = 12.sp, color = TextSecondary)
            }
        }

        Icon(Icons.Default.ChevronRight, null, tint = TextTertiary.copy(0.4f))
    }

    HorizontalDivider(
        Modifier.padding(start = 82.dp, end = 20.dp),
        color = Color.White.copy(alpha = 0.04f),
        thickness = 0.5.dp
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddLendSheet(viewModel: MainViewModel, onDismiss: () -> Unit) {
    var name       by remember { mutableStateOf("") }
    var amountStr  by remember { mutableStateOf("") }
    var note       by remember { mutableStateOf("") }
    var type       by remember { mutableStateOf(LendBorrowType.LENT) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = BgCard) {
        Column(
            Modifier.padding(horizontal = 24.dp).padding(bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Add Record", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)

            // Lent / Borrowed toggle
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(BgCardAlt),
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                listOf(LendBorrowType.LENT to "I Lent", LendBorrowType.BORROWED to "I Borrowed").forEach { (t, label) ->
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (type == t) if (t == LendBorrowType.LENT) IncomeGreen else ExpenseRed else Color.Transparent)
                            .clickable { type = t }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            fontWeight = FontWeight.Bold,
                            color = if (type == t) Color.White else TextSecondary
                        )
                    }
                }
            }

            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Person name", color = TextSecondary) },
                modifier = Modifier.fillMaxWidth(),
                colors = mmLendTextFieldColors(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            OutlinedTextField(
                value = amountStr, onValueChange = { amountStr = it },
                label = { Text("Amount", color = TextSecondary) },
                prefix = { Text("₹", color = Accent1, fontWeight = FontWeight.Bold) },
                modifier = Modifier.fillMaxWidth(),
                colors = mmLendTextFieldColors(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            OutlinedTextField(
                value = note, onValueChange = { note = it },
                label = { Text("Note (optional)", color = TextSecondary) },
                modifier = Modifier.fillMaxWidth(),
                colors = mmLendTextFieldColors(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: return@Button
                    if (name.isBlank()) return@Button
                    viewModel.addLendBorrow(LendBorrow(
                        type = type, personName = name.trim(),
                        amount = amt, note = note.trim(), date = java.util.Date()
                    ))
                    onDismiss()
                },
                enabled = name.isNotBlank() && (amountStr.toDoubleOrNull() ?: 0.0) > 0,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Accent1)
            ) {
                Text("Save", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun mmLendTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Accent1, unfocusedBorderColor = BgCardAlt,
    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
    cursorColor = Accent1, focusedContainerColor = BgCardAlt,
    unfocusedContainerColor = BgCardAlt, focusedLabelColor = Accent1
)
