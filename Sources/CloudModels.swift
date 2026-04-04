import Foundation
import FirebaseFirestore

// MARK: - UserProfile
// Stored in Firestore at /users/{uid}

struct UserProfile: Codable, Identifiable {
    var id: String           // Firebase UID
    var phone: String        // e.g. "+919876543210"
    var displayName: String
    var upiId: String        // e.g. "karan@ybl" — searchable by friends
    var profileImageURL: String?  // Firebase Storage URL
    var createdAt: Date

    // Firestore document dictionary (manual coding for Timestamp)
    func toDict() -> [String: Any] {
        var d: [String: Any] = [
            "id":          id,
            "phone":       phone,
            "displayName": displayName,
            "upiId":       upiId,
            "createdAt":   Timestamp(date: createdAt)
        ]
        if let url = profileImageURL { d["profileImageURL"] = url }
        return d
    }

    static func from(_ dict: [String: Any], uid: String) -> UserProfile? {
        guard let phone = dict["phone"] as? String,
              let name  = dict["displayName"] as? String else { return nil }
        let upi = dict["upiId"] as? String ?? ""
        let imageURL = dict["profileImageURL"] as? String
        let ts = dict["createdAt"] as? Timestamp
        return UserProfile(
            id: uid, phone: phone, displayName: name,
            upiId: upi, profileImageURL: imageURL,
            createdAt: ts?.dateValue() ?? Date()
        )
    }
}

// MARK: - SharedLedger (Splitwise style)
// A shared record between exactly two users that is mutually visible.
// Stored in Firestore at /shared_ledgers/{ledgerId}

struct SharedLedger: Identifiable, Codable {
    var id: String               // UUID string
    var fromUID: String          // person who paid / lent
    var toUID: String            // person who owes / borrowed
    var fromPhone: String
    var toPhone: String
    var fromName: String
    var toName: String
    var amount: Double
    var note: String
    var groupName: String?       // e.g. "Goa Trip" – optional
    var date: Date
    var isPaid: Bool
    var paidDate: Date?

    func toDict() -> [String: Any] {
        var d: [String: Any] = [
            "id":        id,
            "fromUID":   fromUID,
            "toUID":     toUID,
            "fromPhone": fromPhone,
            "toPhone":   toPhone,
            "fromName":  fromName,
            "toName":    toName,
            "amount":    amount,
            "note":      note,
            "date":      Timestamp(date: date),
            "isPaid":    isPaid
        ]
        if let g = groupName { d["groupName"] = g }
        if let p = paidDate  { d["paidDate"]  = Timestamp(date: p) }
        return d
    }

    static func from(_ dict: [String: Any]) -> SharedLedger? {
        guard let id      = dict["id"]        as? String,
              let fromUID = dict["fromUID"]    as? String,
              let toUID   = dict["toUID"]      as? String,
              let amount  = dict["amount"]     as? Double,
              let note    = dict["note"]       as? String,
              let ts      = dict["date"]       as? Timestamp else { return nil }
        return SharedLedger(
            id:        id,
            fromUID:   fromUID,
            toUID:     toUID,
            fromPhone: dict["fromPhone"]   as? String ?? "",
            toPhone:   dict["toPhone"]     as? String ?? "",
            fromName:  dict["fromName"]    as? String ?? "",
            toName:    dict["toName"]      as? String ?? "",
            amount:    amount,
            note:      note,
            groupName: dict["groupName"]   as? String,
            date:      ts.dateValue(),
            isPaid:    dict["isPaid"]      as? Bool ?? false,
            paidDate:  (dict["paidDate"]   as? Timestamp)?.dateValue()
        )
    }
}

// MARK: - Firestore Codable Helpers for existing models

extension Transaction {
    func toFirestoreDict() -> [String: Any] {
        var d: [String: Any] = [
            "id":            id.uuidString,
            "amount":        amount,
            "recipientName": recipientName,
            "upiId":         upiId,
            "note":          note,
            "categoryId":    categoryId.uuidString,
            "categoryName":  categoryName,
            "categoryEmoji": categoryEmoji,
            "categoryHex":   categoryHex,
            "date":          Timestamp(date: date)
        ]
        if let app = upiAppUsed    { d["upiAppUsed"]   = app.rawValue }
        if let lid = linkedLendId  { d["linkedLendId"] = lid.uuidString }
        return d
    }

    static func fromFirestore(_ dict: [String: Any]) -> Transaction? {
        guard let idStr  = dict["id"]     as? String, let id = UUID(uuidString: idStr),
              let amount = dict["amount"] as? Double,
              let note   = dict["note"]   as? String,
              let ts     = dict["date"]   as? Timestamp,
              let catStr = dict["categoryId"] as? String, let catId = UUID(uuidString: catStr)
        else { return nil }

        let appRaw = dict["upiAppUsed"]   as? String
        let lendRaw = dict["linkedLendId"] as? String

        return Transaction(
            id:            id,
            amount:        amount,
            recipientName: dict["recipientName"] as? String ?? "",
            upiId:         dict["upiId"]         as? String ?? "",
            note:          note,
            categoryId:    catId,
            categoryName:  dict["categoryName"]  as? String ?? "",
            categoryEmoji: dict["categoryEmoji"] as? String ?? "💸",
            categoryHex:   dict["categoryHex"]   as? String ?? "#FFFFFF",
            date:          ts.dateValue(),
            upiAppUsed:    appRaw.flatMap { UPIApp(rawValue: $0) },
            linkedLendId:  lendRaw.flatMap { UUID(uuidString: $0) }
        )
    }
}

extension LendBorrow {
    func toFirestoreDict() -> [String: Any] {
        var d: [String: Any] = [
            "id":          id.uuidString,
            "type":        type.rawValue,
            "personName":  personName,
            "amount":      amount,
            "paidAmount":  paidAmount,
            "note":        note,
            "date":        Timestamp(date: date),
            "isPaid":      isPaid
        ]
        if let p  = contactPhone { d["contactPhone"] = p }
        if let dd = dueDate      { d["dueDate"]      = Timestamp(date: dd) }
        if let pd = paidDate     { d["paidDate"]     = Timestamp(date: pd) }
        return d
    }

    static func fromFirestore(_ dict: [String: Any]) -> LendBorrow? {
        guard let idStr  = dict["id"]     as? String, let id = UUID(uuidString: idStr),
              let tRaw   = dict["type"]   as? String, let type = LendBorrowType(rawValue: tRaw),
              let name   = dict["personName"] as? String,
              let amount = dict["amount"] as? Double,
              let note   = dict["note"]   as? String,
              let ts     = dict["date"]   as? Timestamp
        else { return nil }

        return LendBorrow(
            id:           id,
            type:         type,
            personName:   name,
            contactPhone: dict["contactPhone"] as? String,
            amount:       amount,
            paidAmount:   dict["paidAmount"]   as? Double ?? 0,
            note:         note,
            date:         ts.dateValue(),
            dueDate:      (dict["dueDate"]  as? Timestamp)?.dateValue(),
            isPaid:       dict["isPaid"]    as? Bool ?? false,
            paidDate:     (dict["paidDate"] as? Timestamp)?.dateValue()
        )
    }
}

extension AppCategory {
    func toFirestoreDict() -> [String: Any] {
        ["id": id.uuidString, "name": name, "emoji": emoji, "colorHex": colorHex]
    }

    static func fromFirestore(_ dict: [String: Any]) -> AppCategory? {
        guard let name  = dict["name"]     as? String,
              let emoji = dict["emoji"]    as? String,
              let hex   = dict["colorHex"] as? String else { return nil }
        let idStr = dict["id"] as? String ?? UUID().uuidString
        return AppCategory(id: UUID(uuidString: idStr) ?? UUID(), name: name, emoji: emoji, colorHex: hex)
    }
}
