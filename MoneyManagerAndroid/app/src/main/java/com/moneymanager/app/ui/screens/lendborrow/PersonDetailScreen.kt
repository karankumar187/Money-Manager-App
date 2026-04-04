package com.moneymanager.app.ui.screens.lendborrow

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.moneymanager.app.data.models.*
import com.moneymanager.app.ui.screens.payment.mmTextFieldColors
import com.moneymanager.app.ui.theme.*
import com.moneymanager.app.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonDetailScreen(
    personName: String,
    phone: String?,
    viewModel: MainViewModel,
    onNavigatePayment: (upi: String?, name: String, amount: Double) -> Unit,
    onBack: () -> Unit
) {
    val lendBorrows by viewModel.lendBorrows.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val savedUPIs by viewModel.savedUPIs.collectAsState()

    var contactProfile by remember { mutableStateOf<UserProfile?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddLend by remember { mutableStateOf(false) }
    var showSendMoney by remember { mutableStateOf(false) }
    var partialTarget by remember { mutableStateOf<LendBorrow?>(null) }

    val personEntries = lendBorrows.filter { it.personName == personName }
    val personTxs = transactions.filter {
        it.recipientName == personName || it.upiId == phone ||
        (phone != null && it.note.contains(phone))
    }

    val totalLent     = personEntries.filter { it.type == LendBorrowType.LENT && !it.isPaid }.sumOf { it.remainingAmount }
    val totalBorrowed = personEntries.filter { it.type == LendBorrowType.BORROWED && !it.isPaid }.sumOf { it.remainingAmount }
    val netBalance    = totalLent - totalBorrowed
    val color         = avatarColor(personName)
    val fmt           = SimpleDateFormat("d MMM yyyy", Locale.getDefault())

    // Fetch contact's account profile (for UPI and profile pic)
    LaunchedEffect(phone) {
        if (!phone.isNullOrEmpty()) {
            contactProfile = viewModel.fetchContactProfile(phone)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Accent1) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgPrimary)
            )
        },
        containerColor = BgPrimary,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showSendMoney = true },
                containerColor = Accent1, contentColor = TextPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Send, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Send Money", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = innerPadding.calculateTopPadding(), bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Header ──────────────────────────────────────────
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = BgCard),
                    shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                        // Avatar — prefer account profile pic
                        Box(
                            modifier = Modifier.size(64.dp).clip(CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!contactProfile?.profileImageURL.isNullOrEmpty()) {
                                AsyncImage(
                                    model = contactProfile!!.profileImageURL,
                                    contentDescription = personName,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(Modifier.fillMaxSize().background(color.copy(alpha = 0.18f), CircleShape), contentAlignment = Alignment.Center) {
                                    Text(personName.firstOrNull()?.uppercaseChar()?.toString() ?: "?", fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, color = color)
                                }
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(personName, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = TextPrimary)
                            if (!phone.isNullOrEmpty()) Text(phone, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            contactProfile?.upiId?.let { upi ->
                                if (upi.isNotEmpty()) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(upi, fontSize = 12.sp, color = Accent1)
                                        Surface(color = IncomeGreen.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
                                            Text("✓ Verified", fontSize = 9.sp, color = IncomeGreen, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Net balance row
                    if (netBalance != 0.0) {
                        HorizontalDivider(color = BgCardAlt)
                        Row(
                            Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (netBalance > 0) "They owe you" else "You owe them",
                                style = MaterialTheme.typography.bodyMedium, color = TextSecondary
                            )
                            Text(
                                viewModel.formatted(kotlin.math.abs(netBalance)),
                                fontWeight = FontWeight.ExtraBold, fontSize = 20.sp,
                                color = if (netBalance > 0) IncomeGreen else ExpenseRed
                            )
                        }
                    }
                }
            }

            // ── Tab picker ──────────────────────────────────────
            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = BgCard,
                    contentColor = Accent1,
                    modifier = Modifier.clip(RoundedCornerShape(16.dp))
                ) {
                    listOf("Lends", "Transfers").forEachIndexed { idx, label ->
                        Tab(selected = selectedTab == idx, onClick = { selectedTab = idx }) {
                            Text(label, modifier = Modifier.padding(vertical = 12.dp), fontWeight = if (selectedTab == idx) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }

            // ── Lends tab ──────────────────────────────────────
            if (selectedTab == 0) {
                val activeLends     = personEntries.filter { it.type == LendBorrowType.LENT && !it.isPaid }
                val activeBorrows   = personEntries.filter { it.type == LendBorrowType.BORROWED && !it.isPaid }
                val paidEntries     = personEntries.filter { it.isPaid }

                if (activeLends.isNotEmpty()) {
                    item { Text("YOU LENT", style = MaterialTheme.typography.labelSmall, color = IncomeGreen) }
                    items(activeLends) { lb -> LBEntryCard(lb, viewModel, onPartialPay = { partialTarget = lb }) }
                }
                if (activeBorrows.isNotEmpty()) {
                    item { Text("YOU BORROWED", style = MaterialTheme.typography.labelSmall, color = ExpenseRed) }
                    items(activeBorrows) { lb -> LBEntryCard(lb, viewModel, onPartialPay = { partialTarget = lb }) }
                }
                if (paidEntries.isNotEmpty()) {
                    item { Text("SETTLED", style = MaterialTheme.typography.labelSmall, color = TextSecondary) }
                    items(paidEntries) { lb -> LBEntryCard(lb, viewModel, onPartialPay = {}) }
                }
                if (personEntries.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                            Text("No lend/borrow records", color = TextSecondary)
                        }
                    }
                }
            }

            // ── Transfers tab ─────────────────────────────────
            if (selectedTab == 1) {
                if (personTxs.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                            Text("No transfers yet", color = TextSecondary)
                        }
                    }
                } else {
                    items(personTxs) { tx ->
                        val isIncoming = tx.note.contains("[ip:")
                        Card(
                            colors = CardDefaults.cardColors(containerColor = BgCard),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(
                                        (if (isIncoming) IncomeGreen else Accent1).copy(alpha = 0.15f)
                                    ), contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        if (isIncoming) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                        null, tint = if (isIncoming) IncomeGreen else Accent1,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(if (isIncoming) "Received" else "Sent", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrimary)
                                    Text(tx.note.substringBefore("[ip:").trim().ifEmpty { fmt.format(tx.date) }, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                }
                                Text(
                                    "${if (isIncoming) "+" else "−"}${viewModel.formatted(tx.amount)}",
                                    fontWeight = FontWeight.ExtraBold, color = if (isIncoming) IncomeGreen else Accent1
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Lend FAB sheet
    if (showAddLend) {
        AddLendSheet(personName = personName, phone = phone, viewModel = viewModel, onDismiss = { showAddLend = false })
    }

    // Send Money sheet
    if (showSendMoney) {
        val accountUpi = contactProfile?.upiId.takeIf { !it.isNullOrEmpty() } ?: savedUPIs[personName]
        SendMoneySheetAndroid(
            personName = personName, phone = phone,
            prefilledUpi = accountUpi,
            prefilledAmount = if (netBalance < 0) kotlin.math.abs(netBalance) else 0.0,
            viewModel = viewModel,
            onDismiss = { showSendMoney = false }
        )
    }

    // Partial payment dialog
    partialTarget?.let { lb ->
        PartialPayDialog(
            lb = lb, viewModel = viewModel,
            onDismiss = { partialTarget = null }
        )
    }
}

@Composable
private fun LBEntryCard(lb: LendBorrow, viewModel: MainViewModel, onPartialPay: () -> Unit) {
    val color = if (lb.type == LendBorrowType.LENT) IncomeGreen else ExpenseRed
    val fmt = SimpleDateFormat("d MMM yyyy", Locale.getDefault())

    Card(
        colors = CardDefaults.cardColors(containerColor = BgCard),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(viewModel.formatted(lb.amount), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = color)
                        if (lb.splitGroupId != null) {
                            Surface(color = AccentBlue.copy(0.15f), shape = RoundedCornerShape(6.dp)) {
                                Text("Split", color = AccentBlue, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                        if (lb.isPaid) {
                            Surface(color = IncomeGreen.copy(0.15f), shape = RoundedCornerShape(6.dp)) {
                                Text("✓ Settled", color = IncomeGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                    Text(lb.note.ifEmpty { "No note" }, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text(fmt.format(lb.date), style = MaterialTheme.typography.bodySmall, color = TextTertiary, fontSize = 11.sp)
                }
            }

            // Progress bar for partial payments
            if (lb.hasPartialPayment && !lb.isPaid) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LinearProgressIndicator(
                        progress = { lb.paidFraction.toFloat() },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                        color = color, trackColor = BgCardAlt
                    )
                    Text("${viewModel.formatted(lb.paidAmount)} paid · ${viewModel.formatted(lb.remainingAmount)} remaining",
                        style = MaterialTheme.typography.bodySmall, color = color, fontSize = 11.sp)
                }
            }

            if (!lb.isPaid) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onPartialPay,
                        modifier = Modifier.weight(1f).height(36.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
                        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
                    ) { Text("Partial", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                    Button(
                        onClick = { viewModel.markPaid(lb.id) },
                        modifier = Modifier.weight(1f).height(36.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = color)
                    ) { Text("Mark Paid", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SendMoneySheetAndroid(
    personName: String, phone: String?,
    prefilledUpi: String?,
    prefilledAmount: Double,
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val contacts by viewModel.savedContacts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    var amountStr by remember { mutableStateOf(if (prefilledAmount > 0) prefilledAmount.toLong().toString() else "") }
    var note by remember { mutableStateOf("") }
    var recordAsLend by remember { mutableStateOf(false) }
    var selectedCat by remember { mutableStateOf(categories.firstOrNull()) }
    val context = androidx.compose.ui.platform.LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = BgCard,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Send Money to $personName", style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.ExtraBold)

            if (!prefilledUpi.isNullOrEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.AccountBalance, null, tint = Accent1, modifier = Modifier.size(16.dp))
                    Text(prefilledUpi, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                    Surface(color = IncomeGreen.copy(0.15f), shape = RoundedCornerShape(6.dp)) {
                        Text("✓ Verified", color = IncomeGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }

            OutlinedTextField(
                value = amountStr, onValueChange = { amountStr = it },
                prefix = { Text("₹", color = Accent1, fontSize = 22.sp, fontWeight = FontWeight.Bold) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true, modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Amount", color = TextTertiary) },
                colors = mmTextFieldColors(), shape = RoundedCornerShape(12.dp),
                textStyle = LocalTextStyle.current.copy(fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
            )

            OutlinedTextField(
                value = note, onValueChange = { note = it },
                placeholder = { Text("Note (optional)", color = TextTertiary) },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
                colors = mmTextFieldColors(), shape = RoundedCornerShape(12.dp)
            )

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Record as Lend", fontWeight = FontWeight.Medium, color = TextPrimary, fontSize = 14.sp)
                Switch(
                    checked = recordAsLend, onCheckedChange = { recordAsLend = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = TextPrimary, checkedTrackColor = IncomeGreen)
                )
            }

            Button(
                onClick = {
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    if (amount <= 0) return@Button
                    val cat = selectedCat ?: AppCategory.defaults[0]
                    val tx = Transaction(
                        amount = amount, recipientName = personName,
                        upiId = prefilledUpi ?: "", note = note.ifEmpty { cat.name },
                        categoryName = cat.name, categoryEmoji = cat.emoji,
                        categoryHex = cat.colorHex, date = java.util.Date()
                    )
                    viewModel.addTransaction(tx)
                    if (recordAsLend) viewModel.addLendBorrow(LendBorrow(
                        type = LendBorrowType.LENT, personName = personName,
                        contactPhone = phone, amount = amount, note = note.ifEmpty { "Lend" }, date = java.util.Date()
                    ))
                    if (!phone.isNullOrEmpty())
                        viewModel.recordOutgoingPaymentForRecipient(personName, phone, amount, note.ifEmpty { cat.name })

                    if (!prefilledUpi.isNullOrEmpty()) {
                        // Copy & open UPI app
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("UPI ID", prefilledUpi))
                        val upiUri = android.net.Uri.parse("upi://pay?pa=${android.net.Uri.encode(prefilledUpi)}&pn=${android.net.Uri.encode(personName)}&am=$amount&cu=INR")
                        try { context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, upiUri).apply { addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK) }) } catch (_: Exception) {}
                    }
                    onDismiss()
                },
                enabled = (amountStr.toDoubleOrNull() ?: 0.0) > 0,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Accent1)
            ) {
                Icon(Icons.Default.Send, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (!prefilledUpi.isNullOrEmpty()) "Pay via UPI" else "Log Payment", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun AddLendSheet(personName: String, phone: String?, viewModel: MainViewModel, onDismiss: () -> Unit) { /* Simplified — same pattern as above */ }

@Composable
private fun PartialPayDialog(lb: LendBorrow, viewModel: MainViewModel, onDismiss: () -> Unit) {
    var amountStr by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Partial Payment", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Remaining: ${viewModel.formatted(lb.remainingAmount)}", color = TextSecondary)
                OutlinedTextField(
                    value = amountStr, onValueChange = { amountStr = it },
                    prefix = { Text("₹", color = Accent1) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true, colors = mmTextFieldColors(), shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val amt = amountStr.toDoubleOrNull() ?: return@Button
                viewModel.addPartialPayment(lb.id, amt)
                onDismiss()
            }, colors = ButtonDefaults.buttonColors(containerColor = Accent1)) {
                Text("Apply")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) } },
        containerColor = BgCard, titleContentColor = TextPrimary, textContentColor = TextSecondary
    )
}
