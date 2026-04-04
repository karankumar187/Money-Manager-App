import SwiftUI

// MARK: - Person Detail View

struct PersonDetailView: View {
    let personName : String
    let phone      : String?
    @EnvironmentObject var store: CloudDataStore
    @Environment(\.dismiss) var dismiss

    @State private var showAddSheet      = false
    @State private var addType           : LendBorrowType = .lent
    @State private var partialTarget     : LendBorrow? = nil
    @State private var editTarget        : LendBorrow? = nil
    @State private var showDeleteAlert   = false
    @State private var deleteTarget      : LendBorrow? = nil
    @State private var showSendMoney     = false
    @State private var showClipboardHint = false
    @State private var contactProfile    : UserProfile? = nil   // fetched from Firestore if they have an account

    // All entries for this person
    var entries: [LendBorrow] {
        store.lendBorrows.filter { $0.personName == personName }.sorted { $0.date > $1.date }
    }
    var lentEntries    : [LendBorrow] { entries.filter { $0.type == .lent } }
    var borrowedEntries: [LendBorrow] { entries.filter { $0.type == .borrowed } }

    var totalLentOut  : Double { lentEntries.filter     { !$0.isPaid }.reduce(0) { $0 + $1.remainingAmount } }
    var totalBorrowed : Double { borrowedEntries.filter { !$0.isPaid }.reduce(0) { $0 + $1.remainingAmount } }
    var netBalance    : Double { totalLentOut - totalBorrowed }

    // Normal transactions for this person (Transfers)
    var transfers: [Transaction] {
        store.transactions.filter { $0.recipientName == personName }.sorted { $0.date > $1.date }
    }

    @State private var selectedTab = 0 // 0 = Lends, 1 = Transfers

    // Avatar color from name hash
    var avatarColor: Color {
        let palette: [Color] = [.catPurple, .accentBlue, .catOrange, .incomeGreen, .catPink, .catYellow]
        return palette[abs(personName.hashValue) % palette.count]
    }

    var body: some View {
        NavigationView {
            ZStack {
                Color.bgPrimary.ignoresSafeArea()
                ScrollView(showsIndicators: false) {
                    VStack(spacing: 0) {

                        // ── Person Header Card ──────────────────────
                        VStack(spacing: 16) {
                            HStack(spacing: 16) {
                                // Avatar — prefer account profile pic, then local avatar, then initials
                                ZStack {
                                    if let urlStr = contactProfile?.profileImageURL,
                                       let url = URL(string: urlStr) {
                                        AsyncImage(url: url) { phase in
                                            if let img = phase.image {
                                                img.resizable().scaledToFill()
                                                    .frame(width: 64, height: 64).clipShape(Circle())
                                            } else {
                                                Circle().fill(avatarColor.opacity(0.18)).frame(width: 64, height: 64)
                                                Text(String(personName.prefix(1)).uppercased())
                                                    .font(.system(size: 26, weight: .bold)).foregroundColor(avatarColor)
                                            }
                                        }
                                    } else if let data = store.savedAvatars[personName], let uiImage = UIImage(data: data) {
                                        Image(uiImage: uiImage)
                                            .resizable().scaledToFill()
                                            .frame(width: 64, height: 64).clipShape(Circle())
                                    } else {
                                        Circle().fill(avatarColor.opacity(0.18)).frame(width: 64, height: 64)
                                        Text(String(personName.prefix(1)).uppercased())
                                            .font(.system(size: 26, weight: .bold)).foregroundColor(avatarColor)
                                    }
                                }
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(personName).font(.system(size: 20, weight: .bold)).foregroundColor(.white)
                                    if let p = phone {
                                        Label(p, systemImage: "phone")
                                            .font(.system(size: 12)).foregroundColor(.textSecondary)
                                    }
                                    Text("\(entries.count) records")
                                        .font(.system(size: 12)).foregroundColor(.textSecondary)
                                }
                                Spacer()
                            }

                            // Net balance bar
                            HStack(spacing: 12) {
                                VStack(alignment: .leading, spacing: 4) {
                                    Text("LENT").font(.system(size: 9, weight: .bold)).foregroundColor(.textSecondary).tracking(1.2)
                                    Text(store.formatted(totalLentOut))
                                        .font(.system(size: 18, weight: .bold, design: .rounded)).foregroundColor(.incomeGreen)
                                    Circle().fill(Color.incomeGreen).frame(width: 7, height: 7)
                                }
                                .frame(maxWidth: .infinity, alignment: .leading)

                                Rectangle().fill(Color.textTertiary).frame(width: 1, height: 44)

                                VStack(alignment: .trailing, spacing: 4) {
                                    Text("BORROWED").font(.system(size: 9, weight: .bold)).foregroundColor(.textSecondary).tracking(1.2)
                                    Text(store.formatted(totalBorrowed))
                                        .font(.system(size: 18, weight: .bold, design: .rounded)).foregroundColor(.expenseRed)
                                    Circle().fill(Color.expenseRed).frame(width: 7, height: 7)
                                }
                                .frame(maxWidth: .infinity, alignment: .trailing)
                            }

                            // Net indicator
                            HStack {
                                Text("NET").font(.system(size: 9, weight: .bold)).foregroundColor(.textSecondary).tracking(1.2)
                                Spacer()
                                let c = netBalance > 0 ? Color.incomeGreen : netBalance < 0 ? Color.expenseRed : Color.textSecondary
                                Text(netBalance == 0 ? "All settled ✓" :
                                     netBalance > 0 ? "\(personName.components(separatedBy: " ").first ?? personName) owes you \(store.formatted(netBalance))" :
                                        "You owe \(store.formatted(abs(netBalance)))")
                                    .font(.system(size: 12, weight: .semibold)).foregroundColor(c)
                            }
                        }
                        .padding(20).glassCard(radius: 22)
                        .padding(.horizontal, 20).padding(.top, 16).padding(.bottom, 16)

                        // ── Send Money Button ──────────────────────────
                        Button { showSendMoney = true } label: {
                            HStack {
                                Image(systemName: "paperplane.fill")
                                Text(netBalance < 0
                                     ? "Settle Balance (\(store.formatted(abs(netBalance))))"
                                     : "Send Money to \(personName.components(separatedBy: " ").first ?? personName)")
                            }
                            .font(.system(size: 16, weight: .bold)).foregroundColor(.white)
                            .frame(maxWidth: .infinity).frame(height: 52)
                            .background(LinearGradient.accentGradient)
                            .clipShape(RoundedRectangle(cornerRadius: 16))
                            .shadow(color: Color.accent1.opacity(0.3), radius: 10, y: 4)
                        }
                        .padding(.horizontal, 20).padding(.bottom, 28)

                        // ── Tabs (Lends | Transfers) ──────────────────────────
                        Picker("", selection: $selectedTab) {
                            Text("Lends").tag(0)
                            Text("Transfers").tag(1)
                        }
                        .pickerStyle(.segmented)
                        .padding(.horizontal, 20).padding(.bottom, 24)

                        if selectedTab == 0 {
                            // ── Lends Section ──────────────────────────────
                            if lentEntries.isEmpty && borrowedEntries.isEmpty {
                                Text("No lend records for this person.")
                                    .font(.system(size: 14)).foregroundColor(.textSecondary)
                                    .frame(maxWidth: .infinity, alignment: .center).padding(40)
                            } else {
                                if !lentEntries.isEmpty {
                                    EntrySection(
                                        title: "LENT",
                                        entries: lentEntries,
                                        accentColor: .incomeGreen,
                                        onPartialPay: { partialTarget = $0 },
                                        onMarkPaid: { store.markPaid(id: $0.id) },
                                        onEdit: { editTarget = $0 },
                                        onDelete: { deleteTarget = $0; showDeleteAlert = true }
                                    )
                                    .padding(.bottom, 20)
                                }
                                if !borrowedEntries.isEmpty {
                                    EntrySection(
                                        title: "BORROWED",
                                        entries: borrowedEntries,
                                        accentColor: .expenseRed,
                                        onPartialPay: { partialTarget = $0 },
                                        onMarkPaid: { store.markPaid(id: $0.id) },
                                        onEdit: { editTarget = $0 },
                                        onDelete: { deleteTarget = $0; showDeleteAlert = true }
                                    )
                                    .padding(.bottom, 20)
                                }
                            }
                        } else {
                            // ── Transfers Section ──────────────────────────
                            if transfers.isEmpty {
                                Text("No direct transfer history for this person.")
                                    .font(.system(size: 14)).foregroundColor(.textSecondary)
                                    .frame(maxWidth: .infinity, alignment: .center).padding(40)
                            } else {
                                VStack(spacing: 14) {
                                    ForEach(transfers) { t in
                                        TxRow(tx: t, onDelete: { store.deleteTransaction(id: t.id) })
                                            .padding(.horizontal, 20)
                                    }
                                }
                                .padding(.bottom, 20)
                            }
                        }

                        // ── Add Buttons ────────────────────────────────
                        HStack(spacing: 12) {
                            AddEntryButton(label: "Add Lend", icon: "arrow.up.circle", color: .incomeGreen) {
                                addType = .lent; showAddSheet = true
                            }
                            AddEntryButton(label: "Add Borrow", icon: "arrow.down.circle", color: .expenseRed) {
                                addType = .borrowed; showAddSheet = true
                            }
                        }
                        .padding(.horizontal, 20).padding(.bottom, 40)
                    }
                }
            }
            .navigationTitle(personName)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Done") { dismiss() }.foregroundColor(.accent1)
                }
            }
        }
        // ── Sheets & Overlays ──────────────────────────
        // Send Money sheet (unified: amount + note + category + lend toggle + UPI)
        .sheet(isPresented: $showSendMoney) {
            SendMoneySheet(
                personName: personName,
                phone: phone,
                prefilledAmount: netBalance < 0 ? abs(netBalance) : 0,
                prefilledUPI: contactProfile?.upiId   // auto-fill from their account
            ) {
                withAnimation { showClipboardHint = true }
                DispatchQueue.main.asyncAfter(deadline: .now() + 3) {
                    withAnimation { showClipboardHint = false }
                }
            }
            .environmentObject(store)
        }
        // Partial payment
        .sheet(item: $partialTarget) { lb in
            PartialPaySheet(entry: lb).environmentObject(store)
        }
        // Edit entry
        .sheet(item: $editTarget) { lb in
            EditEntrySheet(entry: lb).environmentObject(store)
        }
        // Delete confirmation
        .alert("Delete Entry?", isPresented: $showDeleteAlert) {
            Button("Delete", role: .destructive) {
                if let t = deleteTarget { store.deleteLendBorrow(id: t.id) }
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("This will permanently remove this lending record.")
        }
        // Add new lend/borrow entry
        .sheet(isPresented: $showAddSheet) {
            LBAddSheet(
                prefilledName: personName,
                prefilledPhone: phone,
                prefilledType: addType,
                onContactPick: nil
            ).environmentObject(store)
        }
        // Clipboard hint banner
        .overlay(alignment: .bottom) {
            if showClipboardHint {
                HStack(spacing: 10) {
                    Image(systemName: "doc.on.clipboard.fill").foregroundColor(.accent1)
                    VStack(alignment: .leading, spacing: 2) {
                        Text("UPI ID copied!").font(.system(size: 13, weight: .semibold)).foregroundColor(.white)
                        Text("Paste it in the payment app").font(.system(size: 11)).foregroundColor(.textSecondary)
                    }
                    Spacer()
                }
                .padding(14)
                .background(Color.bgCard)
                .clipShape(RoundedRectangle(cornerRadius: 16))
                .overlay(RoundedRectangle(cornerRadius: 16).stroke(Color.accent1.opacity(0.3), lineWidth: 1))
                .padding(.horizontal, 20).padding(.bottom, 30)
                .transition(.move(edge: .bottom).combined(with: .opacity))
                .shadow(color: .black.opacity(0.3), radius: 12, y: 4)
            }
        }
        .animation(.spring(response: 0.35), value: showClipboardHint)
        .task {
            // Fetch contact's account profile (for UPI auto-fill + profile pic)
            if let p = phone {
                contactProfile = await store.fetchContactProfile(byPhone: p)
            }
        }
    }
}

// MARK: - Send Money Sheet (unified: amount + note + category + lend toggle + UPI)

struct SendMoneySheet: View {
    let personName      : String
    let phone           : String?
    let prefilledAmount : Double
    var  prefilledUPI   : String? = nil   // auto-filled from contact's MoneyManager account
    let onPaid          : () -> Void

    @EnvironmentObject var store: CloudDataStore
    @Environment(\.dismiss) var dismiss

    @State private var amountStr  = ""
    @State private var note       = ""
    @State private var selectedCat: AppCategory? = nil
    @State private var addAsLend  = false
    @State private var selectedApp: UPIApp = .gpay
    @State private var showScanner = false
    @State private var upiId      : String = ""
    @FocusState private var amountFocused: Bool

    var amount: Double   { Double(amountStr) ?? 0 }
    var canProceed: Bool { amount > 0 }

    // UPI priority: user-typed > saved locally > from their account
    var savedUPI: String? { store.savedUPIs[personName] }
    var effectiveUPI: String {
        if !upiId.isEmpty { return upiId }
        if let s = savedUPI, !s.isEmpty { return s }
        return prefilledUPI ?? ""
    }
    var hasUPI: Bool { !effectiveUPI.isEmpty }
    var upiFromAccount: Bool { effectiveUPI == prefilledUPI && !(prefilledUPI ?? "").isEmpty && upiId.isEmpty && savedUPI == nil }
    var firstName: String { personName.components(separatedBy: " ").first ?? personName }

    var avatarColor: Color {
        let p: [Color] = [.catPurple, .accentBlue, .catOrange, .incomeGreen, .catPink, .catYellow]
        return p[abs(personName.hashValue) % p.count]
    }

    var body: some View {
        NavigationView {
            ZStack {
                Color.bgPrimary.ignoresSafeArea()
                ScrollView(showsIndicators: false) {
                    VStack(spacing: 18) {

                        // ── Person header ────────────────────────────
                        HStack(spacing: 14) {
                            ZStack {
                                if let data = store.savedAvatars[personName], let img = UIImage(data: data) {
                                    Image(uiImage: img).resizable().scaledToFill()
                                        .frame(width: 52, height: 52).clipShape(Circle())
                                } else {
                                    Circle().fill(avatarColor.opacity(0.18)).frame(width: 52, height: 52)
                                    Text(String(personName.prefix(1)).uppercased())
                                        .font(.system(size: 22, weight: .bold)).foregroundColor(avatarColor)
                                }
                            }
                            VStack(alignment: .leading, spacing: 3) {
                                Text(personName).font(.system(size: 17, weight: .bold)).foregroundColor(.white)
                                if let p = phone { Text(p).font(.system(size: 12)).foregroundColor(.textSecondary) }
                                if hasUPI {
                                    HStack(spacing: 6) {
                                        Text(effectiveUPI).font(.system(size: 11)).foregroundColor(.accent1)
                                            .lineLimit(1).truncationMode(.middle)
                                        if upiFromAccount {
                                            Text("✓ Verified")
                                                .font(.system(size: 9, weight: .bold))
                                                .foregroundColor(.incomeGreen)
                                                .padding(.horizontal, 6).padding(.vertical, 2)
                                                .background(Capsule().fill(Color.incomeGreen.opacity(0.15)))
                                        }
                                    }
                                }
                            }
                            Spacer()
                        }
                        .padding(16).glassCard(radius: 18)

                        // ── Amount ────────────────────────────────────
                        VStack(alignment: .leading, spacing: 8) {
                            Text("AMOUNT").font(.system(size: 10, weight: .bold)).foregroundColor(.textSecondary).tracking(1.5)
                            HStack(alignment: .firstTextBaseline, spacing: 4) {
                                Text(store.currencySymbol).font(.system(size: 26, weight: .bold)).foregroundColor(.accent1)
                                TextField("0", text: $amountStr)
                                    .focused($amountFocused).keyboardType(.decimalPad)
                                    .font(.system(size: 38, weight: .bold, design: .rounded)).foregroundColor(.white)
                            }
                            .padding(16).glassCard()
                            if prefilledAmount > 0 {
                                Button { amountStr = String(format: "%.0f", prefilledAmount) } label: {
                                    Text("Fill \(store.formatted(prefilledAmount)) (outstanding)")
                                        .font(.system(size: 11, weight: .semibold)).foregroundColor(.accent1)
                                        .padding(.horizontal, 12).padding(.vertical, 6)
                                        .background(Capsule().fill(Color.accent1.opacity(0.12)))
                                }
                            }
                        }

                        // ── Note ──────────────────────────────────────
                        VStack(alignment: .leading, spacing: 8) {
                            Text("NOTE").font(.system(size: 10, weight: .bold)).foregroundColor(.textSecondary).tracking(1.5)
                            HStack {
                                Image(systemName: "pencil").foregroundColor(.textSecondary).font(.system(size: 14))
                                TextField("What's this for?", text: $note).font(.system(size: 15)).foregroundColor(.white)
                            }
                            .padding(14).glassCard()
                        }

                        // ── Category ─────────────────────────────────
                        VStack(alignment: .leading, spacing: 10) {
                            Text("CATEGORY").font(.system(size: 10, weight: .bold)).foregroundColor(.textSecondary).tracking(1.5)
                            ScrollView(.horizontal, showsIndicators: false) {
                                HStack(spacing: 8) {
                                    ForEach(store.categories) { cat in
                                        Button { selectedCat = cat } label: {
                                            HStack(spacing: 6) {
                                                Text(cat.emoji).font(.system(size: 14))
                                                Text(cat.name.components(separatedBy: " ").first ?? cat.name)
                                                    .font(.system(size: 12, weight: .semibold))
                                            }
                                            .foregroundColor(selectedCat?.id == cat.id ? .white : .textSecondary)
                                            .padding(.horizontal, 12).padding(.vertical, 8)
                                            .background(Capsule().fill(selectedCat?.id == cat.id ? cat.color.opacity(0.7) : Color.bgCard))
                                            .overlay(Capsule().stroke(selectedCat?.id == cat.id ? cat.color.opacity(0.4) : Color.white.opacity(0.06), lineWidth: 1))
                                        }
                                    }
                                }
                                .padding(.horizontal, 2)
                            }
                        }

                        // ── Record as Lend toggle ─────────────────────
                        HStack(spacing: 14) {
                            ZStack {
                                RoundedRectangle(cornerRadius: 10).fill(Color.incomeGreen.opacity(0.15)).frame(width: 40, height: 40)
                                Image(systemName: "arrow.up.arrow.down.circle.fill").foregroundColor(.incomeGreen).font(.system(size: 20))
                            }
                            VStack(alignment: .leading, spacing: 2) {
                                Text("Record as Lend").font(.system(size: 14, weight: .semibold)).foregroundColor(.white)
                                Text(addAsLend ? "Saved to your Lends list" : "Only saved in History").font(.system(size: 11)).foregroundColor(.textSecondary)
                            }
                            Spacer()
                            Toggle("", isOn: $addAsLend).toggleStyle(SwitchToggleStyle(tint: .incomeGreen)).labelsHidden()
                        }
                        .padding(14).glassCard(radius: 16)

                        // ── UPI Section ───────────────────────────────
                        if hasUPI {
                            VStack(alignment: .leading, spacing: 10) {
                                Text("PAY VIA").font(.system(size: 10, weight: .bold)).foregroundColor(.textSecondary).tracking(1.5)
                                ScrollView(.horizontal, showsIndicators: false) {
                                    HStack(spacing: 10) {
                                        ForEach(UPIApp.allCases) { app in
                                            Button { selectedApp = app } label: {
                                                VStack(spacing: 6) {
                                                    if app.logoName.isEmpty {
                                                        Image(systemName: "indianrupeesign.circle")
                                                            .resizable().scaledToFit().frame(width: 36, height: 36).foregroundColor(app.color)
                                                    } else {
                                                        Image(app.logoName).resizable().scaledToFit()
                                                            .frame(width: 36, height: 36).clipShape(RoundedRectangle(cornerRadius: 8))
                                                    }
                                                    Text(app.rawValue.components(separatedBy: ":").first ?? app.rawValue)
                                                        .font(.system(size: 10, weight: .semibold))
                                                        .foregroundColor(selectedApp == app ? app.color : .textSecondary)
                                                }
                                                .padding(10)
                                                .background(RoundedRectangle(cornerRadius: 14).fill(selectedApp == app ? app.color.opacity(0.15) : Color.bgCard))
                                                .overlay(RoundedRectangle(cornerRadius: 14).stroke(selectedApp == app ? app.color.opacity(0.4) : Color.white.opacity(0.05), lineWidth: 1))
                                            }
                                        }
                                    }.padding(.horizontal, 2)
                                }
                            }
                        } else {
                            // No UPI yet — scan or skip
                            VStack(spacing: 10) {
                                Text("UPI ID REQUIRED TO PAY")
                                    .font(.system(size: 10, weight: .bold)).foregroundColor(.textSecondary).tracking(1.5)
                                    .frame(maxWidth: .infinity, alignment: .leading)
                                HStack(spacing: 10) {
                                    Button { showScanner = true } label: {
                                        HStack(spacing: 8) {
                                            Image(systemName: "qrcode.viewfinder").font(.system(size: 16))
                                            Text("Scan QR").font(.system(size: 13, weight: .semibold))
                                        }
                                        .foregroundColor(.white).frame(maxWidth: .infinity).frame(height: 44)
                                        .background(RoundedRectangle(cornerRadius: 12).fill(Color.accent1.opacity(0.8)))
                                    }
                                    Button { showScanner = true } label: {
                                        HStack(spacing: 8) {
                                            Image(systemName: "keyboard").font(.system(size: 16))
                                            Text("Skip Pay").font(.system(size: 13, weight: .semibold))
                                        }
                                        .foregroundColor(.textSecondary).frame(maxWidth: .infinity).frame(height: 44)
                                        .background(RoundedRectangle(cornerRadius: 12).fill(Color.bgCard))
                                    }
                                }
                            }
                        }

                        // ── Pay Button ────────────────────────────────
                        Button { handlePay() } label: {
                            HStack(spacing: 10) {
                                Image(systemName: hasUPI ? "paperplane.fill" : "square.and.pencil")
                                Text(hasUPI ? "Open \(selectedApp.rawValue.components(separatedBy: ":").first ?? selectedApp.rawValue)" : "Log Payment Only")
                                    .font(.system(size: 17, weight: .bold))
                            }
                            .foregroundColor(.white).frame(maxWidth: .infinity).frame(height: 56)
                            .background(canProceed ? LinearGradient.accentGradient : LinearGradient(colors: [.bgCard], startPoint: .leading, endPoint: .trailing))
                            .clipShape(RoundedRectangle(cornerRadius: 16))
                            .shadow(color: Color.accent1.opacity(canProceed ? 0.35 : 0), radius: 12, y: 4)
                        }
                        .disabled(!canProceed)

                        Color.clear.frame(height: 20)
                    }
                    .padding(.horizontal, 20).padding(.top, 20)
                }
            }
            .navigationTitle("Send Money")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("Cancel") { dismiss() }.foregroundColor(.accent1)
                }
                ToolbarItemGroup(placement: .keyboard) {
                    Spacer()
                    Button("Done") {
                        UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
                    }.foregroundColor(.accent1)
                }
            }
            .onAppear {
                if selectedCat == nil { selectedCat = store.categories.first }
                amountFocused = true
                // Pre-fill amount if settling a balance
                if prefilledAmount > 0 && amountStr.isEmpty {
                    amountStr = String(format: "%.0f", prefilledAmount)
                }
                // prefilledUPI from account is used via effectiveUPI computed var — no state needed
            }
        }

        .sheet(isPresented: $showScanner) {
            QRScannerView { qrData in
                upiId = qrData.upiId
                // Auto-save for this person
                store.saveUPI(name: personName, upi: qrData.upiId)
                if !qrData.name.isEmpty && note.isEmpty { note = qrData.name }
                if let amt = qrData.amount, amt > 0, amountStr.isEmpty {
                    amountStr = String(format: "%.0f", amt)
                }
            }
        }
    }

    private func handlePay() {
        let cat = selectedCat ?? store.categories.first ?? AppCategory(name: "Other", emoji: "💰", colorHex: "#8E8E93")
        let upi = effectiveUPI

        // 1. Log as transaction
        let tx = Transaction(
            amount: amount,
            recipientName: personName,
            upiId: upi,
            note: note.isEmpty ? (addAsLend ? "Lend to \(firstName)" : "Payment to \(firstName)") : note,
            categoryId: cat.id,
            categoryName: cat.name,
            categoryEmoji: cat.emoji,
            categoryHex: cat.colorHex,
            date: Date(),
            upiAppUsed: hasUPI ? selectedApp : nil
        )
        store.addTransaction(tx)

        // 2. Always save as a quick-pay contact for the Payments view
        store.addPaymentContact(name: personName, phone: phone)

        // 3. Optionally add as lend record
        if addAsLend {
            let lb = LendBorrow(
                type: .lent,
                personName: personName,
                contactPhone: phone,
                amount: amount,
                note: note.isEmpty ? "Lend" : note,
                date: Date()
            )
            store.addLendBorrow(lb)
        }

        // 4. If UPI available: copy + open app
        if hasUPI {
            UIPasteboard.general.string = upi
            if let url = selectedApp.makeURL() {
                UIApplication.shared.open(url)
            }
            onPaid()  // triggers clipboard hint banner
        }

        dismiss()
    }
}

// MARK: - Entry Section

struct EntrySection: View {
    let title       : String
    let entries     : [LendBorrow]
    let accentColor : Color
    var onPartialPay : (LendBorrow) -> Void
    var onMarkPaid   : (LendBorrow) -> Void
    var onEdit       : (LendBorrow) -> Void
    var onDelete     : (LendBorrow) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(title)
                .font(.system(size: 10, weight: .bold)).foregroundColor(.textSecondary).tracking(1.2)
                .padding(.horizontal, 24)

            ForEach(entries) { lb in
                LBEntryCard(
                    entry: lb, accentColor: accentColor,
                    onPartialPay: { onPartialPay(lb) },
                    onMarkPaid: { onMarkPaid(lb) },
                    onEdit: { onEdit(lb) },
                    onDelete: { onDelete(lb) }
                )
                .padding(.horizontal, 20)
            }
        }
    }
}

// MARK: - Entry Card

struct LBEntryCard: View {
    let entry       : LendBorrow
    let accentColor : Color
    let onPartialPay: () -> Void
    let onMarkPaid  : () -> Void
    let onEdit      : () -> Void
    let onDelete    : () -> Void
    @EnvironmentObject var store: CloudDataStore

    var body: some View {
        VStack(spacing: 12) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 4) {
                    HStack(spacing: 8) {
                        if entry.isPaid {
                            Image(systemName: "checkmark.circle.fill").foregroundColor(.incomeGreen)
                        } else if entry.hasPartialPayment {
                            Image(systemName: "circle.lefthalf.filled").foregroundColor(.catOrange)
                        } else {
                            Image(systemName: "circle").foregroundColor(.textTertiary)
                        }
                        Text(entry.note.isEmpty ? "No note" : entry.note)
                            .font(.system(size: 14, weight: .medium)).foregroundColor(.white)
                            .lineLimit(1)
                        // Split badge — shown for any entry created from a bill split
                        if entry.splitGroupId != nil {
                            Text("Split")
                                .font(.system(size: 9, weight: .bold))
                                .foregroundColor(.catBlue)
                                .padding(.horizontal, 6).padding(.vertical, 2)
                                .background(Capsule().fill(Color.catBlue.opacity(0.18)))
                        }
                    }
                    Text(entry.date, style: .date)
                        .font(.system(size: 11)).foregroundColor(.textSecondary)
                    if let due = entry.dueDate, !entry.isPaid {
                        Label {
                            Text(due, style: .date).foregroundColor(entry.isOverdue ? .expenseRed : entry.isDueSoon ? .catOrange : .textSecondary)
        .font(.system(size: 11))
                        } icon: {
                            Image(systemName: "calendar").foregroundColor(entry.isOverdue ? .expenseRed : .textSecondary)
                        }
                        .font(.system(size: 11))
                    }
                }
                Spacer()
                VStack(alignment: .trailing, spacing: 4) {
                    Text(store.formatted(entry.amount))
                        .font(.system(size: 16, weight: .bold)).foregroundColor(entry.isPaid ? .textSecondary : .white)
                    if entry.hasPartialPayment && !entry.isPaid {
                        Text("Remaining: \(store.formatted(entry.remainingAmount))")
                            .font(.system(size: 11)).foregroundColor(accentColor)
                    }
                    if entry.isPaid {
                        Text("PAID").font(.system(size: 9, weight: .bold)).foregroundColor(.incomeGreen).tracking(1)
                    }
                }
            }

            // Partial payment progress bar
            if entry.hasPartialPayment && !entry.isPaid {
                GeometryReader { g in
                    ZStack(alignment: .leading) {
                        RoundedRectangle(cornerRadius: 3).fill(Color.white.opacity(0.07)).frame(height: 4)
                        RoundedRectangle(cornerRadius: 3).fill(accentColor)
                            .frame(width: g.size.width * CGFloat(entry.paidFraction), height: 4)
                    }
                }.frame(height: 4)
            }

            // Action buttons (only for unpaid entries)
            if !entry.isPaid {
                HStack(spacing: 8) {
                    EntryActionBtn(label: "Pay Part", icon: "minus.circle") { onPartialPay() }
                    EntryActionBtn(label: "Mark Paid", icon: "checkmark.circle", tint: .incomeGreen) { onMarkPaid() }
                    EntryActionBtn(label: "Edit", icon: "pencil") { onEdit() }
                    EntryActionBtn(label: "Delete", icon: "trash", tint: .expenseRed) { onDelete() }
                }
            } else {
                HStack {
                    Spacer()
                    Button { onDelete() } label: {
                        Label("Remove", systemImage: "trash")
                            .font(.system(size: 11)).foregroundColor(.textSecondary)
                    }
                }
            }
        }
        .padding(16).glassCard(radius: 18)
    }
}

struct EntryActionBtn: View {
    let label: String; let icon: String; var tint: Color = .textSecondary
    let action: () -> Void
    var body: some View {
        Button(action: action) {
            VStack(spacing: 2) {
                Image(systemName: icon).font(.system(size: 14))
                Text(label).font(.system(size: 9, weight: .medium))
            }
            .foregroundColor(tint)
            .frame(maxWidth: .infinity).frame(height: 44)
            .background(RoundedRectangle(cornerRadius: 10).fill(tint.opacity(0.1)))
        }
    }
}

struct AddEntryButton: View {
    let label: String; let icon: String; let color: Color; let action: () -> Void
    var body: some View {
        Button(action: action) {
            HStack(spacing: 8) {
                Image(systemName: icon).font(.system(size: 16))
                Text(label).font(.system(size: 14, weight: .semibold))
            }
            .foregroundColor(color)
            .frame(maxWidth: .infinity).frame(height: 48)
            .background(RoundedRectangle(cornerRadius: 14).fill(color.opacity(0.1))
                .overlay(RoundedRectangle(cornerRadius: 14).stroke(color.opacity(0.2), lineWidth: 1)))
        }
    }
}

// MARK: - Partial Payment Sheet

struct PartialPaySheet: View {
    let entry: LendBorrow
    @EnvironmentObject var store: CloudDataStore
    @Environment(\.dismiss) var dismiss
    @State private var amountStr = ""
    @FocusState private var focused: Bool

    var payAmount: Double { Double(amountStr) ?? 0 }
    var canPay: Bool { payAmount > 0 && payAmount <= entry.remainingAmount }

    var body: some View {
        NavigationView {
            ZStack {
                Color.bgPrimary.ignoresSafeArea()
                VStack(spacing: 24) {
                    // Summary card
                    VStack(spacing: 10) {
                        Text("OUTSTANDING").font(.system(size: 10, weight: .bold)).foregroundColor(.textSecondary).tracking(1.2)
                        Text(store.formatted(entry.remainingAmount))
                            .font(.system(size: 36, weight: .bold, design: .rounded)).foregroundColor(.white)
                        if entry.hasPartialPayment {
                            Text("Already paid: \(store.formatted(entry.paidAmount))")
                                .font(.system(size: 12)).foregroundColor(.textSecondary)
                        }
                    }
                    .frame(maxWidth: .infinity).padding(24).glassCard(radius: 20)

                    // Amount entry
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Payment Amount").font(.system(size: 12, weight: .semibold)).foregroundColor(.textSecondary)
                        HStack {
                            Text(store.currencySymbol).foregroundColor(.accent1).font(.system(size: 20, weight: .bold))
                            TextField("0", text: $amountStr)
                                .focused($focused)
                                .keyboardType(.decimalPad)
                                .font(.system(size: 28, weight: .bold)).foregroundColor(.white)
                        }
                        .padding(16).glassCard()

                        // Quick amounts
                        HStack(spacing: 8) {
                            let half = entry.remainingAmount / 2
                            let full = entry.remainingAmount
                            ForEach([half, full], id: \.self) { amt in
                                Button {
                                    amountStr = String(format: "%.0f", amt)
                                } label: {
                                    Text(amt == full ? "Full (\(store.formatted(amt)))" : "Half (\(store.formatted(amt)))")
                                        .font(.system(size: 11, weight: .semibold)).foregroundColor(.accent1)
                                        .padding(.horizontal, 12).padding(.vertical, 8)
                                        .background(Capsule().fill(Color.accent1.opacity(0.12)))
                                }
                            }
                            Spacer()
                        }
                    }

                    Button {
                        store.addPartialPayment(id: entry.id, paidNow: payAmount)
                        dismiss()
                    } label: {
                        Text("Record \(store.formatted(payAmount)) Payment")
                            .font(.system(size: 16, weight: .bold)).foregroundColor(.white)
                            .frame(maxWidth: .infinity).frame(height: 52)
                            .background(canPay ? LinearGradient.accentGradient : LinearGradient(colors: [Color.bgCard], startPoint: .leading, endPoint: .trailing))
                            .clipShape(RoundedRectangle(cornerRadius: 16))
                    }
                    .disabled(!canPay)

                    Spacer()
                }
                .padding(20)
            }
            .navigationTitle("Partial Payment")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("Cancel") { dismiss() }.foregroundColor(.accent1)
                }
                ToolbarItemGroup(placement: .keyboard) {
                    Spacer()
                    Button("Done") {
                        focused = false
                        UIApplication.shared.hideKeyboard()
                    }.foregroundColor(.accent1)
                }
            }
            .onAppear { focused = true }
        }
    }
}

// MARK: - Edit Entry Sheet

struct EditEntrySheet: View {
    let entry: LendBorrow
    @EnvironmentObject var store: CloudDataStore
    @Environment(\.dismiss) var dismiss
    @FocusState private var focused: Bool

    @State private var amountStr = ""
    @State private var note      = ""
    @State private var hasDue    = false
    @State private var dueDate   = Date()

    var body: some View {
        NavigationView {
            ZStack {
                Color.bgPrimary.ignoresSafeArea()
                VStack(spacing: 16) {
                    // Amount
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Amount").font(.system(size: 12, weight: .semibold)).foregroundColor(.textSecondary)
                        HStack {
                            Text(store.currencySymbol).foregroundColor(.accent1).font(.system(size: 20, weight: .bold))
                            TextField("0", text: $amountStr).focused($focused)
                                .keyboardType(.decimalPad)
                                .font(.system(size: 24, weight: .bold)).foregroundColor(.white)
                        }
                        .padding(14).glassCard()
                    }

                    // Note
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Note").font(.system(size: 12, weight: .semibold)).foregroundColor(.textSecondary)
                        TextField("Reason", text: $note)
                            .font(.system(size: 15)).foregroundColor(.white)
                            .padding(14).glassCard()
                    }

                    // Due date
                    Toggle(isOn: $hasDue) {
                        Label("Due Date", systemImage: "calendar").font(.system(size: 14)).foregroundColor(.white)
                    }.toggleStyle(SwitchToggleStyle(tint: .accent1))

                    if hasDue {
                        DatePicker("", selection: $dueDate, displayedComponents: .date)
                            .datePickerStyle(.compact).colorScheme(.dark)
                    }

                    Button("Save Changes") {
                        var updated = entry
                        updated.amount  = Double(amountStr) ?? entry.amount
                        updated.note    = note
                        updated.dueDate = hasDue ? dueDate : nil
                        store.updateLendBorrow(updated)
                        dismiss()
                    }
                    .font(.system(size: 16, weight: .bold)).foregroundColor(.white)
                    .frame(maxWidth: .infinity).frame(height: 52)
                    .background(LinearGradient.accentGradient)
                    .clipShape(RoundedRectangle(cornerRadius: 16))

                    Spacer()
                }
                .padding(20)
            }
            .navigationTitle("Edit Entry")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("Cancel") { dismiss() }.foregroundColor(.accent1)
                }
                ToolbarItemGroup(placement: .keyboard) {
                    Spacer()
                    Button("Done") {
                        focused = false
                        UIApplication.shared.hideKeyboard()
                    }.foregroundColor(.accent1)
                }
            }
            .onAppear {
                amountStr = String(format: "%.0f", entry.amount)
                note      = entry.note
                if let d = entry.dueDate { hasDue = true; dueDate = d }
            }
        }
    }
}

// MARK: - Split Ledger Row

struct SplitLedgerRow: View {
    let ledger: SharedLedger
    let iAmSender: Bool        // true = I paid and they owe me
    let currencySymbol: String
    let onSettle: () -> Void

    private var fmt: DateFormatter {
        let df = DateFormatter()
        df.dateStyle = .medium
        df.timeStyle = .none
        return df
    }

    var body: some View {
        HStack(spacing: 14) {
            // Direction indicator
            ZStack {
                Circle()
                    .fill(iAmSender ? Color.incomeGreen.opacity(0.15) : Color.expenseRed.opacity(0.15))
                    .frame(width: 42, height: 42)
                Image(systemName: iAmSender ? "arrow.up.right" : "arrow.down.left")
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundColor(iAmSender ? .incomeGreen : .expenseRed)
            }

            VStack(alignment: .leading, spacing: 3) {
                if let group = ledger.groupName, !group.isEmpty {
                    Text(group)
                        .font(.system(size: 14, weight: .semibold)).foregroundColor(.white)
                }
                Text(ledger.note.isEmpty ? "Split expense" : ledger.note)
                    .font(.system(size: 13)).foregroundColor(.textSecondary)
                    .lineLimit(1)
                Text(fmt.string(from: ledger.date))
                    .font(.system(size: 11)).foregroundColor(.textTertiary)
            }

            Spacer()

            VStack(alignment: .trailing, spacing: 4) {
                Text("\(iAmSender ? "+" : "-")\(currencySymbol)\(String(format: "%.0f", ledger.amount))")
                    .font(.system(size: 15, weight: .bold))
                    .foregroundColor(iAmSender ? .incomeGreen : .expenseRed)

                if ledger.isPaid {
                    Text("Settled ✓")
                        .font(.system(size: 11, weight: .medium)).foregroundColor(.incomeGreen)
                } else if iAmSender {
                    Button("Settle") { onSettle() }
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundColor(.white)
                        .padding(.horizontal, 10).padding(.vertical, 4)
                        .background(Color.accent1.opacity(0.8))
                        .clipShape(Capsule())
                }
            }
        }
        .padding(16)
        .glassCard(radius: 16)
        .opacity(ledger.isPaid ? 0.6 : 1.0)
    }
}

#Preview {
    PersonDetailView(personName: "Rahul Sharma", phone: "+91 98765 43210")
        .environmentObject(CloudDataStore())
        .preferredColorScheme(.dark)
}
