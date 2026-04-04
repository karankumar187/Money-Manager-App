package com.moneymanager.app.data.models

import com.google.firebase.Timestamp
import java.util.Date
import java.util.UUID

// ── Mirrors iOS Models.swift exactly ──────────────────────────────────────

enum class UPIApp(val label: String, val scheme: String, val color: Long) {
    GPAY("Google Pay",  "tez://",       0xFF1AA260),
    PHONEPE("PhonePe",  "phonepe://",   0xFF5F259F),
    KOTAK("Kotak 811",  "kotak811://",  0xFFEC1C24),
    SLICE("Slice",      "slicepay://",  0xFFFF2F77);

    fun makeUPIUri(upiId: String, name: String, amount: Double, note: String): String =
        "upi://pay?pa=${upiId}&pn=${name}&am=${amount}&cu=INR&tn=${note}"
}

enum class LendBorrowType(val label: String) {
    LENT("I Lent"),
    BORROWED("I Borrowed")
}

enum class AnalyticsPeriod(val label: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly")
}

data class AppCategory(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val emoji: String,
    val colorHex: String
) {
    companion object {
        val defaults = listOf(
            AppCategory(name = "Food",          emoji = "🍔", colorHex = "#FF9F0A"),
            AppCategory(name = "Transport",     emoji = "🚌", colorHex = "#5AC8FA"),
            AppCategory(name = "Shopping",      emoji = "🛍️", colorHex = "#7B61FF"),
            AppCategory(name = "Bills",         emoji = "🧾", colorHex = "#FF375F"),
            AppCategory(name = "Subscriptions", emoji = "📱", colorHex = "#30D158"),
            AppCategory(name = "Rent",          emoji = "🏠", colorHex = "#FF453A"),
            AppCategory(name = "Other",         emoji = "📦", colorHex = "#FFD60A"),
        )
    }
}

data class Transaction(
    val id: String = UUID.randomUUID().toString(),
    val amount: Double = 0.0,
    val recipientName: String = "",
    val upiId: String = "",
    val note: String = "",
    val categoryId: String = "",
    val categoryName: String = "",
    val categoryEmoji: String = "",
    val categoryHex: String = "#FF9F0A",
    val date: Date = Date(),
    val upiAppUsed: String? = null,
    val linkedLendId: String? = null,
    val splitGroupId: String? = null
)

data class LendBorrow(
    val id: String = UUID.randomUUID().toString(),
    val type: LendBorrowType = LendBorrowType.LENT,
    val personName: String = "",
    val contactPhone: String? = null,
    val amount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val note: String = "",
    val date: Date = Date(),
    val dueDate: Date? = null,
    val isPaid: Boolean = false,
    val paidDate: Date? = null,
    val splitGroupId: String? = null
) {
    val remainingAmount: Double get() = if (isPaid) 0.0 else maxOf(0.0, amount - paidAmount)
    val paidFraction: Double    get() = if (amount > 0) minOf(paidAmount / amount, 1.0) else 0.0
    val hasPartialPayment: Boolean get() = paidAmount > 0 && !isPaid

    val isDueSoon: Boolean get() {
        val d = dueDate ?: return false
        if (isPaid) return false
        val diff = d.time - System.currentTimeMillis()
        return diff in 0..(3 * 86400 * 1000L)
    }
    val isOverdue: Boolean get() {
        val d = dueDate ?: return false
        return !isPaid && d.time < System.currentTimeMillis()
    }
}

data class UserProfile(
    val id: String = "",
    val phone: String = "",
    val displayName: String = "",
    val upiId: String = "",
    val profileImageURL: String? = null,
    val createdAt: Date = Date()
)

data class SharedLedger(
    val id: String = UUID.randomUUID().toString(),
    val fromUID: String = "",
    val toUID: String = "",
    val fromPhone: String = "",
    val toPhone: String = "",
    val fromName: String = "",
    val toName: String = "",
    val amount: Double = 0.0,
    val note: String = "",
    val groupName: String? = null,
    val date: Date = Date(),
    val isPaid: Boolean = false,
    val paidDate: Date? = null
)

data class UPIQRData(
    val upiId: String,
    val name: String,
    val amount: Double?
)

// ── Helper: format currency ───────────────────────────────────────────────
fun formatAmount(amount: Double, symbol: String = "₹"): String {
    return if (amount == amount.toLong().toDouble())
        "$symbol${amount.toLong()}"
    else
        "$symbol${String.format("%.2f", amount)}"
}

// ── Helper: parse UPI deep link / QR string ──────────────────────────────
fun parseUPIString(raw: String): UPIQRData? {
    return try {
        val uri = android.net.Uri.parse(raw)
        val pa = uri.getQueryParameter("pa") ?: return null
        if (pa.isEmpty()) return null
        var pn = uri.getQueryParameter("pn") ?: ""
        val genericNames = listOf("phonepe merchant", "bharatpe merchant", "gpay merchant",
            "google pay merchant", "paytm merchant", "amazon pay merchant", "merchant", "bhim merchant")
        if (pn.lowercase().trim() in genericNames || pn.isEmpty()) {
            pn = pa.substringBefore("@")
                .replace(".", " ").replace("_", " ")
                .split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
        }
        val am = uri.getQueryParameter("am")?.toDoubleOrNull()
        UPIQRData(upiId = pa, name = pn, amount = am)
    } catch (e: Exception) { null }
}

// ── Firestore serialization ───────────────────────────────────────────────
fun Transaction.toFirestoreMap(): Map<String, Any?> = mapOf(
    "id" to id, "amount" to amount, "recipientName" to recipientName,
    "upiId" to upiId, "note" to note, "categoryId" to categoryId,
    "categoryName" to categoryName, "categoryEmoji" to categoryEmoji,
    "categoryHex" to categoryHex, "date" to Timestamp(date),
    "upiAppUsed" to upiAppUsed, "linkedLendId" to linkedLendId,
    "splitGroupId" to splitGroupId
)

fun Map<String, Any?>.toTransaction(): Transaction? {
    return try {
        val id = get("id") as? String ?: return null
        Transaction(
            id = id,
            amount = (get("amount") as? Number)?.toDouble() ?: 0.0,
            recipientName = get("recipientName") as? String ?: "",
            upiId = get("upiId") as? String ?: "",
            note = get("note") as? String ?: "",
            categoryId = get("categoryId") as? String ?: "",
            categoryName = get("categoryName") as? String ?: "",
            categoryEmoji = get("categoryEmoji") as? String ?: "",
            categoryHex = get("categoryHex") as? String ?: "#FF9F0A",
            date = (get("date") as? Timestamp)?.toDate() ?: Date(),
            upiAppUsed = get("upiAppUsed") as? String,
            linkedLendId = get("linkedLendId") as? String,
            splitGroupId = get("splitGroupId") as? String
        )
    } catch (e: Exception) { null }
}

fun LendBorrow.toFirestoreMap(): Map<String, Any?> = mapOf(
    "id" to id, "type" to type.name, "personName" to personName,
    "contactPhone" to contactPhone, "amount" to amount, "paidAmount" to paidAmount,
    "note" to note, "date" to Timestamp(date),
    "dueDate" to dueDate?.let { Timestamp(it) },
    "isPaid" to isPaid, "paidDate" to paidDate?.let { Timestamp(it) },
    "splitGroupId" to splitGroupId
)

fun Map<String, Any?>.toLendBorrow(): LendBorrow? {
    return try {
        val id = get("id") as? String ?: return null
        LendBorrow(
            id = id,
            type = LendBorrowType.valueOf(get("type") as? String ?: "LENT"),
            personName = get("personName") as? String ?: "",
            contactPhone = get("contactPhone") as? String,
            amount = (get("amount") as? Number)?.toDouble() ?: 0.0,
            paidAmount = (get("paidAmount") as? Number)?.toDouble() ?: 0.0,
            note = get("note") as? String ?: "",
            date = (get("date") as? Timestamp)?.toDate() ?: Date(),
            dueDate = (get("dueDate") as? Timestamp)?.toDate(),
            isPaid = get("isPaid") as? Boolean ?: false,
            paidDate = (get("paidDate") as? Timestamp)?.toDate(),
            splitGroupId = get("splitGroupId") as? String
        )
    } catch (e: Exception) { null }
}

fun Map<String, Any?>.toSharedLedger(): SharedLedger? {
    return try {
        val id = get("id") as? String ?: return null
        SharedLedger(
            id = id,
            fromUID = get("fromUID") as? String ?: "",
            toUID = get("toUID") as? String ?: "",
            fromPhone = get("fromPhone") as? String ?: "",
            toPhone = get("toPhone") as? String ?: "",
            fromName = get("fromName") as? String ?: "",
            toName = get("toName") as? String ?: "",
            amount = (get("amount") as? Number)?.toDouble() ?: 0.0,
            note = get("note") as? String ?: "",
            groupName = get("groupName") as? String,
            date = (get("date") as? Timestamp)?.toDate() ?: Date(),
            isPaid = get("isPaid") as? Boolean ?: false,
            paidDate = (get("paidDate") as? Timestamp)?.toDate()
        )
    } catch (e: Exception) { null }
}

fun Map<String, Any?>.toUserProfile(uid: String): UserProfile? {
    return try {
        val phone = get("phone") as? String ?: return null
        UserProfile(
            id = uid,
            phone = phone,
            displayName = get("displayName") as? String ?: "",
            upiId = get("upiId") as? String ?: "",
            profileImageURL = get("profileImageURL") as? String,
            createdAt = (get("createdAt") as? Timestamp)?.toDate() ?: Date()
        )
    } catch (e: Exception) { null }
}
