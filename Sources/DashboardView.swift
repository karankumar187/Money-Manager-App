import SwiftUI

struct DashboardView: View {
    @EnvironmentObject var store: CloudDataStore
    @State private var selectedCategory: AppCategory? = nil
    @State private var showSettings = false

    var body: some View {
        ScrollView(showsIndicators: false) {
            VStack(spacing: 0) {

                // ── Header ──────────────────────────────────────
                HStack {
                    VStack(alignment: .leading, spacing: 3) {
                        Text("Hello")
                            .font(.system(size: 14, weight: .medium)).foregroundColor(.textSecondary)
                        Text(store.userName)
                            .font(.system(size: 26, weight: .bold)).foregroundColor(.white)
                    }
                    Spacer()
                    Button {
                        showSettings = true
                    } label: {
                        if let data = store.userProfileImageData, let uiImage = UIImage(data: data) {
                            Image(uiImage: uiImage)
                                .resizable().scaledToFill()
                                .frame(width: 44, height: 44).clipShape(Circle())
                                .overlay(Circle().stroke(Color.white.opacity(0.1), lineWidth: 1))
                        } else {
                            ZStack {
                                Circle()
                                    .fill(Color.bgCardAlt)
                                    .frame(width: 44, height: 44)
                                    .overlay(Circle().stroke(Color.white.opacity(0.06), lineWidth: 0.8))
                                Text(String(store.userName.prefix(1)).uppercased())
                                    .font(.system(size: 16, weight: .bold)).foregroundColor(.white)
                            }
                        }
                    }
                }
                .padding(.horizontal, 24).padding(.top, 60).padding(.bottom, 24)

                // ── Monthly Spend Card ─────────────────────────
                VStack(alignment: .leading, spacing: 18) {
                    // Label
                    Text("SPENDINGS THIS MONTH")
                        .font(.system(size: 11, weight: .semibold)).foregroundColor(.textSecondary)
                        .tracking(1.4)

                    // Amount
                    Text(store.formatted(store.thisMonthTotal))
                        .font(.system(size: 42, weight: .bold, design: .rounded))
                        .foregroundColor(.white)

                    // Budget bar
                    VStack(spacing: 8) {
                        GeometryReader { geo in
                            ZStack(alignment: .leading) {
                                RoundedRectangle(cornerRadius: 4)
                                    .fill(Color.white.opacity(0.07)).frame(height: 5)
                                RoundedRectangle(cornerRadius: 4)
                                    .fill(store.budgetUsedFraction > 0.85 ? Color.expenseRed : Color.incomeGreen)
                                    .frame(width: geo.size.width * CGFloat(store.budgetUsedFraction), height: 5)
                            }
                        }.frame(height: 5)
                        HStack {
                            Text("Budget \(store.formatted(store.monthlyBudget))")
                                .font(.system(size: 11)).foregroundColor(.textSecondary)
                            Spacer()
                            Text("\(Int(store.budgetUsedFraction * 100))% used")
                                .font(.system(size: 11, weight: .semibold))
                                .foregroundColor(store.budgetUsedFraction > 0.85 ? .expenseRed : .textSecondary)
                        }
                    }
                }
                .padding(24)
                .background(Color.black.opacity(0.2))
                .cornerRadius(22)
                .overlay(RoundedRectangle(cornerRadius: 22).stroke(Color.white.opacity(0.05), lineWidth: 1))
                .padding(.horizontal, 20)
                .padding(.bottom, 24)

                // ── Today / Week / Avg ────────────────────────────
                HStack(spacing: 10) {
                    StatMini(label: "TODAY",    value: store.formatted(store.todayTotal),        dot: Color.accent1)
                    StatMini(label: "THIS WEEK", value: store.formatted(store.thisWeekTotal),   dot: Color.accentBlue)
                    StatMini(label: "AVG / DAY", value: store.formatted(store.averageDailySpend), dot: Color.incomeGreen)
                }
                .padding(.horizontal, 20).padding(.bottom, 22)

                // ── Lend & Borrow Card ──────────────────────────
                HStack(spacing: 14) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("LENT OUT").font(.system(size: 10, weight: .bold)).foregroundColor(.textSecondary).tracking(1)
                        Text(store.formatted(store.totalLent)).font(.system(size: 18, weight: .bold)).foregroundColor(.incomeGreen)
                    }
                    Spacer()
                    Rectangle().fill(Color.white.opacity(0.08)).frame(width: 1, height: 40)
                    Spacer()
                    VStack(alignment: .trailing, spacing: 4) {
                        Text("BORROWED").font(.system(size: 10, weight: .bold)).foregroundColor(.textSecondary).tracking(1)
                        Text(store.formatted(store.totalBorrowed)).font(.system(size: 18, weight: .bold)).foregroundColor(.expenseRed)
                    }
                }
                .padding(20).glassCard(radius: 22)
                .padding(.horizontal, 20).padding(.bottom, 28)

                // ── Category Quick Access ───────────────────────
                VStack(alignment: .leading, spacing: 12) {
                    Text("This Month")
                        .font(.system(size: 17, weight: .bold)).foregroundColor(.white)
                        .padding(.horizontal, 24)

                    let topCats = store.categoryData(for: .monthly).prefix(6)
                    if !topCats.isEmpty {
                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 10) {
                                Spacer(minLength: 16)
                                ForEach(Array(topCats)) { item in
                                    Button { selectedCategory = item.category } label: {
                                        VStack(spacing: 6) {
                                            ZStack {
                                                Circle()
                                                    .fill(item.category.color.opacity(0.18))
                                                    .frame(width: 50, height: 50)
                                                Text(item.category.emoji).font(.system(size: 24))
                                            }
                                            Text(item.category.name.components(separatedBy: " ").first ?? "")
                                                .font(.system(size: 11, weight: .semibold))
                                                .foregroundColor(.white)
                                            Text(store.formatted(item.amount))
                                                .font(.system(size: 12, weight: .bold))
                                                .foregroundColor(item.category.color)
                                                .lineLimit(1).minimumScaleFactor(0.5)
                                        }
                                        .frame(minWidth: 68)
                                    }
                                }
                                Spacer(minLength: 16)
                            }
                        }
                    }
                }
                .padding(.bottom, 24)

                // ── Recent Transactions ─────────────────────────
                VStack(alignment: .leading, spacing: 14) {
                    Text("Recent Transactions")
                        .font(.system(size: 17, weight: .bold)).foregroundColor(.white)
                        .padding(.horizontal, 24)

                    if store.transactions.isEmpty {
                        Text("No transactions yet. Tap Pay to get started.")
                            .font(.system(size: 14)).foregroundColor(.textSecondary)
                            .frame(maxWidth: .infinity).padding(40)
                    } else {
                        ForEach(store.transactions.sorted { $0.date > $1.date }.prefix(5)) { t in
                            TxRow(tx: t, onDelete: { store.deleteTransaction(id: t.id) })
                                .padding(.horizontal, 20)
                        }
                    }
                }
                .padding(.bottom, 24)
            }
        }
        .sheet(item: $selectedCategory) { cat in
            CategoryDetailView(category: cat)
                .environmentObject(store)
        }
        .sheet(isPresented: $showSettings) {
            SettingsView().environmentObject(store)
        }
    }

    private func greeting() -> String {
        let h = Calendar.current.component(.hour, from: Date())
        if h < 12 { return "Morning" }; if h < 17 { return "Afternoon" }; return "Evening"
    }
}

// MARK: - Stat Mini Card

struct StatMini: View {
    let label: String; let value: String; let dot: Color
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(label)
                .font(.system(size: 9, weight: .bold))
                .foregroundColor(.textSecondary).tracking(1.2)
            Text(value)
                .font(.system(size: 15, weight: .bold))
                .foregroundColor(.white).lineLimit(1).minimumScaleFactor(0.6)
            // Color dot at bottom like the reference
            Circle().fill(dot).frame(width: 8, height: 8)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(14).glassCard(radius: 16)
    }
}

// MARK: - Transaction Row (shared)

struct TxRow: View {
    let tx: Transaction
    var onDelete: (() -> Void)? = nil
    @EnvironmentObject var store: CloudDataStore
    @State private var showDeleteAlert = false

    var body: some View {
        HStack(spacing: 14) {
            // Person avatar (from contacts/QR scan) or colored initial fallback
            let palette: [Color] = [.catPurple, .accentBlue, .catOrange, .incomeGreen, .catPink, .catYellow]
            let avatarColor = palette[abs(tx.recipientName.hashValue) % palette.count]
            ZStack {
                if let data = store.savedAvatars[tx.recipientName], let uiImage = UIImage(data: data) {
                    Image(uiImage: uiImage)
                        .resizable().scaledToFill()
                        .frame(width: 48, height: 48)
                        .clipShape(Circle())
                } else {
                    Circle().fill(avatarColor.opacity(0.18)).frame(width: 48, height: 48)
                    Text(String(tx.recipientName.prefix(1)).uppercased())
                        .font(.system(size: 20, weight: .bold))
                        .foregroundColor(avatarColor)
                }
            }

            // Central info
            VStack(alignment: .leading, spacing: 4) {
                Text(tx.recipientName).font(.system(size: 15, weight: .semibold)).foregroundColor(.white)
                Text(tx.note.isEmpty ? tx.fallbackCategory.name : tx.note)
                    .font(.system(size: 12)).foregroundColor(.textSecondary).lineLimit(1)

                // UPI app badge
                if let app = tx.upiAppUsed {
                    Text(app.rawValue.components(separatedBy: ":").first ?? app.rawValue)
                        .font(.system(size: 9, weight: .bold))
                        .foregroundColor(app.color)
                        .padding(.horizontal, 7).padding(.vertical, 3)
                        .background(Capsule().fill(app.color.opacity(0.15)))
                }
            }

            Spacer()

            // Amount + date
            VStack(alignment: .trailing, spacing: 3) {
                Text("−\(store.formatted(tx.amount))")
                    .font(.system(size: 15, weight: .bold)).foregroundColor(.expenseRed)
                Text(tx.date, style: .date)
                    .font(.system(size: 11)).foregroundColor(.textSecondary)
            }
        }
        .padding(14).glassCard(radius: 18)
        // Long-press context menu
        .contextMenu {
            Button(role: .destructive) {
                showDeleteAlert = true
            } label: {
                Label("Payment Failed – Delete", systemImage: "xmark.circle.fill")
            }
            Button {
                UIPasteboard.general.string = tx.upiId
            } label: {
                Label("Copy UPI ID", systemImage: "doc.on.doc")
            }
        }
        // Confirmation dialog
        .alert("Remove This Expense?\(tx.splitGroupId != nil ? " (Split)" : "")", isPresented: $showDeleteAlert) {
            Button("Delete\(tx.splitGroupId != nil ? " + Remove Split Debts" : "")", role: .destructive) {
                withAnimation {
                    if let del = onDelete { del() }
                    else { store.deleteTransactionAndSplitData(tx) }
                }
            }
            Button("Keep It", role: .cancel) {}
        } message: {
            Text("Remove \"\(tx.recipientName)\" — \(store.formatted(tx.amount)) from your expenses?\n\nUse this if the payment failed or was cancelled.")
        }
    }
}

#Preview {
    DashboardView().environmentObject(CloudDataStore()).preferredColorScheme(.dark)
}
