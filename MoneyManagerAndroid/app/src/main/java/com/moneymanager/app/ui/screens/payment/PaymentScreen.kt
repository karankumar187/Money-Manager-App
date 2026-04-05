package com.moneymanager.app.ui.screens.payment

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moneymanager.app.data.models.*
import com.moneymanager.app.ui.theme.*
import com.moneymanager.app.ui.viewmodel.MainViewModel
import java.util.Date

// ── UPI apps identical to iOS ──────────────────────────────────────────────────
data class UPIAppOption(val label: String, val scheme: String, val color: Color)

val upiApps = listOf(
    UPIAppOption("GPay",    "tez://upi/pay",   Color(0xFF1AA260)),
    UPIAppOption("PhonePe", "phonepe://pay",   Color(0xFF5F259F)),
    UPIAppOption("Kotak",   "kotak://upi/pay", Color(0xFFEA3323)),
    UPIAppOption("Pop UPI", "upi://pay",       Color(0xFF7B61FF)),
)

data class SplitFriendEntry(val name: String, val phone: String? = null, val share: Double)

// ── Payment Screen — 3-step flow matching iOS exactly ─────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    prefilledUpi    : String = "",
    prefilledName   : String = "",
    prefilledAmount : Double = 0.0,
    viewModel       : MainViewModel,
    onBack          : () -> Unit
) {
    val context    = LocalContext.current
    val contacts   by viewModel.savedContacts.collectAsState()
    val savedUPIs  by viewModel.savedUPIs.collectAsState()
    val categories by viewModel.categories.collectAsState()

    var step         by remember { mutableStateOf(if (prefilledName.isNotEmpty()) 1 else 0) }
    var upiId        by remember { mutableStateOf(prefilledUpi) }
    var recipientName by remember { mutableStateOf(prefilledName) }
    var amountStr    by remember { mutableStateOf(if (prefilledAmount > 0) prefilledAmount.toLong().toString() else "") }
    var note         by remember { mutableStateOf("") }
    var selectedCat  by remember { mutableStateOf<AppCategory?>(null) }
    var selectedApp  by remember { mutableStateOf<UPIAppOption?>(upiApps.first()) }
    var recordAsLend by remember { mutableStateOf(false) }
    var splitFriends by remember { mutableStateOf<List<SplitFriendEntry>>(emptyList()) }
    var showSplit    by remember { mutableStateOf(false) }
    var showSuccess  by remember { mutableStateOf(false) }

    val amount = amountStr.toDoubleOrNull() ?: 0.0

    LaunchedEffect(Unit) { selectedCat = selectedCat ?: categories.firstOrNull() }
    LaunchedEffect(prefilledName) {
        if (prefilledName.isNotEmpty()) {
            val savedUpi = savedUPIs[prefilledName]
            if (!savedUpi.isNullOrEmpty() && upiId.isEmpty()) upiId = savedUpi
        }
    }

    Box(Modifier.fillMaxSize().background(BgPrimary)) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {

            // ── Nav: back button row ───────────────────────────────────────
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (step) {
                    0 -> IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, null, tint = Accent1, modifier = Modifier.size(22.dp))
                    }
                    1 -> IconButton(onClick = { step = 0; upiId = "" }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ChevronLeft, null, tint = Accent1)
                            Text("Back", color = Accent1, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    else -> IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, null, tint = Accent1, modifier = Modifier.size(22.dp))
                    }
                }
                if (step == 1 && upiId.isNotEmpty()) {
                    Text(
                        upiId, fontSize = 11.sp, color = TextTertiary,
                        maxLines = 1, modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
            }

            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { it } + fadeIn() togetherWith
                        slideOutHorizontally { -it } + fadeOut()
                    } else {
                        slideInHorizontally { -it } + fadeIn() togetherWith
                        slideOutHorizontally { it } + fadeOut()
                    }
                },
                label = "payment_step"
            ) { s ->
                when (s) {
                    // ─── Step 0: QR / UPI entry ─────────────────────────────
                    0 -> StepZero(
                        upiId     = upiId,
                        onUpiChange = { upiId = it },
                        onContinue  = { step = 1 }
                    )

                    // ─── Step 1+: Payment Details ────────────────────────────
                    else -> StepOne(
                        amountStr       = amountStr,
                        onAmountChange  = { amountStr = it },
                        recipientName   = recipientName,
                        onNameChange    = { recipientName = it },
                        note            = note,
                        onNoteChange    = { note = it },
                        categories      = categories,
                        selectedCat     = selectedCat,
                        onCatSelect     = { selectedCat = it },
                        selectedApp     = selectedApp,
                        onAppSelect     = { selectedApp = it },
                        splitFriends    = splitFriends,
                        onSplitTap      = { showSplit = true },
                        onSplitClear    = { splitFriends = emptyList() },
                        recordAsLend    = recordAsLend,
                        onRecordToggle  = { recordAsLend = it },
                        canPay          = amount > 0 && recipientName.isNotBlank(),
                        onPay = {
                            handlePay(
                                context = context, viewModel = viewModel,
                                recipientName = recipientName, upiId = upiId,
                                amount = amount, note = note,
                                cat = selectedCat ?: categories.firstOrNull() ?: AppCategory.defaults[0],
                                app = selectedApp, recordAsLend = recordAsLend,
                                splitFriends = splitFriends, contacts = contacts
                            )
                            showSuccess = true
                        }
                    )
                }
            }
        }

        // ── Success overlay ─────────────────────────────────────────────────
        AnimatedVisibility(
            visible = showSuccess,
            enter = fadeIn(), exit = fadeOut()
        ) {
            Box(
                Modifier.fillMaxSize().background(BgPrimary.copy(0.97f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        Modifier.size(90.dp).clip(CircleShape).background(IncomeGreen.copy(0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, null, tint = IncomeGreen, modifier = Modifier.size(48.dp))
                    }
                    Text("Payment Initiated!", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Logged successfully", fontSize = 14.sp, color = TextSecondary)
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = onBack,
                        shape  = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent1),
                        modifier = Modifier.fillMaxWidth(0.6f).height(50.dp)
                    ) {
                        Text("Done", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }

    // ── Split sheet ────────────────────────────────────────────────────────
    if (showSplit) {
        SplitSheet(
            viewModel   = viewModel,
            totalAmount = amount,
            onConfirm   = { friends -> splitFriends = friends; showSplit = false },
            onDismiss   = { showSplit = false }
        )
    }
}

// ─── Step 0: QR / UPI entry ─────────────────────────────────────────────────────
@Composable
private fun StepZero(upiId: String, onUpiChange: (String) -> Unit, onContinue: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Hero prompt ─────────────────────────────────────────────────────
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                Modifier.size(96.dp).clip(CircleShape).background(Accent1.copy(0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.QrCodeScanner, null, tint = Accent1, modifier = Modifier.size(46.dp))
            }
            Text("Scan to Pay", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(
                "Point your camera at a UPI QR code,\nor choose another method below",
                fontSize = 14.sp, color = TextSecondary, textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }

        Spacer(Modifier.height(40.dp))

        // ── Options card ────────────────────────────────────────────────────
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(BgCard)
                .border(BorderStroke(0.6.dp, Color.White.copy(0.055f)), RoundedCornerShape(20.dp))
        ) {
            // Scan camera option
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { /* Camera scanner — in production: launch CameraX QR */ onContinue() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Accent1.copy(0.15f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.CameraAlt, null, tint = Accent1, modifier = Modifier.size(22.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text("Open Camera", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text("Scan a UPI QR code", fontSize = 12.sp, color = TextSecondary)
                }
                Icon(Icons.Default.ChevronRight, null, tint = TextTertiary.copy(0.4f))
            }

            HorizontalDivider(Modifier.padding(start = 76.dp), color = Color.White.copy(0.07f))

            // Type UPI manually
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFFF9F0A).copy(0.15f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Keyboard, null, tint = Color(0xFFFF9F0A), modifier = Modifier.size(22.dp))
                    }
                    Text("Enter UPI ID manually", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }

                // UPI field
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(0.06f))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("@", fontSize = 14.sp, color = TextSecondary)
                    androidx.compose.foundation.text.BasicTextField(
                        value = upiId.removePrefix("@").removePrefix("@"),
                        onValueChange = onUpiChange,
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(color = TextPrimary, fontSize = 15.sp),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(Accent1),
                        decorationBox = { inner ->
                            if (upiId.isEmpty()) Text("yourname@bank", color = TextSecondary, fontSize = 15.sp)
                            inner()
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Continue button
        val canContinue = upiId.isNotBlank()
        Button(
            onClick = onContinue,
            enabled  = canContinue,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(56.dp),
            shape  = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (canContinue) Accent1 else BgCard,
                disabledContainerColor = BgCard
            )
        ) {
            Text("Continue →", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = if (canContinue) Color.White else TextSecondary)
        }

        Spacer(Modifier.height(50.dp))
    }
}

// ─── Step 1: Payment Details ─────────────────────────────────────────────────
@Composable
private fun StepOne(
    amountStr: String, onAmountChange: (String) -> Unit,
    recipientName: String, onNameChange: (String) -> Unit,
    note: String, onNoteChange: (String) -> Unit,
    categories: List<AppCategory>, selectedCat: AppCategory?, onCatSelect: (AppCategory) -> Unit,
    selectedApp: UPIAppOption?, onAppSelect: (UPIAppOption?) -> Unit,
    splitFriends: List<SplitFriendEntry>, onSplitTap: () -> Unit, onSplitClear: () -> Unit,
    recordAsLend: Boolean, onRecordToggle: (Boolean) -> Unit,
    canPay: Boolean, onPay: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // ── Large ₹ Amount — matches iOS 68sp ──────────────────────────────
        Column(
            Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "₹",
                    fontSize = 40.sp, fontWeight = FontWeight.SemiBold,
                    color = if (amountStr.isEmpty()) TextSecondary.copy(0.5f) else Accent1,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                androidx.compose.foundation.text.BasicTextField(
                    value = amountStr, onValueChange = onAmountChange,
                    modifier = Modifier.widthIn(min = 80.dp),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 68.sp, fontWeight = FontWeight.Bold,
                        color = if (amountStr.isEmpty()) TextSecondary.copy(0.3f) else TextPrimary,
                        textAlign = TextAlign.Center
                    ),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(Accent1),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    decorationBox = { inner ->
                        if (amountStr.isEmpty()) Text("0", fontSize = 68.sp, fontWeight = FontWeight.Bold, color = TextSecondary.copy(0.3f), textAlign = TextAlign.Center)
                        inner()
                    }
                )
            }
            Text(
                if (amountStr.isNotEmpty() && (amountStr.toDoubleOrNull() ?: 0.0) > 0)
                    "₹${amountStr.toDouble().toLong()}"
                else "Enter amount",
                fontSize = 14.sp, color = TextSecondary
            )
        }

        // ── Category pills row ─────────────────────────────────────────────
        Row(
            Modifier
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Spacer(Modifier.width(20.dp))
            categories.forEach { cat ->
                val isSelected = selectedCat?.id == cat.id
                val catColor = hexColor(cat.colorHex)
                Box(
                    Modifier
                        .clip(CircleShape)
                        .background(if (isSelected) catColor.copy(0.25f) else Color.White.copy(0.04f))
                        .border(
                            BorderStroke(1.dp, if (isSelected) catColor.copy(0.5f) else Color.White.copy(0.08f)),
                            CircleShape
                        )
                        .clickable { onCatSelect(cat) }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(cat.emoji, fontSize = 14.sp)
                        Text(
                            cat.name.split(" ").first(),
                            fontSize = 13.sp, fontWeight = FontWeight.Medium,
                            color = if (isSelected) Color.White else TextSecondary
                        )
                    }
                }
            }
            Spacer(Modifier.width(20.dp))
        }

        // ── Unified form block ─────────────────────────────────────────────
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(BgCard)
                .border(BorderStroke(0.6.dp, Color.White.copy(0.055f)), RoundedCornerShape(20.dp))
        ) {
            // Recipient name
            FormRow(Icons.Default.Person, "Who are you paying?", recipientName, onNameChange)
            HorizontalDivider(Modifier.padding(start = 54.dp), color = Color.White.copy(0.06f))

            // Note
            FormRow(Icons.Default.Edit, "Add a note (dinner, rent…)", note, onNoteChange)
            HorizontalDivider(Modifier.padding(start = 54.dp), color = Color.White.copy(0.06f))

            // Pay via UPI apps (horizontal)
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(Modifier.width(4.dp))
                Text("Pay Via", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextSecondary)

                upiApps.forEach { app ->
                    val sel = selectedApp == app
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(BorderStroke(if (sel) 3.dp else 0.dp, if (sel) app.color else Color.Transparent), CircleShape)
                            .clickable { onAppSelect(app) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("₹", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = app.color)
                    }
                }

                // Manual "no app" option
                val noApp = selectedApp == null
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (noApp) Accent1.copy(0.2f) else Color.White.copy(0.08f))
                        .border(BorderStroke(if (noApp) 2.dp else 0.dp, if (noApp) Accent1 else Color.Transparent), CircleShape)
                        .clickable { onAppSelect(null) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Edit, null, tint = if (noApp) Accent1 else TextSecondary, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(16.dp))
            }

            HorizontalDivider(Modifier.padding(start = 54.dp), color = Color.White.copy(0.06f))

            // Split Bill row
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onSplitTap() }
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    if (splitFriends.isEmpty()) Icons.Default.People else Icons.Default.PeopleAlt,
                    null, tint = AccentBlue, modifier = Modifier.size(24.dp)
                )
                Column(Modifier.weight(1f)) {
                    Text("Split Bill", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                    if (splitFriends.isNotEmpty()) {
                        Text("Splitting with ${splitFriends.size} people", fontSize = 12.sp, color = AccentBlue)
                    }
                }
                if (splitFriends.isNotEmpty()) {
                    IconButton(onClick = onSplitClear, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Cancel, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                } else {
                    Icon(Icons.Default.ChevronRight, null, tint = TextTertiary.copy(0.4f))
                }
            }

            HorizontalDivider(Modifier.padding(start = 54.dp), color = Color.White.copy(0.06f))

            // Record as Lend toggle
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(Icons.Default.ArrowCircleUp, null, tint = IncomeGreen, modifier = Modifier.size(24.dp))
                Column(Modifier.weight(1f)) {
                    Text("Record as Lend", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                    Text(
                        if (recordAsLend) "Saved to Lends" else "Only in History",
                        fontSize = 12.sp, color = TextSecondary
                    )
                }
                Switch(
                    checked = recordAsLend, onCheckedChange = onRecordToggle,
                    colors = SwitchDefaults.colors(checkedTrackColor = IncomeGreen)
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        // ── Pay Button — matches iOS ─────────────────────────────────────
        Button(
            onClick = onPay,
            enabled  = canPay,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(58.dp),
            shape  = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor    = Accent1,
                disabledContainerColor = BgCard
            )
        ) {
            Text(
                if (selectedApp != null) "Open ${selectedApp.label}" else "Log Payment Only",
                fontWeight = FontWeight.Bold, fontSize = 17.sp,
                color = if (canPay) Color.White else TextSecondary
            )
        }

        Spacer(Modifier.height(60.dp))
    }
}

// ── Form row helper ───────────────────────────────────────────────────────────
@Composable
private fun FormRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    placeholder: String, value: String, onChange: (String) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(icon, null, tint = TextSecondary, modifier = Modifier.size(24.dp))
        androidx.compose.foundation.text.BasicTextField(
            value = value, onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(color = TextPrimary, fontSize = 16.sp),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(Accent1),
            decorationBox = { inner ->
                if (value.isEmpty()) Text(placeholder, color = TextSecondary, fontSize = 16.sp)
                inner()
            }
        )
    }
}

// ── Split Sheet ────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitSheet(
    viewModel: MainViewModel,
    totalAmount: Double,
    onConfirm: (List<SplitFriendEntry>) -> Unit,
    onDismiss: () -> Unit
) {
    val contacts by viewModel.savedContacts.collectAsState()
    val friendsWithPhone = contacts.entries
        .filter { !it.value.isNullOrEmpty() }
        .sortedBy { it.key }

    var amountStr       by remember { mutableStateOf(if (totalAmount > 0) totalAmount.toLong().toString() else "") }
    var splitNote       by remember { mutableStateOf("") }
    var groupName       by remember { mutableStateOf("") }
    var selectedPhones  by remember { mutableStateOf(setOf<String>()) }

    val total     = amountStr.toDoubleOrNull() ?: 0.0
    val peopleCount = selectedPhones.size + 1
    val share     = if (peopleCount > 0) total / peopleCount else 0.0
    val canSplit  = total > 0 && selectedPhones.isNotEmpty() && splitNote.isNotBlank()

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = BgCard) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Split Expense", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                TextButton(onClick = onDismiss) { Text("Cancel", color = Accent1) }
            }

            // ── Amount card ────────────────────────────────────────────────
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(0.04f))
                    .border(BorderStroke(1.dp, Color.White.copy(0.08f)), RoundedCornerShape(24.dp))
                    .padding(vertical = 24.dp, horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("TOTAL AMOUNT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary, letterSpacing = 1.5.sp)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Text("₹", fontSize = 32.sp, fontWeight = FontWeight.SemiBold, color = Accent1.copy(0.8f))
                    androidx.compose.foundation.text.BasicTextField(
                        value = amountStr, onValueChange = { amountStr = it },
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 56.sp, fontWeight = FontWeight.Bold,
                            color = TextPrimary, textAlign = TextAlign.Center
                        ),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(Accent1),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        decorationBox = { inner ->
                            if (amountStr.isEmpty()) Text("0", fontSize = 56.sp, fontWeight = FontWeight.Bold, color = TextSecondary.copy(0.3f), textAlign = TextAlign.Center)
                            inner()
                        }
                    )
                }
            }

            // ── Details card ───────────────────────────────────────────────
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(BgCardAlt)
                    .border(BorderStroke(0.6.dp, Color.White.copy(0.055f)), RoundedCornerShape(20.dp))
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(Icons.Default.Edit, null, tint = Accent1, modifier = Modifier.size(20.dp))
                    androidx.compose.foundation.text.BasicTextField(
                        value = splitNote, onValueChange = { splitNote = it },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(color = TextPrimary, fontSize = 15.sp),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(Accent1),
                        decorationBox = { inner ->
                            if (splitNote.isEmpty()) Text("What was this for? (e.g. Dinner)", color = TextSecondary, fontSize = 15.sp)
                            inner()
                        }
                    )
                }
                HorizontalDivider(color = Color.White.copy(0.07f))
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(Icons.Default.LocalOffer, null, tint = Accent1, modifier = Modifier.size(20.dp))
                    androidx.compose.foundation.text.BasicTextField(
                        value = groupName, onValueChange = { groupName = it },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(color = TextPrimary, fontSize = 15.sp),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(Accent1),
                        decorationBox = { inner ->
                            if (groupName.isEmpty()) Text("Group Name (Optional)", color = TextSecondary, fontSize = 15.sp)
                            inner()
                        }
                    )
                }
            }

            // ── SPLIT WITH contact grid ─────────────────────────────────────
            Text("SPLIT WITH", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary, letterSpacing = 1.5.sp)

            if (friendsWithPhone.isEmpty()) {
                Text(
                    "Add contacts with phone numbers in the Payments tab to split bills.",
                    fontSize = 14.sp, color = TextSecondary
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // 2-column grid
                    friendsWithPhone.chunked(2).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            row.forEach { (name, phone) ->
                                val isSel = selectedPhones.contains(phone!!)
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSel) Accent1.copy(0.15f) else Color.White.copy(0.04f))
                                        .border(
                                            BorderStroke(1.dp, if (isSel) Accent1 else Color.White.copy(0.08f)),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            selectedPhones = if (isSel)
                                                selectedPhones - phone
                                            else
                                                selectedPhones + phone
                                        }
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            Modifier.size(20.dp).clip(CircleShape)
                                                .background(if (isSel) Accent1 else Color.White.copy(0.1f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSel) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(12.dp))
                                        }
                                        Text(
                                            name, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                                            color = if (isSel) TextPrimary else TextSecondary, maxLines = 1
                                        )
                                    }
                                }
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }

            // ── Summary ─────────────────────────────────────────────────────
            AnimatedVisibility(visible = canSplit) {
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "Split equally among $peopleCount people",
                        fontSize = 13.sp, color = TextSecondary
                    )
                    Text(
                        "Everyone pays ₹${share.toLong()}",
                        fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary
                    )
                }
            }

            // ── Split button ─────────────────────────────────────────────
            Button(
                onClick = {
                    if (!canSplit) return@Button
                    val selected = friendsWithPhone
                        .filter { selectedPhones.contains(it.value) }
                        .map { SplitFriendEntry(it.key, it.value, share) }
                    onConfirm(selected)
                },
                enabled  = canSplit,
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape    = RoundedCornerShape(20.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = Accent1, disabledContainerColor = BgCardAlt
                )
            ) {
                Text("Split Bill", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        }
    }
}

// ── Handle payment ─────────────────────────────────────────────────────────────
private fun handlePay(
    context: android.content.Context,
    viewModel: MainViewModel,
    recipientName: String, upiId: String,
    amount: Double, note: String, cat: AppCategory,
    app: UPIAppOption?, recordAsLend: Boolean,
    splitFriends: List<SplitFriendEntry>,
    contacts: Map<String, String?>
) {
    if (splitFriends.isNotEmpty()) {
        val myShare = maxOf(0.0, amount - splitFriends.sumOf { it.share })
        viewModel.splitBill(
            totalAmount = amount, myShare = myShare,
            note = note.ifEmpty { cat.name },
            groupName = recipientName,
            originalRecipient = recipientName,
            friends = splitFriends.map {
                com.moneymanager.app.data.repository.SplitFriend(name = it.name, phone = it.phone ?: "", share = it.share)
            }
        )
    } else {
        val tx = Transaction(
            amount = amount, recipientName = recipientName, upiId = upiId,
            note = note.ifEmpty { cat.name },
            categoryId = cat.id, categoryName = cat.name,
            categoryEmoji = cat.emoji, categoryHex = cat.colorHex,
            upiAppUsed = app?.label,
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

        val phone = contacts[recipientName] ?: upiId.takeIf { it.isNotEmpty() }
        if (!phone.isNullOrEmpty()) {
            viewModel.recordOutgoingPaymentForRecipient(recipientName, phone, amount, note.ifEmpty { cat.name })
        }
    }

    // Launch UPI deep link
    if (upiId.isNotEmpty() && app != null) {
        val upiUri = Uri.parse(
            "upi://pay?pa=${Uri.encode(upiId)}&pn=${Uri.encode(recipientName)}&am=$amount&cu=INR&tn=${Uri.encode(note.ifEmpty { cat.name })}"
        )
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, upiUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (_: Exception) {}
    }
}

// ── Legacy helpers still used by LendBorrowScreen ─────────────────────────────
@Composable
fun SectionCard(content: @Composable () -> Unit) {
    Card(
        colors   = CardDefaults.cardColors(containerColor = BgCard),
        shape    = RoundedCornerShape(18.dp),
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
    focusedBorderColor     = Accent1, unfocusedBorderColor = BgCardAlt,
    focusedTextColor       = TextPrimary, unfocusedTextColor = TextPrimary,
    cursorColor            = Accent1, focusedContainerColor = BgCardAlt,
    unfocusedContainerColor = BgCardAlt
)
