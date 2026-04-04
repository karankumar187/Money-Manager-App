package com.moneymanager.app.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.moneymanager.app.data.models.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID

// ── Mirrors CloudDataStore.swift exactly ──────────────────────────────────
class FirestoreRepository {

    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val uid  get() = auth.currentUser?.uid ?: ""

    private fun userRef() = db.collection("users").document(uid)
    private fun txCol()   = userRef().collection("transactions")
    private fun lbCol()   = userRef().collection("lendborrows")
    private fun catCol()  = userRef().collection("categories")
    private fun contactsCol() = userRef().collection("paymentContacts")

    // ── Profile ────────────────────────────────────────────────────────
    suspend fun loadProfile(): UserProfile? {
        val snap = userRef().get().await()
        return snap.data?.toUserProfile(uid)
    }

    suspend fun saveProfile(profile: UserProfile, imageURL: String? = null) {
        val map = mutableMapOf<String, Any?>(
            "id" to profile.id,
            "phone" to profile.phone,
            "displayName" to profile.displayName,
            "upiId" to profile.upiId,
            "createdAt" to Timestamp(profile.createdAt)
        )
        imageURL?.let { map["profileImageURL"] = it }
        userRef().set(map).await()
    }

    /**
     * Looks up another user's profile by phone number.
     * Used to auto-fill UPI ID when sending money to a contact who has the app.
     */
    suspend fun fetchContactProfile(phone: String): UserProfile? {
        val snap = db.collection("users")
            .whereEqualTo("phone", phone)
            .limit(1)
            .get().await()
        val doc = snap.documents.firstOrNull() ?: return null
        return doc.data?.toUserProfile(doc.id)
    }

    // ── Transactions ───────────────────────────────────────────────────
    fun listenTransactions(): Flow<List<Transaction>> = callbackFlow {
        val reg: ListenerRegistration = txCol()
            .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                val list = snap?.documents?.mapNotNull { it.data?.toTransaction() } ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    suspend fun addTransaction(tx: Transaction) {
        txCol().document(tx.id).set(tx.toFirestoreMap()).await()
    }

    suspend fun deleteTransaction(id: String) {
        txCol().document(id).delete().await()
    }

    // ── LendBorrows ────────────────────────────────────────────────────
    fun listenLendBorrows(): Flow<List<LendBorrow>> = callbackFlow {
        val reg = lbCol()
            .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                val list = snap?.documents?.mapNotNull { it.data?.toLendBorrow() } ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    suspend fun addLendBorrow(lb: LendBorrow) {
        lbCol().document(lb.id).set(lb.toFirestoreMap()).await()
    }

    suspend fun updateLendBorrow(lb: LendBorrow) {
        lbCol().document(lb.id).set(lb.toFirestoreMap()).await()
    }

    suspend fun deleteLendBorrow(id: String) {
        lbCol().document(id).delete().await()
    }

    suspend fun markPaid(id: String) {
        lbCol().document(id).update(
            mapOf("isPaid" to true, "paidDate" to Timestamp(Date()))
        ).await()
    }

    // ── Shared Ledgers (Split bills) ───────────────────────────────────
    fun listenSharedLedgers(myPhone: String): Flow<List<SharedLedger>> = callbackFlow {
        val results = mutableMapOf<String, SharedLedger>()

        // Listen from both sides (as sender and receiver)
        val r1 = db.collection("shared_ledgers")
            .whereEqualTo("fromPhone", myPhone)
            .addSnapshotListener { snap, _ ->
                snap?.documents?.mapNotNull { it.data?.toSharedLedger() }?.forEach { results[it.id] = it }
                trySend(results.values.sortedByDescending { it.date })
            }
        val r2 = db.collection("shared_ledgers")
            .whereEqualTo("toPhone", myPhone)
            .addSnapshotListener { snap, _ ->
                snap?.documents?.mapNotNull { it.data?.toSharedLedger() }?.forEach { results[it.id] = it }
                trySend(results.values.sortedByDescending { it.date })
            }
        awaitClose { r1.remove(); r2.remove() }
    }

    suspend fun markSharedLedgerPaid(ledgerId: String) {
        db.collection("shared_ledgers").document(ledgerId).update(
            mapOf("isPaid" to true, "paidDate" to Timestamp(Date()))
        ).await()
    }

    // ── Incoming Payments ──────────────────────────────────────────────
    fun listenIncomingPayments(myPhone: String): Flow<List<Map<String, Any>>> = callbackFlow {
        val reg = db.collection("incoming_payments")
            .whereEqualTo("toPhone", myPhone)
            .addSnapshotListener { snap, _ ->
                val list = snap?.documents?.map { it.data ?: emptyMap() } ?: emptyList()
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    suspend fun recordOutgoingPayment(
        toName: String, toPhone: String,
        amount: Double, note: String,
        fromName: String, fromPhone: String
    ) {
        if (toPhone.isEmpty() || fromPhone.isEmpty()) return
        val payId = UUID.randomUUID().toString()
        val map = mapOf(
            "id" to payId, "fromName" to fromName, "fromPhone" to fromPhone,
            "toName" to toName, "toPhone" to toPhone,
            "amount" to amount, "note" to note, "date" to Timestamp(Date())
        )
        db.collection("incoming_payments").document(payId).set(map).await()
    }

    // ── Payment Contacts (saved quick-pay contacts) ────────────────────
    suspend fun loadPaymentContacts(): Map<String, String?> {
        val snap = contactsCol().get().await()
        return snap.documents.associate {
            (it.getString("name") ?: "") to it.getString("phone")
        }
    }

    suspend fun savePaymentContact(name: String, phone: String?) {
        val doc = mapOf("name" to name, "phone" to phone)
        contactsCol().document(name.replace(" ", "_")).set(doc).await()
    }

    // ── Categories ─────────────────────────────────────────────────────
    fun listenCategories(): Flow<List<AppCategory>> = callbackFlow {
        val reg = catCol().addSnapshotListener { snap, _ ->
            val list = snap?.documents?.mapNotNull { doc ->
                doc.data?.let { d ->
                    AppCategory(
                        id = d["id"] as? String ?: doc.id,
                        name = d["name"] as? String ?: "",
                        emoji = d["emoji"] as? String ?: "",
                        colorHex = d["colorHex"] as? String ?: "#FF9F0A"
                    )
                }
            } ?: emptyList()
            if (list.isEmpty()) trySend(AppCategory.defaults)
            else trySend(list)
        }
        awaitClose { reg.remove() }
    }

    suspend fun saveCategory(cat: AppCategory) {
        catCol().document(cat.id).set(
            mapOf("id" to cat.id, "name" to cat.name, "emoji" to cat.emoji, "colorHex" to cat.colorHex)
        ).await()
    }

    suspend fun deleteCategory(id: String) {
        catCol().document(id).delete().await()
    }

    // ── Split Bill ─────────────────────────────────────────────────────
    suspend fun splitBill(
        totalAmount: Double,
        myShare: Double,
        note: String,
        groupName: String,
        originalRecipient: String,
        friends: List<SplitFriend>,
        myProfile: UserProfile,
        myDefaultCat: AppCategory
    ) {
        val groupId = UUID.randomUUID().toString()

        // My share as a transaction
        val myTx = Transaction(
            amount = myShare,
            recipientName = if (originalRecipient.isEmpty()) groupName else originalRecipient,
            note = note,
            categoryName = myDefaultCat.name,
            categoryEmoji = myDefaultCat.emoji,
            categoryHex = myDefaultCat.colorHex,
            date = Date(),
            splitGroupId = groupId
        )
        addTransaction(myTx)

        // For each friend — create a SharedLedger record
        for (friend in friends) {
            val ledgerId = UUID.randomUUID().toString()
            val ledger = mapOf(
                "id" to ledgerId,
                "fromUID" to uid,
                "toUID" to (friend.uid ?: ""),
                "fromPhone" to myProfile.phone,
                "toPhone" to (friend.phone ?: ""),
                "fromName" to myProfile.displayName,
                "toName" to friend.name,
                "amount" to friend.share,
                "note" to note,
                "groupName" to groupName,
                "date" to Timestamp(Date()),
                "isPaid" to false,
                "splitGroupId" to groupId
            )
            db.collection("shared_ledgers").document(ledgerId).set(ledger).await()

            // Lend record on my side
            val lb = LendBorrow(
                type = LendBorrowType.LENT,
                personName = friend.name,
                contactPhone = friend.phone,
                amount = friend.share,
                note = note,
                date = Date(),
                splitGroupId = groupId
            )
            addLendBorrow(lb)

            // Auto-save as payment contact
            savePaymentContact(friend.name, friend.phone)
        }
    }
}

data class SplitFriend(
    val name: String,
    val phone: String? = null,
    val uid: String? = null,
    val share: Double
)
