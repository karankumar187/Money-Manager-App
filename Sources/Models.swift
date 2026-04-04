import Foundation
import SwiftUI

// MARK: - Enums
// AppCategory has been removed in favor of AppCategory
enum UPIApp: String, Codable, CaseIterable, Identifiable {
    case gpay    = "Google Pay"
    case phonepe = "PhonePe"
    case kotak   = "kotak bank: 811 mobile app"
    case slice   = "Slice"

    var id: String { rawValue }

    var scheme: String {
        switch self {
        case .gpay:    return "tez"
        case .phonepe: return "phonepe"
        case .kotak:   return "kotak811"
        case .slice:   return "slicepay"
        }
    }

    var logoName: String {
        switch self {
        case .gpay:    return "gpay_logo"
        case .phonepe: return "phonepe_logo"
        case .kotak:   return "kotak_logo"
        case .slice:   return "slice_logo"
        }
    }

    var color: Color {
        switch self {
        case .gpay:    return Color(red: 0.11, green: 0.64, blue: 0.33)   // Google green
        case .phonepe: return Color(red: 0.37, green: 0.15, blue: 0.63)   // PhonePe purple
        case .kotak:   return Color(red: 0.93, green: 0.11, blue: 0.14)   // Kotak red
        case .slice:   return Color(red: 1.00, green: 0.18, blue: 0.47)   // Slice pink
        }
    }

    func makeURL(upiId: String, name: String, amount: Double) -> URL? {
        let basePath: String
        switch self {
        case .gpay:    basePath = "gpay://upi/pay"
        case .phonepe: basePath = "phonepe://pay"
        case .kotak:   basePath = "kotak811://upi/pay"
        case .slice:   basePath = "slice://pay"
        }
        
        var components = URLComponents(string: basePath)
        let trRef = "MM\(Int(Date().timeIntervalSince1970))"
        let safeName = name.isEmpty ? "Merchant" : name
        
        components?.queryItems = [
            URLQueryItem(name: "pa", value: upiId),
            URLQueryItem(name: "pn", value: safeName),
            URLQueryItem(name: "am", value: String(format: "%.2f", amount)),
            URLQueryItem(name: "cu", value: "INR"),
            URLQueryItem(name: "tr", value: trRef),
            URLQueryItem(name: "mc", value: "0000")
        ]
        return components?.url
    }
}

enum LendBorrowType: String, Codable, CaseIterable {
    case lent     = "I Lent"
    case borrowed = "I Borrowed"
}

enum AnalyticsPeriod: String, CaseIterable {
    case daily   = "Daily"
    case weekly  = "Weekly"
    case monthly = "Monthly"
}

// MARK: - Custom User Category

struct AppCategory: Identifiable, Codable, Equatable, Hashable {
    var id    = UUID()
    var name  : String
    var emoji : String
    var colorHex: String  // e.g. "#FF6B6B"

    var color: Color {
        var hex = colorHex.trimmingCharacters(in: .whitespacesAndNewlines)
        if hex.hasPrefix("#") { hex.removeFirst() }
        var rgb: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&rgb)
        return Color(red: Double((rgb>>16)&0xFF)/255,
                     green: Double((rgb>>8)&0xFF)/255,
                     blue: Double(rgb&0xFF)/255)
    }

    static let defaultCategories: [AppCategory] = [
        AppCategory(name: "Food", emoji: "🍔", colorHex: "#FF9F0A"),
        AppCategory(name: "Transport", emoji: "🚌", colorHex: "#5AC8FA"),
        AppCategory(name: "Shopping", emoji: "🛍️", colorHex: "#7B61FF"),
        AppCategory(name: "Bills", emoji: "🧾", colorHex: "#FF375F"),
        AppCategory(name: "Subscriptions", emoji: "📱", colorHex: "#30D158"),
        AppCategory(name: "Rent", emoji: "🏠", colorHex: "#FF453A"),
        AppCategory(name: "Other", emoji: "📦", colorHex: "#FFD60A")
    ]
}

// MARK: - Models

struct Transaction: Identifiable, Codable {
    var id            = UUID()
    var amount        : Double
    var recipientName : String
    var upiId         : String
    var note          : String
    var categoryId    : UUID
    var categoryName  : String
    var categoryEmoji : String
    var categoryHex   : String
    var date          : Date
    var upiAppUsed    : UPIApp?
    var linkedLendId  : UUID? = nil

    var fallbackCategory: AppCategory {
        AppCategory(id: categoryId, name: categoryName, emoji: categoryEmoji, colorHex: categoryHex)
    }
}

struct LendBorrow: Identifiable, Codable {
    var id           = UUID()
    var type         : LendBorrowType
    var personName   : String
    var contactPhone : String? = nil
    var amount       : Double
    var paidAmount   : Double = 0
    var note         : String
    var date         : Date
    var dueDate      : Date?
    var isPaid       : Bool = false
    var paidDate     : Date? = nil

    // Computed
    var remainingAmount: Double  { isPaid ? 0 : max(0, amount - paidAmount) }
    var paidFraction: Double     { amount > 0 ? min(paidAmount / amount, 1.0) : 0 }
    var hasPartialPayment: Bool  { paidAmount > 0 && !isPaid }

    var isDueSoon: Bool {
        guard let d = dueDate, !isPaid else { return false }
        return d.timeIntervalSinceNow < 3*86400 && d.timeIntervalSinceNow > 0
    }
    var isOverdue: Bool {
        guard let d = dueDate, !isPaid else { return false }
        return d.timeIntervalSinceNow < 0
    }
}

struct UPIQRData {
    var upiId  : String
    var name   : String
    var amount : Double?
}

func parseUPIQR(_ raw: String) -> UPIQRData? {
    guard let components = URLComponents(string: raw) else { return nil }
    
    // Safely parse query items to avoid duplicate key crash
    var params = [String: String]()
    for item in components.queryItems ?? [] {
        if let val = item.value { params[item.name] = val }
    }
    
    guard let pa = params["pa"], !pa.isEmpty else { return nil }
    var payeeName = params["pn"] ?? ""
    
    // Prevent generic names from overwriting actual store names
    let lowerName = payeeName.lowercased().trimmingCharacters(in: .whitespaces)
    let genericNames = ["phonepe merchant", "bharatpe merchant", "gpay merchant", "google pay merchant", "paytm merchant", "amazon pay merchant", "merchant", "bhim merchant"]
    
    if genericNames.contains(lowerName) || payeeName.isEmpty {
        // Fallback to capitalizing the UPI ID username
        // e.g. "sharma.sweets@ybl" -> "Sharma Sweets"
        let username = pa.components(separatedBy: "@").first ?? pa
        payeeName = username
            .replacingOccurrences(of: ".", with: " ")
            .replacingOccurrences(of: "_", with: " ")
            .capitalized
    }
    
    return UPIQRData(upiId: pa, name: payeeName, amount: params["am"].flatMap(Double.init))
}

// MARK: - Analytics Data Structs

struct DailySpend: Identifiable {
    var id = UUID(); var date: Date; var label: String; var amount: Double
}
struct WeeklySpend: Identifiable {
    var id = UUID(); var label: String; var amount: Double
}
struct MonthlySpend: Identifiable {
    var id = UUID(); var label: String; var amount: Double
}
struct CategorySpend: Identifiable {
    var id = UUID(); var category: AppCategory; var amount: Double; var percentage: Double
}

// MARK: - DataStore

class DataStore: ObservableObject {
    @Published var transactions    : [Transaction]  = []
    @Published var lendBorrows     : [LendBorrow]   = []
    @Published var savedUPIs       : [String: String] = [:]
    @Published var savedAvatars    : [String: Data] = [:]
    // Quick-pay contacts: name -> optional phone (shown in Payments view)
    @Published var savedContacts   : [String: String?] = [:]
    @Published var categories      : [AppCategory]  = AppCategory.defaultCategories
    @Published var monthlyBudget   : Double = 30000
    @Published var userName        : String = "User"
    @Published var currencySymbol  : String = "₹"
    @Published var userProfileImageData: Data? = nil

    private let txKey         = "mm_transactions_v4"
    private let lbKey         = "mm_lendborrow_v4"
    private let upiKey        = "mm_saved_upis_v1"
    private let catKey        = "mm_usercategories_v1"
    private let budgetKey     = "mm_budget"
    private let nameKey       = "mm_username"
    private let currencyKey   = "mm_currency"
    private let avatarKey     = "mm_profile_image"
    private let avatarsKey    = "savedAvatars_v4"
    private let contactsKey   = "mm_savedContacts_v1"

    init() {
        load()
        let b = UserDefaults.standard.double(forKey: budgetKey)
        monthlyBudget  = b > 0 ? b : 30000
        userName       = UserDefaults.standard.string(forKey: nameKey) ?? "User"
        currencySymbol = UserDefaults.standard.string(forKey: currencyKey) ?? "₹"
        userProfileImageData = UserDefaults.standard.data(forKey: avatarKey)
        let savedCats = UserDefaults.standard.data(forKey: catKey)
        if let d = savedCats, let v = try? JSONDecoder().decode([AppCategory].self, from: d), !v.isEmpty {
            categories = v
        } else {
            categories = AppCategory.defaultCategories
        }
        if transactions.isEmpty && lendBorrows.isEmpty { seedSampleData() }
    }

    func formatted(_ v: Double) -> String { fmt(v, symbol: currencySymbol) }

    // MARK: Stats
    var todayTotal: Double {
        let cal = Calendar.current
        return transactions.filter { cal.isDateInToday($0.date) }.reduce(0) { $0 + $1.amount }
    }
    var thisWeekTotal: Double {
        let cal = Calendar.current
        let start = cal.date(from: cal.dateComponents([.yearForWeekOfYear,.weekOfYear], from: Date()))!
        return transactions.filter { $0.date >= start }.reduce(0) { $0 + $1.amount }
    }
    var thisMonthTotal: Double {
        let cal = Calendar.current
        let start = cal.date(from: cal.dateComponents([.year,.month], from: Date()))!
        return transactions.filter { $0.date >= start }.reduce(0) { $0 + $1.amount }
    }
    var budgetUsedFraction: Double { min(thisMonthTotal / max(monthlyBudget, 1), 1.0) }
    var averageDailySpend: Double {
        guard !transactions.isEmpty, let oldest = transactions.map({ $0.date }).min() else { return 0 }
        let days = max(1, Calendar.current.dateComponents([.day], from: oldest, to: Date()).day ?? 1)
        return transactions.reduce(0) { $0+$1.amount } / Double(days)
    }
    var totalLent: Double     { lendBorrows.filter { $0.type == .lent     && !$0.isPaid }.reduce(0) { $0+$1.amount } }
    var totalBorrowed: Double { lendBorrows.filter { $0.type == .borrowed && !$0.isPaid }.reduce(0) { $0+$1.amount } }

    // MARK: Analytics
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
        return grouped.map { (catId, items) -> CategorySpend in
            let sum = items.reduce(0) { $0 + $1.amount }
            let appCat = categories.first(where: { $0.id == catId }) ?? items.first!.fallbackCategory
            return CategorySpend(category: appCat, amount: sum, percentage: total > 0 ? sum / total : 0)
        }.sorted { $0.amount > $1.amount }
    }

    // MARK: CRUD – Transactions
    func addTransaction(_ t: Transaction) { transactions.append(t); save() }
    func deleteTransaction(id: UUID) {
        if let tx = transactions.first(where: { $0.id == id }), let lendId = tx.linkedLendId {
            lendBorrows.removeAll { $0.id == lendId }
        }
        transactions.removeAll { $0.id == id }
        save()
    }

    // MARK: CRUD – UPIs
    func saveUPI(name: String, upi: String) {
        if !name.trimmingCharacters(in: .whitespaces).isEmpty && !upi.trimmingCharacters(in: .whitespaces).isEmpty {
            savedUPIs[name] = upi
            save()
        }
    }

    // MARK: CRUD – Payment Contacts
    func addPaymentContact(name: String, phone: String?) {
        let n = name.trimmingCharacters(in: .whitespaces)
        guard !n.isEmpty else { return }
        savedContacts[n] = phone
        save()
    }
    func removePaymentContact(name: String) { savedContacts.removeValue(forKey: name); save() }

    // MARK: CRUD – Lend/Borrow
    func addLendBorrow(_ lb: LendBorrow) { lendBorrows.append(lb); save() }
    func updateLendBorrow(_ lb: LendBorrow) {
        if let i = lendBorrows.firstIndex(where: { $0.id == lb.id }) { lendBorrows[i] = lb; save() }
    }
    func deleteLendBorrow(id: UUID) { lendBorrows.removeAll { $0.id == id }; save() }
    func markPaid(id: UUID) {
        if let i = lendBorrows.firstIndex(where: { $0.id == id }) {
            lendBorrows[i].isPaid = true; lendBorrows[i].paidDate = Date()
            lendBorrows[i].paidAmount = lendBorrows[i].amount
            save()
        }
    }
    func addPartialPayment(id: UUID, paidNow: Double) {
        if let i = lendBorrows.firstIndex(where: { $0.id == id }) {
            lendBorrows[i].paidAmount = min(lendBorrows[i].amount,
                                            lendBorrows[i].paidAmount + paidNow)
            if lendBorrows[i].paidAmount >= lendBorrows[i].amount {
                lendBorrows[i].isPaid = true; lendBorrows[i].paidDate = Date()
            }
            save()
        }
    }

    // MARK: CRUD – Custom Categories
    func addAppCategory(_ c: AppCategory) { categories.append(c); save() }
    func updateAppCategory(_ c: AppCategory) {
        if let idx = categories.firstIndex(where: { $0.id == c.id }) {
            categories[idx] = c
            save()
        }
    }
    func deleteAppCategory(id: UUID) { categories.removeAll { $0.id == id }; save() }

    // MARK: Settings
    func setProfileImage(_ data: Data?) { userProfileImageData = data; UserDefaults.standard.set(data, forKey: avatarKey) }
    func saveAvatar(name: String, data: Data) { savedAvatars[name] = data; save() }
    func setBudget(_ v: Double)   { monthlyBudget = v;   UserDefaults.standard.set(v, forKey: budgetKey) }
    func setName(_ v: String)     { userName = v;        UserDefaults.standard.set(v, forKey: nameKey) }
    func setCurrency(_ v: String) { currencySymbol = v;  UserDefaults.standard.set(v, forKey: currencyKey) }
    func clearAll() { transactions = []; lendBorrows = []; categories = AppCategory.defaultCategories; save() }

    // MARK: Persistence
    private func save() {
        if let dtx  = try? JSONEncoder().encode(transactions)    { UserDefaults.standard.set(dtx,  forKey: txKey)       }
        if let dlb  = try? JSONEncoder().encode(lendBorrows)     { UserDefaults.standard.set(dlb,  forKey: lbKey)       }
        if let dcat = try? JSONEncoder().encode(categories)      { UserDefaults.standard.set(dcat, forKey: catKey)      }
        if let dupi = try? JSONEncoder().encode(savedUPIs)       { UserDefaults.standard.set(dupi, forKey: upiKey)      }
        if let dav  = try? JSONEncoder().encode(savedAvatars)    { UserDefaults.standard.set(dav,  forKey: avatarsKey)  }
        if let dcon = try? JSONEncoder().encode(savedContacts)   { UserDefaults.standard.set(dcon, forKey: contactsKey) }
        UserDefaults.standard.set(monthlyBudget, forKey: budgetKey)
        UserDefaults.standard.set(userName, forKey: nameKey)
        UserDefaults.standard.set(currencySymbol, forKey: currencyKey)
        UserDefaults.standard.set(userProfileImageData, forKey: avatarKey)
    }
    private func load() {
        if let d = UserDefaults.standard.data(forKey: txKey),       let v = try? JSONDecoder().decode([Transaction].self,     from: d) { transactions    = v }
        if let d = UserDefaults.standard.data(forKey: lbKey),       let v = try? JSONDecoder().decode([LendBorrow].self,      from: d) { lendBorrows     = v }
        if let d = UserDefaults.standard.data(forKey: catKey),      let v = try? JSONDecoder().decode([AppCategory].self,     from: d) { categories      = v }
        if let d = UserDefaults.standard.data(forKey: upiKey),      let v = try? JSONDecoder().decode([String: String].self,  from: d) { savedUPIs       = v }
        if let d = UserDefaults.standard.data(forKey: avatarsKey),  let v = try? JSONDecoder().decode([String: Data].self,   from: d) { savedAvatars    = v }
        if let d = UserDefaults.standard.data(forKey: contactsKey), let v = try? JSONDecoder().decode([String: String?].self, from: d) { savedContacts   = v }
    }

    // MARK: Sample Data
    private func seedSampleData() {
        transactions = []
        lendBorrows = []
        save()
    }
}
