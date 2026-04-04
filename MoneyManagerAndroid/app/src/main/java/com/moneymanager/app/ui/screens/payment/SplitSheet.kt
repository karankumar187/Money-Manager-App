package com.moneymanager.app.ui.screens.payment

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moneymanager.app.ui.theme.*
import com.moneymanager.app.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitSheet(
    viewModel: MainViewModel,
    totalAmount: Double,
    onConfirm: (List<SplitFriendEntry>) -> Unit,
    onDismiss: () -> Unit
) {
    val contacts by viewModel.savedContacts.collectAsState()
    var friends by remember { mutableStateOf<List<SplitFriendEntry>>(emptyList()) }
    var newName by remember { mutableStateOf("") }
    var newShare by remember { mutableStateOf("") }

    val totalSplit = friends.sumOf { it.share }
    val myShare = maxOf(0.0, totalAmount - totalSplit)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = BgCard,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Split Bill", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                TextButton(onClick = { if (friends.isNotEmpty()) onConfirm(friends) else onDismiss() }) {
                    Text("Done", color = Accent1, fontWeight = FontWeight.Bold)
                }
            }

            // Summary
            Card(
                colors = CardDefaults.cardColors(containerColor = BgCardAlt),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Total", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text("₹${totalAmount.toLong()}", fontWeight = FontWeight.ExtraBold, color = TextPrimary, fontSize = 18.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("My Share", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text("₹${myShare.toLong()}", fontWeight = FontWeight.ExtraBold, color = IncomeGreen, fontSize = 18.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Others Pay", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text("₹${totalSplit.toLong()}", fontWeight = FontWeight.ExtraBold, color = AccentBlue, fontSize = 18.sp)
                    }
                }
            }

            // Add friend row
            Card(
                colors = CardDefaults.cardColors(containerColor = BgCardAlt),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newName, onValueChange = { newName = it },
                        placeholder = { Text("Name", color = TextTertiary) },
                        singleLine = true, modifier = Modifier.weight(1.5f),
                        colors = mmTextFieldColors(), shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = newShare, onValueChange = { newShare = it },
                        placeholder = { Text("₹ Share", color = TextTertiary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true, modifier = Modifier.weight(1f),
                        colors = mmTextFieldColors(), shape = RoundedCornerShape(10.dp)
                    )
                    IconButton(
                        onClick = {
                            val share = newShare.toDoubleOrNull() ?: return@IconButton
                            if (newName.isNotBlank() && share > 0) {
                                friends = friends + SplitFriendEntry(
                                    name = newName.trim(),
                                    phone = contacts[newName.trim()],
                                    share = share
                                )
                                newName = ""; newShare = ""
                            }
                        },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(Icons.Default.AddCircle, null, tint = Accent1, modifier = Modifier.size(28.dp))
                    }
                }
            }

            // Friend list
            if (friends.isNotEmpty()) {
                Text("SPLITTING WITH", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                friends.forEachIndexed { idx, friend ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = BgCardAlt),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Person, null, tint = AccentBlue, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(friend.name, fontWeight = FontWeight.SemiBold, color = TextPrimary, modifier = Modifier.weight(1f))
                            Text("₹${friend.share.toLong()}", fontWeight = FontWeight.Bold, color = AccentBlue)
                            Spacer(Modifier.width(8.dp))
                            IconButton(
                                onClick = { friends = friends.toMutableList().also { it.removeAt(idx) } },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Close, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
