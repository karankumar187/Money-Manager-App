package com.moneymanager.app.ui.screens.profile

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.moneymanager.app.data.models.AppCategory
import com.moneymanager.app.ui.theme.*
import com.moneymanager.app.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val profile      by viewModel.userProfile.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val lendBorrows  by viewModel.lendBorrows.collectAsState()
    val categories   by viewModel.categories.collectAsState()
    val currency     by viewModel.currencySymbol.collectAsState()
    val budget       by viewModel.monthlyBudget.collectAsState()

    var showProfile   by remember { mutableStateOf(false) }
    var showBudget    by remember { mutableStateOf(false) }
    var showCurrency  by remember { mutableStateOf(false) }
    var showCategories by remember { mutableStateOf(false) }
    var showClearAlert by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(BgPrimary)
            .verticalScroll(rememberScrollState())
            .systemBarsPadding()
    ) {
        // ── Large "Settings" nav title ─────────────────────────────────────
        Text(
            "Settings",
            fontSize = 34.sp, fontWeight = FontWeight.Bold, color = TextPrimary,
            modifier = Modifier.padding(horizontal = 20.dp).padding(top = 16.dp, bottom = 28.dp)
        )

        // ── ACCOUNT section ───────────────────────────────────────────────
        SectionHeader("ACCOUNT")
        SettingsNavRow(
            icon = Icons.Default.Person, iconColor = Accent1,
            title = "Profile", subtitle = profile?.displayName ?: "Set up profile"
        ) { showProfile = true }

        Spacer(Modifier.height(20.dp))

        // ── FINANCE section ───────────────────────────────────────────────
        SectionHeader("FINANCE")

        SettingsNavRow(
            icon = Icons.Default.AccountBalance, iconColor = IncomeGreen,
            title = "Monthly Budget", subtitle = viewModel.formatted(budget)
        ) { showBudget = true }

        HorizontalDivider(Modifier.padding(start = 68.dp, end = 20.dp), color = Color.White.copy(0.04f))

        SettingsNavRow(
            icon = Icons.Default.CurrencyRupee, iconColor = Accent2,
            title = "Currency", subtitle = currency
        ) { showCurrency = true }

        Spacer(Modifier.height(20.dp))

        // ── CATEGORIES section ─────────────────────────────────────────────
        SectionHeader("CATEGORIES")
        SettingsNavRow(
            icon = Icons.Default.LocalOffer, iconColor = Color(0xFFA39BFF),
            title = "My Categories", subtitle = "${categories.size} active categories"
        ) { showCategories = true }

        Spacer(Modifier.height(20.dp))

        // ── STATISTICS section ─────────────────────────────────────────────
        SectionHeader("STATISTICS")
        SettingsInfoRow(Icons.Default.ShoppingCart, Accent2,    "Total Transactions", "${transactions.size}")
        HorizontalDivider(Modifier.padding(start = 68.dp, end = 20.dp), color = Color.White.copy(0.04f))
        SettingsInfoRow(Icons.Default.BarChart,     IncomeGreen, "Total Spent",  viewModel.formatted(transactions.sumOf { it.amount }))
        HorizontalDivider(Modifier.padding(start = 68.dp, end = 20.dp), color = Color.White.copy(0.04f))
        SettingsInfoRow(Icons.Default.ArrowUpward,  IncomeGreen, "Money Lent",   viewModel.formatted(lendBorrows.filter { it.type == com.moneymanager.app.data.models.LendBorrowType.LENT }.sumOf { it.amount }))
        HorizontalDivider(Modifier.padding(start = 68.dp, end = 20.dp), color = Color.White.copy(0.04f))
        SettingsInfoRow(Icons.Default.ArrowDownward, ExpenseRed, "Money Borrowed", viewModel.formatted(lendBorrows.filter { it.type == com.moneymanager.app.data.models.LendBorrowType.BORROWED }.sumOf { it.amount }))

        Spacer(Modifier.height(20.dp))

        // ── ABOUT section ──────────────────────────────────────────────────
        SectionHeader("ABOUT")
        SettingsInfoRow(Icons.Default.AppSettingsAlt, Accent1,       "Version", "1.0.0")
        HorizontalDivider(Modifier.padding(start = 68.dp, end = 20.dp), color = Color.White.copy(0.04f))
        SettingsInfoRow(Icons.Default.Code,            Color(0xFFFF9F0A), "Built with", "Kotlin & Jetpack Compose")

        Spacer(Modifier.height(20.dp))

        // ── ACCOUNT & DATA section ─────────────────────────────────────────
        SectionHeader("ACCOUNT & DATA")
        SettingsDestructiveRow(
            icon = Icons.Default.DeleteForever, title = "Clear All Data",
            subtitle = "Delete everything permanently"
        ) { showClearAlert = true }
        HorizontalDivider(Modifier.padding(start = 68.dp, end = 20.dp), color = Color.White.copy(0.04f))
        SettingsDestructiveRow(
            icon = Icons.Default.Logout, title = "Sign Out",
            subtitle = "Log out of your account"
        ) {
            FirebaseAuth.getInstance().signOut()
        }

        Spacer(Modifier.height(60.dp))
    }

    // ── Sub-screens as bottom sheets ───────────────────────────────────────
    if (showProfile)    ProfileSubScreen(viewModel) { showProfile = false }
    if (showBudget)     BudgetSubScreen(viewModel)  { showBudget = false }
    if (showCurrency)   CurrencySubScreen(viewModel) { showCurrency = false }
    if (showCategories) CategoriesSubScreen(viewModel, categories) { showCategories = false }

    if (showClearAlert) {
        AlertDialog(
            onDismissRequest = { showClearAlert = false },
            title = { Text("Clear All Data?", color = TextPrimary) },
            text  = { Text("This permanently deletes all transactions, lend/borrow records and custom categories.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = { showClearAlert = false }) {
                    Text("Delete Everything", color = ExpenseRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAlert = false }) {
                    Text("Cancel", color = Accent1)
                }
            },
            containerColor = BgCard
        )
    }
}

// ── Row components ─────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
        color = TextSecondary, letterSpacing = 0.8.sp,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
    )
}

@Composable
private fun SettingsNavRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector, iconColor: Color,
    title: String, subtitle: String, onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(BgCard)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(iconColor.copy(0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
            Text(subtitle, fontSize = 12.sp, color = TextSecondary)
        }
        Icon(Icons.Default.ChevronRight, null, tint = TextTertiary.copy(0.5f), modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SettingsInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector, iconColor: Color,
    title: String, value: String
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(BgCard)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(iconColor.copy(0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(18.dp))
        }
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary, modifier = Modifier.weight(1f))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
    }
}

@Composable
private fun SettingsDestructiveRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String, subtitle: String, onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(BgCard)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(ExpenseRed.copy(0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = ExpenseRed, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = ExpenseRed)
            Text(subtitle, fontSize = 12.sp, color = TextSecondary)
        }
    }
}

// ── Profile sub-screen ─────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileSubScreen(viewModel: MainViewModel, onDismiss: () -> Unit) {
    val profile  by viewModel.userProfile.collectAsState()
    var nameInput by remember { mutableStateOf(profile?.displayName ?: "") }
    var upiInput  by remember { mutableStateOf(profile?.upiId ?: "") }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = BgCard) {
        Column(
            Modifier.padding(24.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Profile", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)

            // Avatar circle
            Box(Modifier.size(90.dp), contentAlignment = Alignment.BottomEnd) {
                Box(
                    Modifier.fillMaxSize().clip(CircleShape).background(BgCardAlt)
                        .border(BorderStroke(0.8.dp, Color.White.copy(0.06f)), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (!profile?.profileImageURL.isNullOrEmpty()) {
                        AsyncImage(
                            model = profile!!.profileImageURL, contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            (profile?.displayName?.firstOrNull()?.uppercaseChar()?.toString() ?: "?"),
                            fontSize = 36.sp, fontWeight = FontWeight.Bold, color = TextPrimary
                        )
                    }
                }
                // Camera overlay badge
                Box(
                    Modifier.size(28.dp).clip(CircleShape).background(Color.Black.copy(0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }

            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Display Name", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                OutlinedTextField(
                    value = nameInput, onValueChange = { nameInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    colors = mmProfileTextFieldColors(), singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("UPI ID", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                OutlinedTextField(
                    value = upiInput, onValueChange = { upiInput = it },
                    modifier = Modifier.fillMaxWidth(), placeholder = { Text("yourname@bank", color = TextSecondary) },
                    colors = mmProfileTextFieldColors(), singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Button(
                onClick = {
                    val budget   = viewModel.monthlyBudget.value
                    val currency = viewModel.currencySymbol.value
                    val phone    = profile?.phone ?: ""
                    viewModel.saveProfile(nameInput, upiInput, phone, budget, currency)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Accent1)
            ) {
                Text("Save", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            }
        }
    }
}

// ── Budget sub-screen ──────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BudgetSubScreen(viewModel: MainViewModel, onDismiss: () -> Unit) {
    val budget  by viewModel.monthlyBudget.collectAsState()
    val fraction = viewModel.budgetFraction.toFloat().coerceIn(0f, 1f)
    var input   by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = BgCard) {
        Column(
            Modifier.padding(24.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("Monthly Budget", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)

            // Current budget card
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(BgCardAlt).padding(24.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Current Monthly Budget", fontSize = 13.sp, color = TextSecondary)
                    Text(viewModel.formatted(budget), fontSize = 42.sp, fontWeight = FontWeight.Bold, color = Accent1)

                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                        color    = if (fraction > 0.85f) ExpenseRed else IncomeGreen,
                        trackColor = Color.White.copy(0.1f)
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Spent: ${viewModel.formatted(viewModel.thisMonthTotal)}", fontSize = 12.sp, color = TextSecondary)
                        Text("${(fraction * 100).toInt()}% used", fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                            color = if (fraction > 0.85f) ExpenseRed else TextSecondary)
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Set New Budget", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
                OutlinedTextField(
                    value = input, onValueChange = { input = it },
                    modifier = Modifier.fillMaxWidth(),
                    prefix = { Text("₹", color = Accent1, fontWeight = FontWeight.Bold) },
                    placeholder = { Text("Enter amount", color = TextSecondary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = mmProfileTextFieldColors(), singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Button(
                onClick = {
                    val v = input.toDoubleOrNull() ?: return@Button
                    val p = viewModel.userProfile.value
                    viewModel.saveProfile(p?.displayName ?: "", p?.upiId ?: "", p?.phone ?: "", v, viewModel.currencySymbol.value)
                    onDismiss()
                },
                enabled  = input.toDoubleOrNull() != null,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = Accent1)
            ) {
                Text("Set Budget", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            }
        }
    }
}

// ── Currency sub-screen ────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencySubScreen(viewModel: MainViewModel, onDismiss: () -> Unit) {
    val current by viewModel.currencySymbol.collectAsState()
    val currencies = listOf(
        "₹" to "Indian Rupee", "$" to "US Dollar", "€" to "Euro",
        "£" to "British Pound", "¥" to "Japanese Yen", "د.إ" to "UAE Dirham"
    )

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = BgCard) {
        Column(
            Modifier.padding(top = 8.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Text(
                "Currency", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )
            currencies.forEach { (symbol, name) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            val p = viewModel.userProfile.value
                            viewModel.saveProfile(p?.displayName ?: "", p?.upiId ?: "", p?.phone ?: "", viewModel.monthlyBudget.value, symbol)
                            onDismiss()
                        }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(symbol, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Accent1, modifier = Modifier.width(36.dp))
                    Column(Modifier.weight(1f)) {
                        Text(name, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                        Text(symbol, fontSize = 12.sp, color = TextSecondary)
                    }
                    if (current == symbol) {
                        Icon(Icons.Default.CheckCircle, null, tint = Accent1, modifier = Modifier.size(22.dp))
                    }
                }
                HorizontalDivider(Modifier.padding(start = 72.dp), color = Color.White.copy(0.04f))
            }
        }
    }
}

// ── Categories sub-screen ──────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoriesSubScreen(viewModel: MainViewModel, categories: List<AppCategory>, onDismiss: () -> Unit) {
    var newEmoji by remember { mutableStateOf("") }
    var newName  by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = BgCard) {
        Column(
            Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("My Categories", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary,
                modifier = Modifier.padding(top = 8.dp))

            categories.forEach { cat ->
                val color = hexColor(cat.colorHex)
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        Modifier.size(36.dp).clip(CircleShape).background(color.copy(0.18f)),
                        contentAlignment = Alignment.Center
                    ) { Text(cat.emoji, fontSize = 18.sp) }
                    Text(cat.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary, modifier = Modifier.weight(1f))
                    if (!AppCategory.defaults.any { it.id == cat.id }) {
                        IconButton(onClick = { viewModel.deleteCategory(cat.id) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
                HorizontalDivider(color = Color.White.copy(0.04f))
            }

            Spacer(Modifier.height(8.dp))
            Text("Add Custom Category", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = newEmoji, onValueChange = { newEmoji = it },
                    placeholder = { Text("📦", color = TextSecondary) },
                    modifier = Modifier.width(70.dp), singleLine = true,
                    colors = mmProfileTextFieldColors(), shape = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value = newName, onValueChange = { newName = it },
                    placeholder = { Text("Category name", color = TextSecondary) },
                    modifier = Modifier.weight(1f), singleLine = true,
                    colors = mmProfileTextFieldColors(), shape = RoundedCornerShape(10.dp)
                )
                IconButton(
                    onClick = {
                        if (newName.isNotBlank()) {
                            viewModel.addCategory(AppCategory(
                                name = newName.trim(),
                                emoji = newEmoji.trim().ifEmpty { "📦" },
                                colorHex = "#7B61FF"
                            ))
                            newEmoji = ""; newName = ""
                        }
                    },
                    modifier = Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)).background(Accent1)
                ) {
                    Icon(Icons.Default.Add, null, tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun mmProfileTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = Accent1, unfocusedBorderColor = BgCardAlt,
    focusedTextColor     = TextPrimary, unfocusedTextColor = TextPrimary,
    cursorColor          = Accent1, focusedContainerColor = BgCardAlt,
    unfocusedContainerColor = BgCardAlt, focusedLabelColor = Accent1
)
