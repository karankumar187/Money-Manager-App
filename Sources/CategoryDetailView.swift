import SwiftUI

// MARK: - Category Detail Sheet

struct CategoryDetailView: View {
    let category: AppCategory
    @EnvironmentObject var store: DataStore
    @Environment(\.dismiss) var dismiss

    // All transactions for this category this month
    var transactions: [Transaction] {
        let cal = Calendar.current
        let start = cal.date(from: cal.dateComponents([.year, .month], from: Date()))!
        return store.transactions
            .filter { $0.categoryId == category.id && $0.date >= start }
            .sorted { $0.date > $1.date }
    }

    var totalAmount: Double { transactions.reduce(0) { $0 + $1.amount } }

    // Grouped by day
    struct DayGroup: Identifiable {
        let id       = UUID()
        let label    : String
        let sortDate : Date
        let items    : [Transaction]
    }

    var groups: [DayGroup] {
        let cal = Calendar.current
        var dict: [String: (Date, [Transaction])] = [:]
        for t in transactions {
            let label: String; let anchor: Date
            if cal.isDateInToday(t.date)      { label = "Today";     anchor = Date() }
            else if cal.isDateInYesterday(t.date) { label = "Yesterday"; anchor = cal.date(byAdding: .day, value: -1, to: Date())! }
            else {
                let f = DateFormatter(); f.dateStyle = .medium
                label = f.string(from: t.date); anchor = cal.startOfDay(for: t.date)
            }
            var e = dict[label] ?? (anchor, [])
            e.1.append(t); dict[label] = e
        }
        return dict.map { DayGroup(label: $0.key, sortDate: $0.value.0, items: $0.value.1) }
                   .sorted { $0.sortDate > $1.sortDate }
    }

    // Month name for header
    var monthName: String {
        let f = DateFormatter(); f.dateFormat = "MMMM yyyy"
        return f.string(from: Date())
    }

    var body: some View {
        NavigationView {
            ZStack {
                Color.bgPrimary.ignoresSafeArea()

                ScrollView(showsIndicators: false) {
                    VStack(spacing: 0) {

                        // ── Hero Card ────────────────────────────────
                        ZStack {
                            RoundedRectangle(cornerRadius: 28, style: .continuous)
                                .fill(category.color.opacity(0.18))
                                .overlay(
                                    RoundedRectangle(cornerRadius: 28, style: .continuous)
                                        .stroke(category.color.opacity(0.3), lineWidth: 1)
                                )

                            // Decorative circles
                            Circle().fill(category.color.opacity(0.08)).frame(width: 160).offset(x: 90, y: -40)
                            Circle().fill(category.color.opacity(0.05)).frame(width: 100).offset(x: -60, y: 50)

                            VStack(spacing: 10) {
                                Text(category.emoji).font(.system(size: 52))
                                Text(category.name)
                                    .font(.system(size: 22, weight: .bold)).foregroundColor(.white)
                                Text(monthName)
                                    .font(.system(size: 13)).foregroundColor(.white.opacity(0.55))

                                Divider().background(Color.white.opacity(0.1)).padding(.horizontal, 32)

                                HStack(spacing: 16) {
                                    VStack(spacing: 4) {
                                        Text(store.formatted(totalAmount))
                                            .font(.system(size: 24, weight: .bold, design: .rounded))
                                            .foregroundColor(category.color)
                                        Text("Total Spent")
                                            .font(.system(size: 11)).foregroundColor(.white.opacity(0.5))
                                    }
                                    .lineLimit(1).minimumScaleFactor(0.4)
                                    .frame(maxWidth: .infinity)
                                    
                                    Rectangle().fill(Color.white.opacity(0.1)).frame(width: 1, height: 36)
                                    
                                    VStack(spacing: 4) {
                                        Text("\(transactions.count)")
                                            .font(.system(size: 24, weight: .bold, design: .rounded))
                                            .foregroundColor(.white)
                                        Text("Transactions")
                                            .font(.system(size: 11)).foregroundColor(.white.opacity(0.5))
                                    }
                                    .lineLimit(1).minimumScaleFactor(0.4)
                                    .frame(maxWidth: .infinity)
                                    
                                    if transactions.count > 0 {
                                        Rectangle().fill(Color.white.opacity(0.1)).frame(width: 1, height: 36)
                                        
                                        VStack(spacing: 4) {
                                            Text(store.formatted(totalAmount / Double(transactions.count)))
                                                .font(.system(size: 20, weight: .bold, design: .rounded))
                                                .foregroundColor(.white)
                                            Text("Avg / txn")
                                                .font(.system(size: 11)).foregroundColor(.white.opacity(0.5))
                                        }
                                        .lineLimit(1).minimumScaleFactor(0.4)
                                        .frame(maxWidth: .infinity)
                                    }
                                }
                                .padding(.horizontal, 10)
                            }
                            .padding(28)
                        }
                        .padding(.horizontal, 20)
                        .padding(.top, 12)
                        .padding(.bottom, 28)

                        // ── Transaction List ──────────────────────────
                        if groups.isEmpty {
                            VStack(spacing: 16) {
                                Image(systemName: "tray").font(.system(size: 44))
                                    .foregroundColor(.textSecondary.opacity(0.3))
                                Text("No \(category.name) expenses this month")
                                    .font(.system(size: 15)).foregroundColor(.textSecondary)
                                    .multilineTextAlignment(.center)
                            }
                            .frame(maxWidth: .infinity).padding(.top, 60)
                        } else {
                            VStack(alignment: .leading, spacing: 20) {
                                ForEach(groups) { group in
                                    VStack(alignment: .leading, spacing: 10) {
                                        Text(group.label)
                                            .font(.system(size: 12, weight: .semibold))
                                            .foregroundColor(.textSecondary)
                                            .padding(.horizontal, 24)

                                        ForEach(group.items) { t in
                                            HStack(spacing: 14) {
                                                // Left: icon
                                                ZStack {
                                                    RoundedRectangle(cornerRadius: 14, style: .continuous)
                                                        .fill(category.color.opacity(0.15))
                                                        .frame(width: 46, height: 46)
                                                    Text(category.emoji).font(.system(size: 20))
                                                }

                                                // Middle: name + note
                                                VStack(alignment: .leading, spacing: 3) {
                                                    Text(t.recipientName)
                                                        .font(.system(size: 15, weight: .semibold))
                                                        .foregroundColor(.white)
                                                    if !t.note.isEmpty && t.note != category.name {
                                                        Text(t.note)
                                                            .font(.system(size: 12))
                                                            .foregroundColor(.textSecondary)
                                                            .lineLimit(1)
                                                    }
                                                    if let app = t.upiAppUsed {
                                                        Text(app.rawValue.components(separatedBy: ":").first ?? app.rawValue)
                                                            .font(.system(size: 9, weight: .bold))
                                                            .foregroundColor(app.color)
                                                            .padding(.horizontal, 6).padding(.vertical, 2)
                                                            .background(Capsule().fill(app.color.opacity(0.15)))
                                                    }
                                                }

                                                Spacer()

                                                // Right: amount + time
                                                VStack(alignment: .trailing, spacing: 3) {
                                                    Text("−\(store.formatted(t.amount))")
                                                        .font(.system(size: 15, weight: .bold))
                                                        .foregroundColor(.expenseRed)
                                                    Text(t.date, style: .time)
                                                        .font(.system(size: 11))
                                                        .foregroundColor(.textSecondary)
                                                }
                                            }
                                            .padding(14)
                                            .glassCard(radius: 18)
                                            .padding(.horizontal, 20)
                                        }
                                    }
                                }
                            }
                            .padding(.bottom, 40)
                        }
                    }
                }
            }
            .navigationTitle(category.name)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Done") { dismiss() }
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundColor(.accent1)
                }
            }
        }
    }
}

#Preview {
    CategoryDetailView(category: AppCategory.defaultCategories[0])
        .environmentObject(DataStore())
        .preferredColorScheme(.dark)
}
