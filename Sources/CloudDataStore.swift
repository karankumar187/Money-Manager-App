import Foundation
import FirebaseFirestore
import FirebaseAuth
import FirebaseStorage
import SwiftUI

// MARK: - CloudDataStore
// Replaces the local DataStore as the single source of truth.
// All data is persisted to Firestore and listened to in real-time.

@MainActor
class CloudDataStore: ObservableObject {

    // ── Published State ─────────────────────────────────────────────
    @Published var transactions   : [Transaction]   = []
    @Published var lendBorrows    : [LendBorrow]    = []
    @Published var categories     : [AppCategory]   = AppCategory.defaultCategories
    @Published var sharedLedgers  : [SharedLedger]  = []
    @Published var savedContacts  : [String: String?] = [:]
    @Published var savedAvatars   : [String: Data]    = [:]
    @Published var savedUPIs      : [String: String]  = [:]
    @Published var userProfile    : UserProfile?    = nil
    @Published var monthlyBudget  : Double          = 30000
    @Published var userName       : String          = "User"
    @Published var currencySymbol : String          = "₹"
    @Published var userProfileImageData: Data?      = nil
    @Published var isLoading      : Bool            = true

    // ── Firebase References ──────────────────────────────────────────
    private let db      = Firestore.firestore()
    private let storage = Storage.storage()
    private var uid     : String { Auth.auth().currentUser?.uid ?? "" }

    private var txListener     : ListenerRegistration? = nil
    private var lbListener     : ListenerRegistration? = nil
    private var catListener    : ListenerRegistration? = nil
    private var ledgerListener : ListenerRegistration? = nil

    // ── Init ─────────────────────────────────────────────────────────
    init() {
        if let d = UserDefaults.standard.data(forKey: "savedAvatars_v4"),
           let v = try? JSONDecoder().decode([String: Data].self, from: d) {
            self.savedAvatars = v
        }
    }

    // Called once auth is confirmed & migration is done
    func startListening() {
        guard !uid.isEmpty else { return }
        isLoading = true
        loadProfile()
        listenTransactions()
        listenLendBorrows()
        listenCategories()
        listenSharedLedgers()
        isLoading = false
    }

    func stopListening() {
        txListener?.remove();     txListener     = nil
        lbListener?.remove();     lbListener     = nil
        catListener?.remove();    catListener    = nil
        ledgerListener?.remove(); ledgerListener = nil
    }

    // ── Helpers ───────────────────────────────────────────────────────
    func formatted(_ v: Double) -> String { fmt(v, symbol: currencySymbol) }

    private func userRef() -> DocumentReference {
        db.collection("users").document(uid)
    }

    // ── Profile ───────────────────────────────────────────────────────
    func loadProfile() {
        userRef().getDocument { [weak self] snap, _ in
            guard let self, let data = snap?.data() else { return }
            self.monthlyBudget  = data["budget"]         as? Double ?? 30000
            self.userName       = data["displayName"]    as? String ?? "User"
            self.currencySymbol = data["currencySymbol"] as? String ?? "₹"
            if let p = UserProfile.from(data, uid: self.uid) {
                self.userProfile = p
                self.userName    = p.displayName
            }
        }
    }

    func saveProfile(name: String, upiId: String, phone: String, imageData: Data?) async {
        var imageURL: String? = userProfile?.profileImageURL
        if let data = imageData {
            imageURL = await uploadProfileImage(data)
        }
        let profile = UserProfile(
            id: uid, phone: phone, displayName: name,
            upiId: upiId, profileImageURL: imageURL, createdAt: Date()
        )
        var dict = profile.toDict()
        dict["budget"]          = monthlyBudget
        dict["currencySymbol"]  = currencySymbol
        do {
            try await userRef().setData(dict, merge: true)
        } catch {
            print("Error saving profile: \(error)")
        }
        userProfile = profile
        userName    = name
    }

    private func uploadProfileImage(_ data: Data) async -> String? {
        let ref = storage.reference().child("profiles/\(uid)/avatar.jpg")
        do {
            _ = try await ref.putDataAsync(data)
            let url = try await ref.downloadURL()
            return url.absoluteString
        } catch {
            return nil
        }
    }

    func saveSetting(budget: Double? = nil, currency: String? = nil, name: String? = nil) {
        var update: [String: Any] = [:]
        if let b = budget   { monthlyBudget  = b; update["budget"]         = b }
        if let c = currency { currencySymbol = c; update["currencySymbol"] = c }
        if let n = name     { userName       = n; update["displayName"]    = n }
        userRef().setData(update, merge: true)
    }

    // ── Convenience aliases matching old DataStore API ───────────────
    func setName(_ name: String)         { saveSetting(name: name) }
    func setCurrency(_ symbol: String)   { saveSetting(currency: symbol) }

    func setProfileImage(_ data: Data) {
        userProfileImageData = data
        UserDefaults.standard.set(data, forKey: "mm_profile_image")
        // Also upload async in background
        Task { await uploadProfileImageAndSave(data) }
    }

    private func uploadProfileImageAndSave(_ data: Data) async {
        if let url = await uploadProfileImage(data) {
            do {
                try await userRef().setData(["profileImageURL": url], merge: true)
            } catch {
                print("Failed to save profile image URL: \(error)")
            }
        }
    }

    // Category helpers with old naming convention
    func deleteAppCategory(id: UUID) {
        deleteCategory(categories.first(where: { $0.id == id }) ?? AppCategory(id: id, name: "", emoji: "", colorHex: ""))
    }

    func updateAppCategory(_ c: AppCategory) {
        updateCategory(c)
    }

    func addAppCategory(_ c: AppCategory) {
        addCategory(c)
    }

    // ── Real-time Listeners ───────────────────────────────────────────
    private func listenTransactions() {
        txListener = userRef().collection("transactions")
            .order(by: "date", descending: true)
            .addSnapshotListener { [weak self] snap, _ in
                guard let self, let docs = snap?.documents else { return }
                self.transactions = docs.compactMap { Transaction.fromFirestore($0.data()) }
            }
    }

    private func listenLendBorrows() {
        lbListener = userRef().collection("lendborrows")
            .order(by: "date", descending: true)
            .addSnapshotListener { [weak self] snap, _ in
                guard let self, let docs = snap?.documents else { return }
                self.lendBorrows = docs.compactMap { LendBorrow.fromFirestore($0.data()) }
            }
    }

    private func listenCategories() {
        catListener = userRef().collection("categories")
            .addSnapshotListener { [weak self] snap, _ in
                guard let self, let docs = snap?.documents else { return }
                let loaded = docs.compactMap { AppCategory.fromFirestore($0.data()) }
                self.categories = loaded.isEmpty ? AppCategory.defaultCategories : loaded
            }
    }

    private func listenSharedLedgers() {
        // Listen to ledgers where I'm either from or to
        let phone = userProfile?.phone ?? ""
        ledgerListener = db.collection("shared_ledgers")
            .whereField("fromPhone", isEqualTo: phone)
            .addSnapshotListener { [weak self] snap, _ in
                guard let self, let docs = snap?.documents else { return }
                let from = docs.compactMap { SharedLedger.from($0.data()) }
                self.mergeSharedLedgers(from)
            }
        // Second listener for incoming ledgers
        db.collection("shared_ledgers")
            .whereField("toPhone", isEqualTo: phone)
            .addSnapshotListener { [weak self] snap, _ in
                guard let self, let docs = snap?.documents else { return }
                let to = docs.compactMap { SharedLedger.from($0.data()) }
                self.mergeSharedLedgers(to)
            }
    }

    private func mergeSharedLedgers(_ new: [SharedLedger]) {
        var dict = Dictionary(uniqueKeysWithValues: sharedLedgers.map { ($0.id, $0) })
        for l in new { dict[l.id] = l }
        sharedLedgers = Array(dict.values).sorted { $0.date > $1.date }
    }

    // ── MARK: Stats ──────────────────────────────────────────────────
    var todayTotal: Double {
        transactions.filter { Calendar.current.isDateInToday($0.date) }.reduce(0) { $0 + $1.amount }
    }
    var thisMonthTotal: Double {
        let start = Calendar.current.date(from: Calendar.current.dateComponents([.year,.month], from: Date()))!
        return transactions.filter { $0.date >= start }.reduce(0) { $0 + $1.amount }
    }
    var thisWeekTotal: Double {
        let cal = Calendar.current
        let start = cal.date(from: cal.dateComponents([.yearForWeekOfYear, .weekOfYear], from: Date()))!
        return transactions.filter { $0.date >= start }.reduce(0) { $0 + $1.amount }
    }
    var averageDailySpend: Double {
        let cal = Calendar.current
        let start = cal.date(from: cal.dateComponents([.year,.month], from: Date()))!
        let days = max(1, cal.dateComponents([.day], from: start, to: Date()).day ?? 1)
        return thisMonthTotal / Double(days)
    }
    // Short aliases matching old DataStore API
    var totalLent: Double { totalLentOutstanding }
    var totalBorrowed: Double { totalBorrowedOutstanding }

    var totalLentOutstanding: Double {
        lendBorrows.filter { $0.type == .lent && !$0.isPaid }.reduce(0) { $0 + $1.remainingAmount }
    }
    var totalBorrowedOutstanding: Double {
        lendBorrows.filter { $0.type == .borrowed && !$0.isPaid }.reduce(0) { $0 + $1.remainingAmount }
    }

    // ── MARK: Transactions ────────────────────────────────────────────
    func addTransaction(_ t: Transaction) {
        let ref = userRef().collection("transactions").document(t.id.uuidString)
        ref.setData(t.toFirestoreDict())
    }

    func deleteTransaction(_ t: Transaction) {
        userRef().collection("transactions").document(t.id.uuidString).delete()
    }

    // Overload matching old DataStore API
    func deleteTransaction(id: UUID) {
        userRef().collection("transactions").document(id.uuidString).delete()
    }

    func saveUPI(name: String, upi: String) {
        savedUPIs[name] = upi
        userRef().setData(["savedUPIs": savedUPIs], merge: true)
    }

    // ── MARK: Lend/Borrow ────────────────────────────────────────────
    func addLendBorrow(_ lb: LendBorrow) {
        userRef().collection("lendborrows").document(lb.id.uuidString).setData(lb.toFirestoreDict())
    }

    func updateLendBorrow(_ lb: LendBorrow) {
        userRef().collection("lendborrows").document(lb.id.uuidString).setData(lb.toFirestoreDict())
    }

    func deleteLendBorrow(_ lb: LendBorrow) {
        userRef().collection("lendborrows").document(lb.id.uuidString).delete()
    }

    func markPaid(_ lb: LendBorrow) {
        var updated = lb
        updated.isPaid      = true
        updated.paidAmount  = lb.amount
        updated.paidDate    = Date()
        updateLendBorrow(updated)
    }

    // Overload matching old DataStore API
    func markPaid(id: UUID) {
        guard let lb = lendBorrows.first(where: { $0.id == id }) else { return }
        markPaid(lb)
    }

    func deleteLendBorrow(id: UUID) {
        userRef().collection("lendborrows").document(id.uuidString).delete()
    }

    func addPartialPayment(to lb: LendBorrow, amount: Double) {
        var updated = lb
        updated.paidAmount  = min(lb.amount, lb.paidAmount + amount)
        updated.isPaid      = updated.paidAmount >= lb.amount
        if updated.isPaid { updated.paidDate = Date() }
        updateLendBorrow(updated)
    }

    // Overload matching the old DataStore API — looks up entry by UUID
    func addPartialPayment(id: UUID, paidNow: Double) {
        guard let lb = lendBorrows.first(where: { $0.id == id }) else { return }
        addPartialPayment(to: lb, amount: paidNow)
    }

    // ── MARK: Shared Ledgers (Splitwise) ─────────────────────────────
    func addSharedLedger(_ ledger: SharedLedger) {
        db.collection("shared_ledgers").document(ledger.id).setData(ledger.toDict())
        // Also add matching personal lend record
        let lb = LendBorrow(
            id: UUID(), type: .lent, personName: ledger.toName,
            contactPhone: ledger.toPhone,
            amount: ledger.amount, note: ledger.note, date: ledger.date
        )
        addLendBorrow(lb)
    }

    func markSharedLedgerPaid(_ ledger: SharedLedger) {
        var updated = ledger
        updated.isPaid   = true
        updated.paidDate = Date()
        db.collection("shared_ledgers").document(ledger.id).setData(updated.toDict())
    }

    // Split a bill: deducts your share and creates shared ledgers for each friend
    func splitBill(totalAmount: Double, myShare: Double, note: String, groupName: String?, friends: [(name: String, phone: String, share: Double)]) {
        // Log your own expense
        let defaultCat = categories.first ?? AppCategory.defaultCategories[0]
        let myTx = Transaction(
            amount: myShare, recipientName: "Split Bill", upiId: "",
            note: note, categoryId: defaultCat.id, categoryName: defaultCat.name,
            categoryEmoji: defaultCat.emoji, categoryHex: defaultCat.colorHex, date: Date()
        )
        addTransaction(myTx)

        // Create a shared ledger for each friend
        let myPhone = userProfile?.phone ?? ""
        let myName  = userName
        for friend in friends {
            let ledger = SharedLedger(
                id:        UUID().uuidString,
                fromUID:   uid,
                toUID:     "",  // Will be filled when friend links their account
                fromPhone: myPhone,
                toPhone:   friend.phone,
                fromName:  myName,
                toName:    friend.name,
                amount:    friend.share,
                note:      note,
                groupName: groupName,
                date:      Date(),
                isPaid:    false
            )
            db.collection("shared_ledgers").document(ledger.id).setData(ledger.toDict())
        }
    }

    // ── MARK: Categories ─────────────────────────────────────────────
    func addCategory(_ c: AppCategory) {
        userRef().collection("categories").document(c.id.uuidString).setData(c.toFirestoreDict())
    }

    func updateCategory(_ c: AppCategory) {
        userRef().collection("categories").document(c.id.uuidString).setData(c.toFirestoreDict())
    }

    func deleteCategory(_ c: AppCategory) {
        userRef().collection("categories").document(c.id.uuidString).delete()
    }

    // ── MARK: Contacts ───────────────────────────────────────────────
    func addPaymentContact(name: String, phone: String?) {
        savedContacts[name] = phone
        userRef().setData(["savedContacts": savedContacts.mapValues { $0 ?? "" }], merge: true)
    }

    func removePaymentContact(name: String) {
        savedContacts.removeValue(forKey: name)
        userRef().setData(["savedContacts": savedContacts.mapValues { $0 ?? "" }], merge: true)
    }

    // Stores custom contact avatars locally to prevent Firestore 1MB document blooming
    func saveAvatar(name: String, data: Data) {
        savedAvatars[name] = data
        if let dav = try? JSONEncoder().encode(savedAvatars) {
            UserDefaults.standard.set(dav, forKey: "savedAvatars_v4")
        }
    }

    // ── MARK: Budget ─────────────────────────────────────────────────
    func setBudget(_ v: Double) { saveSetting(budget: v) }

    var budgetUsedFraction: Double {
        monthlyBudget > 0 ? min(thisMonthTotal / monthlyBudget, 1.0) : 0
    }

    // ── MARK: Clear All Data ──────────────────────────────────────────
    func clearAll() {
        let batch = db.batch()
        for t in transactions {
            batch.deleteDocument(userRef().collection("transactions").document(t.id.uuidString))
        }
        for lb in lendBorrows {
            batch.deleteDocument(userRef().collection("lendborrows").document(lb.id.uuidString))
        }
        Task {
            do {
                try await batch.commit()
            } catch {
                print("Failed to clear data: \(error)")
            }
        }
        transactions = []
        lendBorrows  = []
        categories   = AppCategory.defaultCategories
    }

    // ── MARK: Sign Out ────────────────────────────────────────────────
    func signOut() {
        stopListening()
        try? FirebaseAuth.Auth.auth().signOut()
        transactions  = []
        lendBorrows   = []
        categories    = AppCategory.defaultCategories
        sharedLedgers = []
        userProfile   = nil
        isLoading     = true
    }


    // ── MARK: Person Groups (Payments tab) ───────────────────────────
    struct PersonGroup: Identifiable {
        let id           = UUID()
        let name         : String
        let phone        : String?
        let entries      : [LendBorrow]
        let sharedItems  : [SharedLedger]
        var totalLent    : Double
        var totalBorrowed: Double
    }

    var personGroups: [PersonGroup] {
        var dict = [String: ([LendBorrow], [SharedLedger], String?)]()

        for lb in lendBorrows {
            var (lbs, sls, ph) = dict[lb.personName] ?? ([], [], nil)
            lbs.append(lb)
            if let cp = lb.contactPhone { ph = cp }
            dict[lb.personName] = (lbs, sls, ph)
        }
        for sl in sharedLedgers {
            let counter = (sl.fromPhone == userProfile?.phone) ? sl.toName : sl.fromName
            var (lbs, sls, ph) = dict[counter] ?? ([], [], nil)
            sls.append(sl)
            dict[counter] = (lbs, sls, ph)
        }
        for (name, phone) in savedContacts {
            if dict[name] == nil { dict[name] = ([], [], phone) }
        }

        return dict.map { (name, val) in
            let (lbs, sls, phone) = val
            let lent     = lbs.filter { $0.type == .lent     && !$0.isPaid }.reduce(0) { $0 + $1.remainingAmount }
            let borrowed = lbs.filter { $0.type == .borrowed && !$0.isPaid }.reduce(0) { $0 + $1.remainingAmount }
            return PersonGroup(name: name, phone: phone, entries: lbs, sharedItems: sls, totalLent: lent, totalBorrowed: borrowed)
        }.sorted { $0.name < $1.name }
    }

    // ── MARK: Lookup friend by phone ─────────────────────────────────
    func findUser(byPhone phone: String) async -> UserProfile? {
        let full = phone.hasPrefix("+91") ? phone : "+91\(phone)"
        do {
            let snap = try await db.collection("users")
                .whereField("phone", isEqualTo: full)
                .limit(to: 1)
                .getDocuments()
            guard let doc = snap.documents.first else { return nil }
            return UserProfile.from(doc.data(), uid: doc.documentID)
        } catch {
            return nil
        }
    }

    // Analytics helpers
    var dailySpends: [DailySpend] {
        let cal = Calendar.current
        let today = Date()
        return (0..<7).compactMap { offset -> DailySpend? in
            guard let d = cal.date(byAdding: .day, value: -offset, to: today) else { return nil }
            let total = transactions
                .filter { cal.isDate($0.date, inSameDayAs: d) }
                .reduce(0) { $0 + $1.amount }
            let label = offset == 0 ? "Today" : (cal.isDateInYesterday(d) ? "Yesterday" :
                d.formatted(.dateTime.weekday(.abbreviated)))
            return DailySpend(date: d, label: label, amount: total)
        }.reversed()
    }

    var categorySpends: [CategorySpend] {
        let start = Calendar.current.date(from: Calendar.current.dateComponents([.year,.month], from: Date()))!
        let monthTx = transactions.filter { $0.date >= start }
        let total = monthTx.reduce(0) { $0 + $1.amount }
        var dict = [UUID: Double]()
        for t in monthTx { dict[t.categoryId, default: 0] += t.amount }
        return dict.compactMap { (catId, amt) -> CategorySpend? in
            guard let cat = categories.first(where: { $0.id == catId }) else { return nil }
            return CategorySpend(category: cat, amount: amt, percentage: total > 0 ? amt/total : 0)
        }.sorted { $0.amount > $1.amount }
    }

    // Full period-filtered version matching the old DataStore API used by DashboardView & AnalyticsView
    func categoryData(for period: AnalyticsPeriod = .monthly) -> [CategorySpend] {
        let cal = Calendar.current
        let filtered: [Transaction]
        switch period {
        case .daily:
            filtered = transactions.filter { cal.isDateInToday($0.date) }
        case .weekly:
            let s = cal.date(from: cal.dateComponents([.yearForWeekOfYear,.weekOfYear], from: Date()))!
            filtered = transactions.filter { $0.date >= s }
        case .monthly:
            let s = cal.date(from: cal.dateComponents([.year,.month], from: Date()))!
            filtered = transactions.filter { $0.date >= s }
        }
        let grouped = Dictionary(grouping: filtered, by: { $0.categoryId })
        let total = filtered.reduce(0) { $0 + $1.amount }
        return grouped.compactMap { (catId, items) -> CategorySpend? in
            let sum = items.reduce(0) { $0 + $1.amount }
            let appCat = categories.first(where: { $0.id == catId }) ?? items.first!.fallbackCategory
            return CategorySpend(category: appCat, amount: sum, percentage: total > 0 ? sum / total : 0)
        }.sorted { $0.amount > $1.amount }
    }

    func dailyData(days n: Int = 14) -> [DailySpend] {
        let cal = Calendar.current
        let fmt = DateFormatter(); fmt.dateFormat = "d MMM"
        return (0..<n).reversed().map { offset in
            let d = cal.date(byAdding: .day, value: -offset, to: Date())!
            let s = cal.startOfDay(for: d), e = cal.date(byAdding: .day, value: 1, to: s)!
            let total = transactions.filter { $0.date >= s && $0.date < e }.reduce(0) { $0+$1.amount }
            return DailySpend(date: d, label: fmt.string(from: d), amount: total)
        }
    }

    func weeklyData(weeks n: Int = 8) -> [WeeklySpend] {
        let cal = Calendar.current
        let fmt = DateFormatter(); fmt.dateFormat = "d MMM"
        return (0..<n).reversed().map { offset in
            let ref = cal.date(byAdding: .weekOfYear, value: -offset, to: Date())!
            let s = cal.date(from: cal.dateComponents([.yearForWeekOfYear,.weekOfYear], from: ref))!
            let e = cal.date(byAdding: .weekOfYear, value: 1, to: s)!
            let total = transactions.filter { $0.date >= s && $0.date < e }.reduce(0) { $0+$1.amount }
            return WeeklySpend(label: offset == 0 ? "This wk" : fmt.string(from: s), amount: total)
        }
    }

    func monthlyData(months n: Int = 6) -> [MonthlySpend] {
        let cal = Calendar.current
        let fmt = DateFormatter(); fmt.dateFormat = "MMM"
        return (0..<n).reversed().map { offset in
            let ref = cal.date(byAdding: .month, value: -offset, to: Date())!
            let s = cal.date(from: cal.dateComponents([.year,.month], from: ref))!
            let e = cal.date(byAdding: .month, value: 1, to: s)!
            let total = transactions.filter { $0.date >= s && $0.date < e }.reduce(0) { $0+$1.amount }
            return MonthlySpend(label: fmt.string(from: ref), amount: total)
        }
    }
}

// MARK: - UI Extensions for PersonGroup
extension CloudDataStore.PersonGroup {
    // Required by Identifiable since it's used in ForEach
    var uniqueId: String { name }

    var lentEntries     : [LendBorrow] { entries.filter { $0.type == .lent } }
    var borrowedEntries : [LendBorrow] { entries.filter { $0.type == .borrowed } }
    var totalLentOut    : Double { totalLent }

    var initial         : String { String(name.prefix(1)).uppercased() }
    var color           : Color {
        let palette: [Color] = [.catPurple, .accentBlue, .catOrange, .incomeGreen, .catPink, .catYellow]
        return palette[abs(name.hashValue) % palette.count]
    }
}
