import SwiftUI

struct HistoryView: View {
    @EnvironmentObject var store: DataStore
    @State private var search       = ""
    @State private var filterCat    : AppCategory? = nil
    @State private var showFilters  = false

    // Transactions matching search + filter
    var filtered: [Transaction] {
        store.transactions.filter { t in
            let matchSearch = search.isEmpty
                || t.recipientName.localizedCaseInsensitiveContains(search)
                || t.note.localizedCaseInsensitiveContains(search)
                || t.categoryName.localizedCaseInsensitiveContains(search)
            let matchCat = filterCat == nil || t.categoryId == filterCat?.id
            return matchSearch && matchCat
        }
        .sorted { $0.date > $1.date }
    }

    // Group by day — store actual Date for reliable sorting
    struct TxGroup: Identifiable {
        let id       = UUID()
        let label    : String
        let sortDate : Date
        let items    : [Transaction]
    }

    var groups: [TxGroup] {
        let cal = Calendar.current
        var dict: [String: (Date, [Transaction])] = [:]
        for t in filtered {
            let label: String
            let anchor: Date
            if cal.isDateInToday(t.date) {
                label = "Today"; anchor = Date()
            } else if cal.isDateInYesterday(t.date) {
                label = "Yesterday"
                anchor = cal.date(byAdding: .day, value: -1, to: cal.startOfDay(for: Date()))!
            } else {
                let f = DateFormatter(); f.dateStyle = .medium
                label = f.string(from: t.date)
                anchor = cal.startOfDay(for: t.date)
            }
            var entry = dict[label] ?? (anchor, [])
            entry.1.append(t)
            dict[label] = entry
        }
        return dict
            .map { TxGroup(label: $0.key, sortDate: $0.value.0, items: $0.value.1) }
            .sorted { $0.sortDate > $1.sortDate }
    }

    var body: some View {
        VStack(spacing: 0) {

            // ── Header ──────────────────────────────────────────
            HStack {
                Text("History")
                    .font(.system(size: 30, weight: .bold)).foregroundColor(.white)
                Spacer()
                Button {
                    withAnimation { showFilters.toggle() }
                } label: {
                    Image(systemName: filterCat != nil
                          ? "line.3.horizontal.decrease.circle.fill"
                          : "line.3.horizontal.decrease.circle")
                    .font(.system(size: 22)).foregroundColor(.accent1)
                }
            }
            .padding(.horizontal, 24).padding(.top, 60).padding(.bottom, 14)

            // ── Search Bar ──────────────────────────────────────
            HStack(spacing: 10) {
                Image(systemName: "magnifyingglass").foregroundColor(.textSecondary)
                TextField("Search by name, note, category…", text: $search)
                    .font(.system(size: 15)).foregroundColor(.white)
                if !search.isEmpty {
                    Button { search = "" } label: {
                        Image(systemName: "xmark.circle.fill").foregroundColor(.textSecondary)
                    }
                }
            }
            .padding(14).glassCard(radius: 16)
            .padding(.horizontal, 20).padding(.bottom, 10)

            // ── Filter Pills ────────────────────────────────────
            if showFilters {
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        Spacer(minLength: 16)
                        if filterCat != nil {
                            Button { filterCat = nil } label: {
                                Label("Clear", systemImage: "xmark")
                                    .font(.system(size: 11, weight: .semibold))
                                    .foregroundColor(.expenseRed)
                                    .padding(.horizontal, 12).padding(.vertical, 7)
                                    .background(Capsule().fill(Color.expenseRed.opacity(0.15)))
                            }
                        }
                        ForEach(store.categories) { cat in
                            Button { filterCat = filterCat == cat ? nil : cat } label: {
                                HStack(spacing: 4) {
                                    Text(cat.emoji).font(.system(size: 12))
                                    Text(cat.name.components(separatedBy: " ").first ?? "")
                                        .font(.system(size: 11, weight: .semibold))
                                }
                                .foregroundColor(filterCat == cat ? .white : .textSecondary)
                                .padding(.horizontal, 12).padding(.vertical, 7)
                                .background(Capsule().fill(filterCat == cat ? cat.color.opacity(0.7) : Color.bgCard))
                            }
                        }
                        Spacer(minLength: 16)
                    }
                }
                .padding(.bottom, 10)
                .transition(.opacity.combined(with: .move(edge: .top)))
            }

            // ── List ─────────────────────────────────────────────
            if store.transactions.isEmpty {
                VStack(spacing: 16) {
                    Image(systemName: "creditcard").font(.system(size: 48)).foregroundColor(.textSecondary.opacity(0.3))
                    Text("No expenses yet").font(.system(size: 15)).foregroundColor(.textSecondary)
                    Text("Tap ₹ to log your first payment").font(.system(size: 13)).foregroundColor(.textSecondary.opacity(0.6))
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)

            } else if groups.isEmpty {
                VStack(spacing: 14) {
                    Image(systemName: "magnifyingglass").font(.system(size: 44)).foregroundColor(.textSecondary.opacity(0.3))
                    Text("No transactions found").font(.system(size: 15)).foregroundColor(.textSecondary)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)

            } else {
                List {
                    ForEach(groups) { group in
                        Section {
                            ForEach(group.items) { t in
                                TxRow(tx: t, onDelete: { store.deleteTransaction(id: t.id) })
                                    .listRowBackground(Color.clear)
                                    .listRowInsets(EdgeInsets(top: 5, leading: 20, bottom: 5, trailing: 20))
                                    .listRowSeparator(.hidden)
                                    .swipeActions(edge: .trailing) {
                                        Button(role: .destructive) {
                                            store.deleteTransaction(id: t.id)
                                        } label: { Label("Delete", systemImage: "trash.fill") }
                                    }
                            }
                        } header: {
                            Text(group.label)
                                .font(.system(size: 12, weight: .semibold))
                                .foregroundColor(.textSecondary)
                                .padding(.horizontal, 4)
                        }
                    }
                    Color.clear.frame(height: 8)
                        .listRowBackground(Color.clear).listRowSeparator(.hidden)
                }
                .listStyle(.plain)
                .scrollContentBackground(.hidden)
            }
        }
    }
}

#Preview {
    HistoryView().environmentObject(DataStore()).preferredColorScheme(.dark)
        .background(Color.bgPrimary)
}
