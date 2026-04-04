package com.moneymanager.app.ui.screens.payment

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moneymanager.app.data.models.*
import com.moneymanager.app.ui.theme.*
import com.moneymanager.app.ui.viewmodel.MainViewModel
import java.util.Date

data class UPIAppOption(
    val label: String, val scheme: String, val color: androidx.compose.ui.graphics.Color
)

val upiApps = listOf(
    UPIAppOption("GPay",    "tez://upi/pay",     androidx.compose.ui.graphics.Color(0xFF1AA260)),
    UPIAppOption("PhonePe", "phonepe://pay",     androidx.compose.ui.graphics.Color(0xFF5F259F)),
    UPIAppOption("Paytm",   "paytmmp://pay",     androidx.compose.ui.graphics.Color(0xFF00B9F1)),
    UPIAppOption("BHIM",    "upi://pay",         AccentBlue)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    prefilledUpi: String = "",
    prefilledName: String = "",
    prefilledAmount: Double = 0.0,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val contacts by viewModel.savedContacts.collectAsState()
    val savedUPIs by viewModel.savedUPIs.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val profile by viewModel.userProfile.collectAsState()

    var recipientName by remember { mutableStateOf(prefilledName) }
    var upiId by remember { mutableStateOf(prefilledUpi) }
    var amountStr by remember { mutableStateOf(if (prefilledAmount > 0) prefilledAmount.toLong().toString() else "") }
    var note by remember { mutableStateOf("") }
    var selectedCat by remember { mutableStateOf<AppCategory?>(null) }
    var selectedApp by remember { mutableStateOf(upiApps.first()) }
    var recordAsLend by remember { mutableStateOf(false) }
    var isSplitEnabled by remember { mutableStateOf(false) }
    var splitFriends by remember { mutableStateOf<List<SplitFriendEntry>>(emptyList()) }
    var showSplitSheet by remember { mutableStateOf(false) }
    var showCopied by remember { mutableStateOf(false) }
    var contactProfile by remember { mutableStateOf<UserProfile?>(null) }

    val amount = amountStr.toDoubleOrNull() ?: 0.0
    val effectiveUpi = upiId.ifEmpty { savedUPIs[recipientName] ?: "" }
    val hasUPI = effectiveUpi.isNotEmpty()
    val canProceed = amount > 0

    LaunchedEffect(Unit) { selectedCat = selectedCat ?: categories.firstOrNull() }
    LaunchedEffect(recipientName) {
        val phone = contacts[recipientName]
        if (!phone.isNullOrEmpty()) {
            contactProfile = viewModel.fetchContactProfile(phone)
            contactProfile?.upiId?.let { if (upiId.isEmpty()) upiId = it }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pay", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Accent1) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgPrimary, titleContentColor = TextPrimary)
            )
        },
        containerColor = BgPrimary
    ) { innerPadding ->
        Column(
            Modifier.padding(innerPadding).padding(horizontal = 20.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Recipient ─────────────────────────────────────────
            SectionCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionLabel("TO")
                    OutlinedTextField(
                        value = recipientName, onValueChange = { recipientName = it },
                        placeholder = { Text("Name or UPI ID", color = TextTertiary) },
                        leadingIcon = { Icon(Icons.Default.Person, null, tint = TextSecondary) },
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                        colors = mmTextFieldColors(), shape = RoundedCornerShape(12.dp)
                    )
                    // UPI ID field
                    OutlinedTextField(
                        value = upiId, onValueChange = { upiId = it },
                        placeholder = { Text("UPI ID (e.g. name@upi)", color = TextTertiary) },
                        leadingIcon = { Icon(Icons.Default.AccountBalance, null, tint = TextSecondary) },
                        trailingIcon = if (hasUPI && contactProfile != null) {{
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("✓", color = IncomeGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(" Verified", color = IncomeGreen, fontSize = 10.sp)
                                Spacer(Modifier.width(8.dp))
                            }
                        }} else null,
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                        colors = mmTextFieldColors(), shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // ── Amount ────────────────────────────────────────────
            SectionCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel("AMOUNT")
                    OutlinedTextField(
                        value = amountStr, onValueChange = { amountStr = it },
                        placeholder = { Text("0", color = TextTertiary, fontSize = 32.sp) },
                        prefix = { Text("₹", color = Accent1, fontSize = 26.sp, fontWeight = FontWeight.Bold) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                        colors = mmTextFieldColors(), shape = RoundedCornerShape(12.dp),
                        textStyle = LocalTextStyle.current.copy(fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                    )
                }
            }

            // ── Note ─────────────────────────────────────────────
            SectionCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel("NOTE")
                    OutlinedTextField(
                        value = note, onValueChange = { note = it },
                        placeholder = { Text("What's this for?", color = TextTertiary) },
                        leadingIcon = { Icon(Icons.Default.Edit, null, tint = TextSecondary) },
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                        colors = mmTextFieldColors(), shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // ── Category ─────────────────────────────────────────
            SectionCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel("CATEGORY")
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        categories.forEach { cat ->
                            val sel = selectedCat?.id == cat.id
                            FilterChip(
                                selected = sel,
                                onClick = { selectedCat = cat },
                                label = { Text("${cat.emoji} ${cat.name}", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = hexColor(cat.colorHex).copy(alpha = 0.7f),
                                    selectedLabelColor = TextPrimary,
                                    containerColor = BgCardAlt,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }
                }
            }

            // ── Options (Split + Lend) ────────────────────────────
            SectionCard {
                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    // Split row
                    Row(
                        Modifier.fillMaxWidth().clickable { showSplitSheet = true }.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.People, null, tint = AccentBlue, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Split Bill", fontWeight = FontWeight.Medium, fontSize = 15.sp, color = TextPrimary)
                            if (splitFriends.isNotEmpty())
                                Text("Splitting with ${splitFriends.size} people", fontSize = 12.sp, color = AccentBlue)
                        }
                        if (splitFriends.isNotEmpty()) {
                            IconButton(onClick = { splitFriends = emptyList(); isSplitEnabled = false }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Cancel, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                            }
                        } else {
                            Icon(Icons.Default.ChevronRight, null, tint = TextTertiary, modifier = Modifier.size(18.dp))
                        }
                    }
                    HorizontalDivider(color = BgCardAlt.copy(alpha = 0.5f))
                    // Lend toggle
                    Row(
                        Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ArrowCircleUp, null, tint = IncomeGreen, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Record as Lend", fontWeight = FontWeight.Medium, fontSize = 15.sp, color = TextPrimary)
                            Text(if (recordAsLend) "Saved to Lends" else "Only in History", fontSize = 12.sp, color = TextSecondary)
                        }
                        Switch(
                            checked = recordAsLend, onCheckedChange = { recordAsLend = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = TextPrimary, checkedTrackColor = IncomeGreen)
                        )
                    }
                }
            }

            // ── UPI App picker ────────────────────────────────────
            if (hasUPI) {
                SectionCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionLabel("PAY VIA")
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            upiApps.forEach { app ->
                                val sel = selectedApp == app
                                Card(
                                    onClick = { selectedApp = app },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (sel) app.color.copy(alpha = 0.15f) else BgCardAlt
                                    ),
                                    border = if (sel) BorderStroke(1.dp, app.color.copy(alpha = 0.5f)) else null,
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Column(
                                        Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(Modifier.size(28.dp).clip(CircleShape).background(app.color.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                                            Text("₹", color = app.color, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                                        }
                                        Text(app.label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (sel) app.color else TextSecondary)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Pay Button ────────────────────────────────────────
            Button(
                onClick = {
                    handlePay(
                        context = context,
                        viewModel = viewModel,
                        recipientName = recipientName,
                        upiId = effectiveUpi,
                        amount = amount,
                        note = note,
                        cat = selectedCat ?: categories.firstOrNull() ?: AppCategory.defaults[0],
                        app = selectedApp,
                        recordAsLend = recordAsLend,
                        splitFriends = splitFriends,
                        isSplit = isSplitEnabled,
                        contacts = contacts
                    )
                    showCopied = true
                },
                enabled = canProceed,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (hasUPI) Accent1 else BgCard,
                    contentColor = TextPrimary,
                    disabledContainerColor = BgCard
                )
            ) {
                Icon(if (hasUPI) Icons.Default.Send else Icons.Default.Save, null,  modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    if (hasUPI) "Open ${selectedApp.label}" else "Log Payment Only",
                    fontWeight = FontWeight.Bold, fontSize = 16.sp
                )
            }

            if (showCopied) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = BgCard),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ContentCopy, null, tint = Accent1)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("UPI ID copied!", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextPrimary)
                            Text("Paste it in the payment app", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }

    if (showSplitSheet) {
        SplitSheet(
            viewModel = viewModel,
            totalAmount = amount,
            onConfirm = { friends ->
                splitFriends = friends
                isSplitEnabled = friends.isNotEmpty()
                showSplitSheet = false
            },
            onDismiss = { showSplitSheet = false }
        )
    }
}

private fun handlePay(
    context: android.content.Context,
    viewModel: MainViewModel,
    recipientName: String, upiId: String,
    amount: Double, note: String, cat: AppCategory,
    app: UPIAppOption, recordAsLend: Boolean,
    splitFriends: List<SplitFriendEntry>,
    isSplit: Boolean, contacts: Map<String, String?>
) {
    if (isSplit && splitFriends.isNotEmpty()) {
        val myShare = maxOf(0.0, amount - splitFriends.sumOf { it.share })
        viewModel.splitBill(
            totalAmount = amount, myShare = myShare,
            note = note.ifEmpty { cat.name },
            groupName = recipientName,
            originalRecipient = recipientName,
            friends = splitFriends.map {
                com.moneymanager.app.data.repository.SplitFriend(name = it.name, phone = it.phone, share = it.share)
            }
        )
    } else {
        val tx = Transaction(
            amount = amount, recipientName = recipientName, upiId = upiId,
            note = note.ifEmpty { cat.name },
            categoryId = cat.id, categoryName = cat.name,
            categoryEmoji = cat.emoji, categoryHex = cat.colorHex,
            date = Date()
        )
        viewModel.addTransaction(tx)

        if (recordAsLend && recipientName.isNotBlank()) {
            viewModel.addLendBorrow(LendBorrow(
                type = LendBorrowType.LENT, personName = recipientName,
                contactPhone = contacts[recipientName],
                amount = amount, note = note.ifEmpty { "Via payment" }, date = Date()
            ))
        }

        viewModel.savePaymentContact(recipientName, contacts[recipientName])

        val recipientPhone = contacts[recipientName] ?: upiId.takeIf { it.isNotEmpty() }
        if (!recipientPhone.isNullOrEmpty()) {
            viewModel.recordOutgoingPaymentForRecipient(recipientName, recipientPhone, amount, note.ifEmpty { cat.name })
        }
    }

    if (upiId.isNotEmpty()) {
        // Copy UPI ID to clipboard
        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("UPI ID", upiId))

        // Build standard UPI deep link and launch
        val upiUri = Uri.parse("upi://pay?pa=${Uri.encode(upiId)}&pn=${Uri.encode(recipientName)}&am=$amount&cu=INR&tn=${Uri.encode(note.ifEmpty { cat.name })}")
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, upiUri).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
        } catch (e: Exception) {
            // Fallback: open Play Store / direct UPI app
        }
    }
}

// Helper composables
@Composable
fun SectionCard(content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = BgCard),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) { content() }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text, style = MaterialTheme.typography.labelSmall, color = TextSecondary,
        modifier = Modifier.padding(start = 14.dp, top = 14.dp, end = 14.dp)
    )
}

@Composable
fun mmTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Accent1, unfocusedBorderColor = BgCardAlt,
    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
    cursorColor = Accent1, focusedContainerColor = BgCardAlt,
    unfocusedContainerColor = BgCardAlt
)

data class SplitFriendEntry(val name: String, val phone: String? = null, val share: Double)
