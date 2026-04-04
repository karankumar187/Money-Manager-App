package com.moneymanager.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.moneymanager.app.data.models.*
import com.moneymanager.app.data.repository.FirestoreRepository
import com.moneymanager.app.data.repository.SplitFriend
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID

// ── Mirrors CloudDataStore — single source of truth for all UI state ──────
class MainViewModel : ViewModel() {

    private val repo = FirestoreRepository()
    private val auth = FirebaseAuth.getInstance()

    // ── Published state ─────────────────────────────────────────────────
    private val _transactions   = MutableStateFlow<List<Transaction>>(emptyList())
    private val _lendBorrows    = MutableStateFlow<List<LendBorrow>>(emptyList())
    private val _sharedLedgers  = MutableStateFlow<List<SharedLedger>>(emptyList())
    private val _categories     = MutableStateFlow(AppCategory.defaults)
    private val _savedContacts  = MutableStateFlow<Map<String, String?>>(emptyMap())
    private val _savedUPIs      = MutableStateFlow<Map<String, String>>(emptyMap())
    private val _userProfile    = MutableStateFlow<UserProfile?>(null)
    private val _monthlyBudget  = MutableStateFlow(30000.0)
    private val _currencySymbol = MutableStateFlow("₹")
    private val _isLoading      = MutableStateFlow(true)

    val transactions   = _transactions.asStateFlow()
    val lendBorrows    = _lendBorrows.asStateFlow()
    val sharedLedgers  = _sharedLedgers.asStateFlow()
    val categories     = _categories.asStateFlow()
    val savedContacts  = _savedContacts.asStateFlow()
    val savedUPIs      = _savedUPIs.asStateFlow()
    val userProfile    = _userProfile.asStateFlow()
    val monthlyBudget  = _monthlyBudget.asStateFlow()
    val currencySymbol = _currencySymbol.asStateFlow()
    val isLoading      = _isLoading.asStateFlow()
    val isSignedIn     get() = auth.currentUser != null

    // ── Stats (computed) ─────────────────────────────────────────────────
    val todayTotal get() = _transactions.value
        .filter { isToday(it.date) }.sumOf { it.amount }
    val thisMonthTotal get() = _transactions.value
        .filter { isSameMonth(it.date) }.sumOf { it.amount }
    val budgetFraction get() = minOf(thisMonthTotal / _monthlyBudget.value.coerceAtLeast(1.0), 1.0)
    val totalLent get() = _lendBorrows.value
        .filter { it.type == LendBorrowType.LENT && !it.isPaid }.sumOf { it.remainingAmount }
    val totalBorrowed get() = _lendBorrows.value
        .filter { it.type == LendBorrowType.BORROWED && !it.isPaid }.sumOf { it.remainingAmount }

    fun formatted(amount: Double) = formatAmount(amount, _currencySymbol.value)

    // ── Init / Start listening ───────────────────────────────────────────
    fun startListening() {
        viewModelScope.launch {
            val profile = repo.loadProfile()
            _userProfile.value = profile
            profile?.let {
                _monthlyBudget.value = it.createdAt.time.toDouble().coerceAtLeast(1.0) // placeholder
                _currencySymbol.value = "₹"
            }
            _isLoading.value = false
        }
        viewModelScope.launch {
            repo.listenTransactions().collect { _transactions.value = it }
        }
        viewModelScope.launch {
            repo.listenLendBorrows().collect { _lendBorrows.value = it }
        }
        viewModelScope.launch {
            repo.listenCategories().collect { _categories.value = it }
        }
        viewModelScope.launch {
            val myPhone = _userProfile.value?.phone ?: ""
            if (myPhone.isNotEmpty()) {
                repo.listenSharedLedgers(myPhone).collect { _sharedLedgers.value = it }
            }
        }
        viewModelScope.launch {
            val myPhone = _userProfile.value?.phone ?: ""
            if (myPhone.isNotEmpty()) {
                repo.listenIncomingPayments(myPhone).collect { payments ->
                    syncIncomingPayments(payments)
                }
            }
        }
        viewModelScope.launch {
            val contacts = repo.loadPaymentContacts()
            _savedContacts.value = contacts
        }
    }

    // ── Incoming Payments Sync ───────────────────────────────────────────
    private fun syncIncomingPayments(payments: List<Map<String, Any>>) {
        viewModelScope.launch {
            val defaultCat = _categories.value.firstOrNull() ?: AppCategory.defaults[0]
            for (pay in payments) {
                val payId = pay["id"] as? String ?: continue
                val fromName = pay["fromName"] as? String ?: continue
                val fromPhone = pay["fromPhone"] as? String ?: continue
                val amount = (pay["amount"] as? Number)?.toDouble() ?: continue
                val note = pay["note"] as? String ?: "Payment received"
                val marker = "ip:$payId"
                val alreadySynced = _transactions.value.any { it.note.contains(marker) }
                if (alreadySynced) continue

                savePaymentContact(fromName, fromPhone)
                addTransaction(Transaction(
                    amount = amount,
                    recipientName = fromName,
                    upiId = fromPhone,
                    note = "$note [$marker]",
                    categoryName = "Received",
                    categoryEmoji = "📥",
                    categoryHex = defaultCat.colorHex,
                    date = Date()
                ))
            }
        }
    }

    // ── Transaction CRUD ─────────────────────────────────────────────────
    fun addTransaction(tx: Transaction) = viewModelScope.launch { repo.addTransaction(tx) }
    fun deleteTransaction(id: String)    = viewModelScope.launch { repo.deleteTransaction(id) }

    // ── LendBorrow CRUD ──────────────────────────────────────────────────
    fun addLendBorrow(lb: LendBorrow)    = viewModelScope.launch { repo.addLendBorrow(lb) }
    fun updateLendBorrow(lb: LendBorrow) = viewModelScope.launch { repo.updateLendBorrow(lb) }
    fun deleteLendBorrow(id: String)     = viewModelScope.launch { repo.deleteLendBorrow(id) }
    fun markPaid(id: String)             = viewModelScope.launch { repo.markPaid(id) }

    fun addPartialPayment(id: String, paidNow: Double) {
        val lb = _lendBorrows.value.firstOrNull { it.id == id } ?: return
        val updated = lb.copy(
            paidAmount = minOf(lb.amount, lb.paidAmount + paidNow),
            isPaid = lb.paidAmount + paidNow >= lb.amount,
            paidDate = if (lb.paidAmount + paidNow >= lb.amount) Date() else null
        )
        updateLendBorrow(updated)
    }

    // ── Payment Contacts ─────────────────────────────────────────────────
    fun savePaymentContact(name: String, phone: String?) {
        viewModelScope.launch {
            if (name.isBlank()) return@launch
            _savedContacts.value = _savedContacts.value.toMutableMap().apply { put(name, phone) }
            repo.savePaymentContact(name, phone)
        }
    }

    fun saveUPI(name: String, upi: String) {
        if (name.isBlank() || upi.isBlank()) return
        _savedUPIs.value = _savedUPIs.value.toMutableMap().apply { put(name, upi) }
    }

    // ── Split Bill ───────────────────────────────────────────────────────
    fun splitBill(
        totalAmount: Double, myShare: Double,
        note: String, groupName: String,
        originalRecipient: String, friends: List<SplitFriend>
    ) {
        val profile = _userProfile.value ?: return
        val cat = _categories.value.firstOrNull() ?: AppCategory.defaults[0]
        viewModelScope.launch {
            repo.splitBill(totalAmount, myShare, note, groupName, originalRecipient, friends, profile, cat)
        }
    }

    // ── Outgoing Payment Notification ────────────────────────────────────
    fun recordOutgoingPaymentForRecipient(toName: String, toPhone: String, amount: Double, note: String) {
        if (toPhone.isEmpty()) return
        val profile = _userProfile.value ?: return
        viewModelScope.launch {
            repo.recordOutgoingPayment(toName, toPhone, amount, note, profile.displayName, profile.phone)
        }
    }

    // ── Contact Profile Lookup ───────────────────────────────────────────
    suspend fun fetchContactProfile(phone: String): UserProfile? = repo.fetchContactProfile(phone)

    // ── Shared Ledger ────────────────────────────────────────────────────
    fun markSharedLedgerPaid(ledgerId: String) = viewModelScope.launch { repo.markSharedLedgerPaid(ledgerId) }

    // ── Profile ──────────────────────────────────────────────────────────
    fun saveProfile(name: String, upiId: String, phone: String, budget: Double, currency: String) {
        val profile = UserProfile(
            id = auth.currentUser?.uid ?: "",
            phone = phone,
            displayName = name,
            upiId = upiId,
            createdAt = Date()
        )
        _userProfile.value = profile
        _monthlyBudget.value = budget
        _currencySymbol.value = currency
        viewModelScope.launch { repo.saveProfile(profile) }
    }

    // ── Categories ───────────────────────────────────────────────────────
    fun addCategory(cat: AppCategory)    = viewModelScope.launch { repo.saveCategory(cat) }
    fun updateCategory(cat: AppCategory) = viewModelScope.launch { repo.saveCategory(cat) }
    fun deleteCategory(id: String)       = viewModelScope.launch { repo.deleteCategory(id) }

    // ── Date helpers ─────────────────────────────────────────────────────
    private fun isToday(date: Date): Boolean {
        val cal = java.util.Calendar.getInstance()
        val c2  = java.util.Calendar.getInstance().apply { time = date }
        return cal.get(java.util.Calendar.DAY_OF_YEAR) == c2.get(java.util.Calendar.DAY_OF_YEAR) &&
                cal.get(java.util.Calendar.YEAR) == c2.get(java.util.Calendar.YEAR)
    }
    private fun isSameMonth(date: Date): Boolean {
        val cal = java.util.Calendar.getInstance()
        val c2  = java.util.Calendar.getInstance().apply { time = date }
        return cal.get(java.util.Calendar.MONTH) == c2.get(java.util.Calendar.MONTH) &&
                cal.get(java.util.Calendar.YEAR) == c2.get(java.util.Calendar.YEAR)
    }
}
