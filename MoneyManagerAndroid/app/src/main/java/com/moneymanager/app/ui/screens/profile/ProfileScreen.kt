package com.moneymanager.app.ui.screens.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import com.google.firebase.auth.FirebaseAuth
import com.moneymanager.app.ui.screens.payment.SectionCard
import com.moneymanager.app.ui.screens.payment.mmTextFieldColors
import com.moneymanager.app.ui.theme.*
import com.moneymanager.app.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val profile by viewModel.userProfile.collectAsState()
    val budget  by viewModel.monthlyBudget.collectAsState()
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current

    var name       by remember(profile) { mutableStateOf(profile?.displayName ?: "") }
    var phone      by remember(profile) { mutableStateOf(profile?.phone ?: "") }
    var upiId      by remember(profile) { mutableStateOf(profile?.upiId ?: "") }
    var budgetStr  by remember(budget)  { mutableStateOf(if (budget > 0) budget.toLong().toString() else "") }
    var currency   by remember { mutableStateOf("₹") }
    var saved      by remember { mutableStateOf(false) }

    val currencies = listOf("₹", "$", "€", "£", "¥")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile & Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Accent1) } },
                actions = {
                    TextButton(onClick = {
                        viewModel.saveProfile(name, upiId, phone, budgetStr.toDoubleOrNull() ?: 30000.0, currency)
                        saved = true
                    }) { Text("Save", color = Accent1, fontWeight = FontWeight.Bold) }
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
            // Avatar
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.size(80.dp).clip(CircleShape).background(avatarColor(name.ifEmpty { "U" }).copy(0.18f)).clickable { /* Image picker */ },
                    contentAlignment = Alignment.Center
                ) {
                    if (!profile?.profileImageURL.isNullOrEmpty()) {
                        AsyncImage(
                            model = profile!!.profileImageURL, contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(name.firstOrNull()?.uppercaseChar()?.toString() ?: "U",
                            fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, color = avatarColor(name.ifEmpty { "U" }))
                    }
                }
            }

            // Info fields
            SectionCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("PERSONAL INFO", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    OutlinedTextField(
                        value = name, onValueChange = { name = it },
                        label = { Text("Display Name", color = TextSecondary) },
                        leadingIcon = { Icon(Icons.Default.Person, null, tint = TextSecondary) },
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                        colors = mmTextFieldColors(), shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = phone, onValueChange = { phone = it },
                        label = { Text("Phone Number", color = TextSecondary) },
                        leadingIcon = { Icon(Icons.Default.Phone, null, tint = TextSecondary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                        colors = mmTextFieldColors(), shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = upiId, onValueChange = { upiId = it },
                        label = { Text("Your UPI ID", color = TextSecondary) },
                        leadingIcon = { Icon(Icons.Default.AccountBalance, null, tint = TextSecondary) },
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                        colors = mmTextFieldColors(), shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Budget
            SectionCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("BUDGET & CURRENCY", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    OutlinedTextField(
                        value = budgetStr, onValueChange = { budgetStr = it },
                        label = { Text("Monthly Budget", color = TextSecondary) },
                        prefix = { Text("₹", color = Accent1, fontWeight = FontWeight.Bold) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                        colors = mmTextFieldColors(), shape = RoundedCornerShape(12.dp)
                    )
                    // Currency picker
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        currencies.forEach { sym ->
                            FilterChip(
                                selected = currency == sym,
                                onClick = { currency = sym },
                                label = { Text(sym, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Accent1.copy(0.2f), selectedLabelColor = Accent1,
                                    containerColor = BgCardAlt, labelColor = TextSecondary
                                )
                            )
                        }
                    }
                }
            }

            if (saved) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = IncomeGreen.copy(0.1f)),
                    shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = IncomeGreen)
                        Spacer(Modifier.width(10.dp))
                        Text("Profile saved!", color = IncomeGreen, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Sign out
            SectionCard {
                Row(
                    Modifier.fillMaxWidth().clickable {
                        auth.signOut()
                        // Trigger recomposition to show auth screen
                    }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.Logout, null, tint = ExpenseRed)
                        Text("Sign Out", color = ExpenseRed, fontWeight = FontWeight.Medium, fontSize = 15.sp)
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = TextTertiary)
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}
