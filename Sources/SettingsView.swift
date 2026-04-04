import SwiftUI
import PhotosUI

struct SettingsView: View {
    @EnvironmentObject var store: CloudDataStore
    @State private var showClearAlert = false

    var body: some View {
        NavigationView {
            ZStack {
                Color.bgPrimary.ignoresSafeArea()
                List {
                    // ── Profile ──────────────────────────────────
                    Section {
                        NavigationLink(destination: ProfileSettingsView()) {
                            SettingsRow(icon: "person.crop.circle.fill", iconColor: .accent1,
                                        title: "Profile", subtitle: store.userName)
                        }
                    } header: { SectionHeader("Account") }

                    // ── Finance ──────────────────────────────────
                    Section {
                        NavigationLink(destination: BudgetSettingsView()) {
                            SettingsRow(icon: "indianrupeesign.circle.fill", iconColor: .incomeGreen,
                                        title: "Monthly Budget", subtitle: store.formatted(store.monthlyBudget))
                        }
                        NavigationLink(destination: CurrencySettingsView()) {
                            SettingsRow(icon: "coloncurrencysign.circle.fill", iconColor: .accent2,
                                        title: "Currency", subtitle: store.currencySymbol)
                        }
                    } header: { SectionHeader("Finance") }

                    // ── Categories ────────────────────────────────
                    Section {
                        NavigationLink(destination: CategoryManagerView()) {
                            SettingsRow(icon: "tag.fill", iconColor: Color(red: 0.64, green: 0.61, blue: 1.0),
                                        title: "My Categories",
                                        subtitle: "\(store.categories.count) active categories")
                        }
                    } header: { SectionHeader("Categories") }

                    // ── UPI Apps ─────────────────────────────────
                    Section {
                        NavigationLink(destination: UPIInfoView()) {
                            SettingsRow(icon: "arrow.trianglehead.counterclockwise", iconColor: .accent1,
                                        title: "UPI Apps", subtitle: "GPay, PhonePe, Kotak811, Pop UPI")
                        }
                    } header: { SectionHeader("Payments") }

                    // ── Stats ─────────────────────────────────────
                    Section {
                        SettingsRowInfo(icon: "cart.fill",            iconColor: .accent2,
                                        title: "Total Transactions",  value: "\(store.transactions.count)")
                        SettingsRowInfo(icon: "chart.bar.fill",       iconColor: .incomeGreen,
                                        title: "Total Spent",
                                        value: store.formatted(store.transactions.reduce(0) { $0+$1.amount }))
                        SettingsRowInfo(icon: "arrow.up.right",       iconColor: .incomeGreen,
                                        title: "Money Lent",
                                        value: store.formatted(store.lendBorrows.filter{$0.type == .lent}.reduce(0){$0+$1.amount}))
                        SettingsRowInfo(icon: "arrow.down.left",      iconColor: .expenseRed,
                                        title: "Money Borrowed",
                                        value: store.formatted(store.lendBorrows.filter{$0.type == .borrowed}.reduce(0){$0+$1.amount}))
                    } header: { SectionHeader("Statistics") }

                    // ── About ─────────────────────────────────────
                    Section {
                        SettingsRowInfo(icon: "app.badge",         iconColor: .accent1,
                                        title: "Version", value: "1.0.0")
                        SettingsRowInfo(icon: "swift",             iconColor: Color.orange,
                                        title: "Built with", value: "SwiftUI")
                    } header: { SectionHeader("About") }

                    // ── Data Management ───────────────────────────
                    Section {
                        Button {
                            showClearAlert = true
                        } label: {
                            SettingsRow(icon: "trash.fill", iconColor: .expenseRed,
                                        title: "Clear All Data", subtitle: "Delete everything permanently",
                                        isDestructive: true)
                        }
                        
                        Button {
                            store.signOut()
                        } label: {
                            SettingsRow(icon: "rectangle.portrait.and.arrow.right.fill", iconColor: .expenseRed,
                                        title: "Sign Out", subtitle: "Log out of your account",
                                        isDestructive: true)
                        }
                    } header: { SectionHeader("Account & Data") }
                }
                .listStyle(.insetGrouped)
                .scrollContentBackground(.hidden)
                .background(Color.bgPrimary)
            }
            .navigationTitle("Settings")
            .navigationBarTitleDisplayMode(.large)
        }
        .alert("Clear All Data?", isPresented: $showClearAlert) {
            Button("Delete Everything", role: .destructive) { store.clearAll() }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("This permanently deletes all transactions, lend/borrow records and custom categories.")
        }
    }
}

// MARK: - Profile Settings

struct ProfileSettingsView: View {
    @EnvironmentObject var store: CloudDataStore
    @State private var nameInput = ""
    @State private var profileItem: PhotosPickerItem? = nil
    @FocusState private var focused: Bool

    var body: some View {
        ZStack {
            Color.bgPrimary.ignoresSafeArea()
            VStack(spacing: 24) {
                // Avatar
                PhotosPicker(selection: $profileItem, matching: .images) {
                    if let data = store.userProfileImageData, let uiImage = UIImage(data: data) {
                        Image(uiImage: uiImage).resizable().scaledToFill()
                            .frame(width: 90, height: 90).clipShape(Circle())
                            .overlay(Circle().stroke(Color.white.opacity(0.1), lineWidth: 1))
                            .overlay(
                                Circle().fill(Color.black.opacity(0.4)).frame(width: 28, height: 28)
                                    .overlay(Image(systemName: "camera.fill").font(.system(size: 11)).foregroundColor(.white))
                                    .offset(x: 30, y: 30)
                            )
                    } else {
                        ZStack {
                            Circle().fill(Color.bgCardAlt).frame(width: 90, height: 90)
                                .overlay(Circle().stroke(Color.white.opacity(0.06), lineWidth: 0.8))
                            Text(String(store.userName.prefix(1)).uppercased())
                                .font(.system(size: 36, weight: .bold)).foregroundColor(.white)
                            Circle().fill(Color.black.opacity(0.4)).frame(width: 28, height: 28)
                                .overlay(Image(systemName: "camera.fill").font(.system(size: 11)).foregroundColor(.white))
                                .offset(x: 30, y: 30)
                        }
                    }
                }
                .onChange(of: profileItem) { newItem in
                    Task {
                        if let data = try? await newItem?.loadTransferable(type: Data.self) {
                            DispatchQueue.main.async { store.setProfileImage(data) }
                        }
                    }
                }
                .padding(.top, 24)

                VStack(spacing: 4) {
                    Text(store.userName).font(.system(size: 22, weight: .bold)).foregroundColor(.white)
                    Text("Personal Finance App").font(.system(size: 13)).foregroundColor(.textSecondary)
                }

                // Name field
                VStack(alignment: .leading, spacing: 8) {
                    Text("Display Name").font(.system(size: 12, weight: .semibold)).foregroundColor(.textSecondary)
                    HStack {
                        TextField(store.userName, text: $nameInput)
                            .focused($focused)
                            .font(.system(size: 15)).foregroundColor(.white)
                        if !nameInput.isEmpty {
                            Button("Save") {
                                store.setName(nameInput); nameInput = ""; focused = false
                            }.foregroundColor(.accent1).font(.system(size: 14, weight: .bold))
                        }
                    }
                    .padding(16).glassCard()
                }
                .padding(.horizontal, 20)

                Spacer()
            }
        }
        .navigationTitle("Profile")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItemGroup(placement: .keyboard) {
                Spacer()
                Button("Done") { focused = false }.foregroundColor(.accent1)
            }
        }
    }
}

// MARK: - Budget Settings

struct BudgetSettingsView: View {
    @EnvironmentObject var store: CloudDataStore
    @State private var budgetInput = ""
    @FocusState private var focused: Bool

    var body: some View {
        ZStack {
            Color.bgPrimary.ignoresSafeArea()
            VStack(spacing: 24) {
                // Current
                VStack(spacing: 6) {
                    Text("Current Monthly Budget")
                        .font(.system(size: 13)).foregroundColor(.textSecondary)
                    Text(store.formatted(store.monthlyBudget))
                        .font(.system(size: 42, weight: .bold, design: .rounded)).foregroundColor(.accent1)

                    // Used bar
                    let fr = store.budgetUsedFraction
                    VStack(spacing: 6) {
                        GeometryReader { g in
                            ZStack(alignment: .leading) {
                                RoundedRectangle(cornerRadius: 5).fill(Color.white.opacity(0.1)).frame(height: 8)
                                RoundedRectangle(cornerRadius: 5)
                                    .fill(fr > 0.85 ? Color.expenseRed : Color.incomeGreen)
                                    .frame(width: g.size.width * CGFloat(fr), height: 8)
                            }
                        }.frame(height: 8)
                        HStack {
                            Text("Spent: \(store.formatted(store.thisMonthTotal))")
                                .font(.system(size: 12)).foregroundColor(.textSecondary)
                            Spacer()
                            Text("\(Int(fr * 100))% used").font(.system(size: 12, weight: .semibold))
                                .foregroundColor(fr > 0.85 ? .expenseRed : .textSecondary)
                        }
                    }
                }
                .padding(24).glassCard(radius: 22).padding(.horizontal, 20)

                // New budget input
                VStack(alignment: .leading, spacing: 10) {
                    Text("Set New Budget").font(.system(size: 12, weight: .semibold)).foregroundColor(.textSecondary)
                    HStack {
                        Text(store.currencySymbol).foregroundColor(.accent1).font(.system(size: 18, weight: .bold))
                        TextField("Enter amount", text: $budgetInput)
                            .focused($focused)
                            .keyboardType(.decimalPad)
                            .font(.system(size: 18)).foregroundColor(.white)
                        if !budgetInput.isEmpty {
                            Button("Set") {
                                if let v = Double(budgetInput) { store.setBudget(v); budgetInput = ""; focused = false }
                            }
                            .font(.system(size: 14, weight: .bold)).foregroundColor(.accent1)
                        }
                    }
                    .padding(16).glassCard()
                }
                .padding(.horizontal, 20)

                Spacer()
            }
            .padding(.top, 20)
        }
        .navigationTitle("Monthly Budget")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItemGroup(placement: .keyboard) {
                Spacer()
                Button("Done") { focused = false }.foregroundColor(.accent1)
            }
        }
    }
}

// MARK: - Currency Settings

struct CurrencySettingsView: View {
    @EnvironmentObject var store: CloudDataStore
    let currencies = [("₹","Indian Rupee"),("$","US Dollar"),("€","Euro"),("£","British Pound"),("¥","Japanese Yen"),("د.إ","UAE Dirham")]

    var body: some View {
        ZStack {
            Color.bgPrimary.ignoresSafeArea()
            List {
                ForEach(currencies, id: \.0) { symbol, name in
                    Button {
                        store.setCurrency(symbol)
                    } label: {
                        HStack(spacing: 16) {
                            Text(symbol).font(.system(size: 24, weight: .bold)).foregroundColor(.accent1).frame(width: 36)
                            VStack(alignment: .leading, spacing: 2) {
                                Text(name).font(.system(size: 15, weight: .medium)).foregroundColor(.white)
                                Text(symbol).font(.system(size: 12)).foregroundColor(.textSecondary)
                            }
                            Spacer()
                            if store.currencySymbol == symbol {
                                Image(systemName: "checkmark.circle.fill").foregroundColor(.accent1)
                            }
                        }
                        .padding(.vertical, 4)
                    }
                    .listRowBackground(Color.bgCard)
                }
            }
            .listStyle(.insetGrouped)
            .scrollContentBackground(.hidden)
        }
        .navigationTitle("Currency")
        .navigationBarTitleDisplayMode(.inline)
    }
}

// MARK: - Category Manager

struct CategoryManagerView: View {
    @EnvironmentObject var store: CloudDataStore
    @State private var newName      = ""
    @State private var newEmoji     = ""
    @State private var selectedHex  = "#A29BFE"
    @State private var editCategory : AppCategory? = nil
    @FocusState private var focused: Bool

    let colorOptions: [(String, Color)] = [
        ("#FF6B6B", Color(red:1.00,green:0.42,blue:0.42)), ("#FF9F43", Color(red:1.00,green:0.62,blue:0.26)),
        ("#FDCB6E", Color(red:0.99,green:0.80,blue:0.43)), ("#1DD1A1", Color(red:0.11,green:0.82,blue:0.63)),
        ("#48DBFB", Color(red:0.28,green:0.86,blue:0.98)), ("#A29BFE", Color(red:0.64,green:0.61,blue:1.00)),
        ("#FD79A8", Color(red:0.99,green:0.47,blue:0.66)), ("#6C5CE7", Color(red:0.42,green:0.36,blue:0.91)),
        ("#D980FA", Color(red:0.85,green:0.50,blue:0.98)), ("#B53471", Color(red:0.71,green:0.20,blue:0.44)),
        ("#EE5A24", Color(red:0.93,green:0.35,blue:0.14)), ("#009432", Color(red:0.00,green:0.58,blue:0.20))
    ]

    let gridCols = [GridItem(.adaptive(minimum: 36), spacing: 12)]

    var body: some View {
        ZStack {
            Color.bgPrimary.ignoresSafeArea()
            List {
                // Categories List
                Section {
                    if store.categories.isEmpty {
                        Text("No categories – add one below!")
                            .font(.system(size: 13)).foregroundColor(.textSecondary)
                            .listRowBackground(Color.bgCard)
                    } else {
                        ForEach(store.categories) { cat in
                            Button {
                                editCategory = cat
                            } label: {
                                HStack(spacing: 14) {
                                    Text(cat.emoji).font(.system(size: 22))
                                    Text(cat.name).font(.system(size: 15)).foregroundColor(.white)
                                    Spacer()
                                    Circle().fill(cat.color).frame(width: 12, height: 12)
                                }
                            }
                            .listRowBackground(Color.bgCard)
                        }
                        .onDelete { idx in idx.forEach { store.deleteAppCategory(id: store.categories[$0].id) } }
                    }
                } header: { SectionHeader("All Categories (tap to edit)") }

                // Add form
                Section {
                    VStack(spacing: 16) {
                        HStack(spacing: 10) {
                            TextField("😀", text: $newEmoji).font(.system(size: 26))
                                .frame(width: 44).multilineTextAlignment(.center)
                                .padding(10).glassCard(radius: 12)
                            TextField("Category name", text: $newName)
                                .focused($focused)
                                .font(.system(size: 15)).foregroundColor(.white)
                                .padding(14).glassCard(radius: 12)
                        }
                        
                        VStack(alignment: .leading, spacing: 10) {
                            Text("Color:").font(.system(size: 12, weight: .semibold)).foregroundColor(.textSecondary)
                            LazyVGrid(columns: gridCols, spacing: 12) {
                                ForEach(colorOptions, id: \.0) { hex, col in
                                    Button { selectedHex = hex } label: {
                                        Circle().fill(col).frame(width: 30, height: 30)
                                            .overlay(Circle().stroke(Color.white, lineWidth: selectedHex == hex ? 2.5 : 0))
                                    }
                                    .buttonStyle(.plain)
                                }
                            }
                        }
                        .padding(.vertical, 4)

                        Button("Add Category") {
                            guard !newName.isEmpty else { return }
                            let emoji = newEmoji.isEmpty ? "📌" : String(newEmoji.prefix(1))
                            store.addAppCategory(AppCategory(name: newName, emoji: emoji, colorHex: selectedHex))
                            newName = ""
                            newEmoji = ""
                            UIApplication.shared.hideKeyboard()
                        }
                        .font(.system(size: 15, weight: .bold)).foregroundColor(.white)
                        .frame(maxWidth: .infinity).frame(height: 46)
                        .background(RoundedRectangle(cornerRadius: 14).fill(newName.isEmpty ? LinearGradient(colors: [Color.bgCard], startPoint: .leading, endPoint: .trailing) : LinearGradient.accentGradient))
                        .disabled(newName.isEmpty)
                        .buttonStyle(.plain)
                    }
                    .padding(.vertical, 8)
                    .listRowBackground(Color.bgCard)
                } header: { SectionHeader("Add New") }
            }
            .listStyle(.insetGrouped)
            .scrollContentBackground(.hidden)
        }
        .navigationTitle("Categories")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItemGroup(placement: .keyboard) {
                Spacer()
                Button("Done") { UIApplication.shared.hideKeyboard() }.foregroundColor(.accent1)
            }
        }
        .sheet(item: $editCategory) { cat in
            EditCategorySheet(category: cat)
                .environmentObject(store)
        }
    }
}

// MARK: - Edit Category Sheet

struct EditCategorySheet: View {
    @EnvironmentObject var store: CloudDataStore
    @Environment(\.dismiss) var dismiss
    
    let originalId: UUID
    @State private var name: String
    @State private var emoji: String
    @State private var selectedHex: String
    @FocusState private var focused: Bool

    init(category: AppCategory) {
        self.originalId = category.id
        _name = State(initialValue: category.name)
        _emoji = State(initialValue: category.emoji)
        _selectedHex = State(initialValue: category.colorHex)
    }

    let colorOptions: [(String, Color)] = [
        ("#FF6B6B", Color(red:1.00,green:0.42,blue:0.42)), ("#FF9F43", Color(red:1.00,green:0.62,blue:0.26)),
        ("#FDCB6E", Color(red:0.99,green:0.80,blue:0.43)), ("#1DD1A1", Color(red:0.11,green:0.82,blue:0.63)),
        ("#48DBFB", Color(red:0.28,green:0.86,blue:0.98)), ("#A29BFE", Color(red:0.64,green:0.61,blue:1.00)),
        ("#FD79A8", Color(red:0.99,green:0.47,blue:0.66)), ("#6C5CE7", Color(red:0.42,green:0.36,blue:0.91)),
        ("#D980FA", Color(red:0.85,green:0.50,blue:0.98)), ("#B53471", Color(red:0.71,green:0.20,blue:0.44)),
        ("#EE5A24", Color(red:0.93,green:0.35,blue:0.14)), ("#009432", Color(red:0.00,green:0.58,blue:0.20))
    ]
    let gridCols = [GridItem(.adaptive(minimum: 36), spacing: 12)]

    var body: some View {
        NavigationView {
            ZStack {
                Color.bgPrimary.ignoresSafeArea()
                VStack(spacing: 24) {
                    VStack(spacing: 8) {
                        Text("Edit Category").font(.system(size: 20, weight: .bold)).foregroundColor(.white)
                    }

                    // Edit form
                    HStack(spacing: 12) {
                        TextField("😀", text: $emoji).font(.system(size: 28))
                            .frame(width: 50, height: 50).multilineTextAlignment(.center)
                            .glassCard(radius: 14)
                        TextField("Category name", text: $name)
                            .focused($focused)
                            .font(.system(size: 16)).foregroundColor(.white)
                            .padding(16).glassCard(radius: 14)
                    }
                    
                    VStack(alignment: .leading, spacing: 14) {
                        Text("Color").font(.system(size: 13, weight: .semibold)).foregroundColor(.textSecondary)
                        LazyVGrid(columns: gridCols, spacing: 14) {
                            ForEach(colorOptions, id: \.0) { hex, col in
                                Button { selectedHex = hex } label: {
                                    Circle().fill(col).frame(width: 34, height: 34)
                                        .overlay(Circle().stroke(Color.white, lineWidth: selectedHex == hex ? 3 : 0))
                                }
                            }
                        }
                    }
                    .padding(16).glassCard(radius: 14)

                    // Confirm button
                    Button {
                        UIApplication.shared.hideKeyboard()
                        let finalEmoji = emoji.isEmpty ? "📌" : String(emoji.prefix(1))
                        store.updateAppCategory(AppCategory(id: originalId, name: name, emoji: finalEmoji, colorHex: selectedHex))
                        dismiss()
                    } label: {
                        Text("Save Changes")
                            .font(.system(size: 16, weight: .bold)).foregroundColor(.white)
                            .frame(maxWidth: .infinity).frame(height: 52)
                            .background(name.trimmingCharacters(in: .whitespaces).isEmpty
                                        ? LinearGradient(colors: [.bgCard], startPoint: .leading, endPoint: .trailing)
                                        : LinearGradient.accentGradient)
                            .clipShape(RoundedRectangle(cornerRadius: 14))
                    }
                    .disabled(name.trimmingCharacters(in: .whitespaces).isEmpty)

                    Spacer()
                }
                .padding(24)
            }
            .navigationBarHidden(true)
            .toolbar {
                ToolbarItemGroup(placement: .keyboard) {
                    Spacer()
                    Button("Done") { UIApplication.shared.hideKeyboard() }
                        .foregroundColor(.accent1)
                }
            }
            .overlay(alignment: .topTrailing) {
                Button { dismiss() } label: {
                    Image(systemName: "xmark.circle.fill").font(.system(size: 24)).foregroundColor(.textSecondary)
                }.padding().padding(.top, 10)
            }
        }
    }
}

// MARK: - UPI Info View

struct UPIInfoView: View {
    var body: some View {
        ZStack {
            Color.bgPrimary.ignoresSafeArea()
            List {
                ForEach(UPIApp.allCases) { app in
                    HStack(spacing: 16) {
                        Image(app.logoName)
                            .resizable().scaledToFit()
                            .frame(width: 44, height: 44)
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                        VStack(alignment: .leading, spacing: 3) {
                            Text(app.rawValue).font(.system(size: 15, weight: .semibold)).foregroundColor(.white)
                            Text("Scheme: \(app.scheme)://").font(.system(size: 11)).foregroundColor(.textSecondary)
                        }
                        Spacer()
                        Circle().fill(app.color).frame(width: 10, height: 10)
                    }
                    .listRowBackground(Color.bgCard)
                }
            }
            .listStyle(.insetGrouped)
            .scrollContentBackground(.hidden)
        }
        .navigationTitle("UPI Apps")
        .navigationBarTitleDisplayMode(.inline)
    }
}

// MARK: - Reusable Row Components

struct SettingsRow: View {
    let icon: String; let iconColor: Color; let title: String
    var subtitle: String = ""; var isDestructive: Bool = false
    var body: some View {
        HStack(spacing: 14) {
            ZStack {
                RoundedRectangle(cornerRadius: 9).fill(iconColor).frame(width: 34, height: 34)
                Image(systemName: icon).font(.system(size: 15, weight: .semibold)).foregroundColor(.white)
            }
            VStack(alignment: .leading, spacing: 2) {
                Text(title).font(.system(size: 15)).foregroundColor(isDestructive ? .expenseRed : .white)
                if !subtitle.isEmpty {
                    Text(subtitle).font(.system(size: 12)).foregroundColor(.textSecondary).lineLimit(1)
                }
            }
            Spacer()
        }
    }
}

struct SettingsRowInfo: View {
    let icon: String; let iconColor: Color; let title: String; let value: String
    var body: some View {
        HStack(spacing: 14) {
            ZStack {
                RoundedRectangle(cornerRadius: 9).fill(iconColor).frame(width: 34, height: 34)
                Image(systemName: icon).font(.system(size: 15, weight: .semibold)).foregroundColor(.white)
            }
            Text(title).font(.system(size: 15)).foregroundColor(.white)
            Spacer()
            Text(value).font(.system(size: 14, weight: .semibold)).foregroundColor(.textSecondary)
        }
    }
}

struct SectionHeader: View {
    let title: String
    init(_ title: String) { self.title = title }
    var body: some View {
        Text(title.uppercased())
            .font(.system(size: 11, weight: .bold))
            .foregroundColor(.textSecondary)
            .tracking(0.8)
    }
}

// MARK: - Backwards-compat stubs (used in other files)
struct SettingsGroup<Content: View>: View {
    let title: String; @ViewBuilder let content: Content
    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(title.uppercased()).font(.system(size: 11, weight: .bold)).foregroundColor(.textSecondary).tracking(0.8)
                .padding(.horizontal, 4)
            content.padding(16).glassCard(radius: 18)
        }
    }
}

struct StatRow: View {
    let label: String; let value: String
    var body: some View {
        HStack {
            Text(label).font(.system(size: 13)).foregroundColor(.textSecondary)
            Spacer()
            Text(value).font(.system(size: 13, weight: .bold)).foregroundColor(.white)
        }
    }
}

#Preview {
    SettingsView().environmentObject(CloudDataStore()).preferredColorScheme(.dark)
}
