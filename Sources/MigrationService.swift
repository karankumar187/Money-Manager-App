import Foundation
import FirebaseFirestore
import FirebaseAuth

// MARK: - MigrationService
// On first cloud login, reads all local UserDefaults/JSON data
// and uploads it to Firestore. Never deletes local data.

@MainActor
class MigrationService {

    private let db = Firestore.firestore()
    private let migrationKey = "mm_cloud_migrated_v1"

    func migrateIfNeeded(uid: String) async {
        guard !UserDefaults.standard.bool(forKey: migrationKey) else {
            print("[Migration] Already migrated. Skipping.")
            return
        }
        print("[Migration] First cloud login — migrating local data to Firestore...")

        await migrateTransactions(uid: uid)
        await migrateLendBorrows(uid: uid)
        await migrateCategories(uid: uid)
        await migrateSettings(uid: uid)

        UserDefaults.standard.set(true, forKey: migrationKey)
        print("[Migration] ✅ Migration complete.")
    }

    private func migrateTransactions(uid: String) async {
        let txKey = "mm_transactions_v4"
        guard let data = UserDefaults.standard.data(forKey: txKey),
              let txs  = try? JSONDecoder().decode([Transaction].self, from: data),
              !txs.isEmpty else { return }

        print("[Migration] Migrating \(txs.count) transactions...")
        let ref = db.collection("users").document(uid).collection("transactions")
        for tx in txs {
            try? await ref.document(tx.id.uuidString).setData(tx.toFirestoreDict())
        }
    }

    private func migrateLendBorrows(uid: String) async {
        let lbKey = "mm_lendborrow_v4"
        guard let data = UserDefaults.standard.data(forKey: lbKey),
              let lbs  = try? JSONDecoder().decode([LendBorrow].self, from: data),
              !lbs.isEmpty else { return }

        print("[Migration] Migrating \(lbs.count) lend/borrow records...")
        let ref = db.collection("users").document(uid).collection("lendborrows")
        for lb in lbs {
            try? await ref.document(lb.id.uuidString).setData(lb.toFirestoreDict())
        }
    }

    private func migrateCategories(uid: String) async {
        let catKey = "mm_usercategories_v1"
        guard let data = UserDefaults.standard.data(forKey: catKey),
              let cats = try? JSONDecoder().decode([AppCategory].self, from: data),
              !cats.isEmpty else { return }

        print("[Migration] Migrating \(cats.count) categories...")
        let ref = db.collection("users").document(uid).collection("categories")
        for cat in cats {
            try? await ref.document(cat.id.uuidString).setData(cat.toFirestoreDict())
        }
    }

    private func migrateSettings(uid: String) async {
        let budgetKey  = "mm_budget"
        let nameKey    = "mm_username"
        let currencyKey = "mm_currency"

        let budget   = UserDefaults.standard.double(forKey: budgetKey)
        let name     = UserDefaults.standard.string(forKey: nameKey) ?? "User"
        let currency = UserDefaults.standard.string(forKey: currencyKey) ?? "₹"

        let settings: [String: Any] = [
            "budget": budget > 0 ? budget : 30000,
            "userName": name,
            "currencySymbol": currency
        ]
        try? await db.collection("users").document(uid)
            .setData(settings, merge: true)
        print("[Migration] Migrated settings.")
    }
}
