import SwiftUI

// Convenience alias so existing code needs no further changes
typealias PersonGroup = CloudDataStore.PersonGroup


// MARK: - Main Lend/Borrow View

struct LendBorrowView: View {
    @EnvironmentObject var store: CloudDataStore

    @State private var showAddSheet          = false
    @State private var showSplitExpense      = false
    @State private var showContactPick       = false
    @State private var selectedPerson        : PersonGroup? = nil
    @State private var prefilledName         = ""
    @State private var prefilledPhone        : String? = nil
    @State private var prefilledImageData    : Data? = nil
    @State private var prefilledType         : LendBorrowType = .lent

    // Pay to contact flow
    @State private var showPayContactPicker  = false
    @State private var showEditName          = false   // name-edit step
    @State private var showSendMoneyDirect   = false
    @State private var sendMoneyName         = ""
    @State private var sendMoneyPhone        : String? = nil
    @State private var showClipboardHint     = false

    // Grand totals (outstanding only)
    var grandTotalLent    : Double { store.lendBorrows.filter { $0.type == .lent     && !$0.isPaid }.reduce(0) { $0 + $1.remainingAmount } }
    var grandTotalBorrowed: Double { store.lendBorrows.filter { $0.type == .borrowed && !$0.isPaid }.reduce(0) { $0 + $1.remainingAmount } }

    // Group by person name — delegated to CloudDataStore
    var personGroups: [PersonGroup] { store.personGroups }
    var activeGroups: [PersonGroup] { store.personGroups.filter { $0.totalLentOut > 0 || $0.totalBorrowed > 0 } }
    var settledGroups: [PersonGroup] { store.personGroups.filter { $0.totalLentOut == 0 && $0.totalBorrowed == 0 } }

    var body: some View {
        VStack(spacing: 0) {
            // ── Header ──────────────────────────────────────────
            HStack(alignment: .bottom) {
                Text("Payments")
                    .font(.system(size: 30, weight: .bold)).foregroundColor(.white)
                Spacer()
                // Pay directly to a contact
                Button {
                    showPayContactPicker = true
                } label: {
                    Image(systemName: "person.crop.circle.badge.plus")
                        .font(.system(size: 16, weight: .semibold)).foregroundColor(.white)
                        .frame(width: 36, height: 36)
                        .background(Color.accentBlue)
                        .clipShape(Circle())
                }
                // Split a bill
                Button {
                    showSplitExpense = true
                } label: {
                    Image(systemName: "square.split.diagonal.2x2")
                        .font(.system(size: 16, weight: .semibold)).foregroundColor(.white)
                        .frame(width: 36, height: 36)
                        .background(Color.catPurple)
                        .clipShape(Circle())
                }
                // Add lend/borrow record
                Button {
                    showAddSheet = true
                } label: {
                    Image(systemName: "plus")
                        .font(.system(size: 16, weight: .semibold)).foregroundColor(.white)
                        .frame(width: 36, height: 36)
                        .background(Color.accent1)
                        .clipShape(Circle())
                }
            }
            .padding(.horizontal, 24).padding(.top, 60).padding(.bottom, 16)

            // ── Summary Bar ─────────────────────────────────────
            HStack(spacing: 12) {
                SummaryPill(label: "LENT OUT",  amount: store.formatted(grandTotalLent),      color: .incomeGreen)
                SummaryPill(label: "BORROWED", amount: store.formatted(grandTotalBorrowed), color: .expenseRed)
            }
            .padding(.horizontal, 20).padding(.bottom, 20)

            // ── Person List ──────────────────────────────────────
            ScrollView(showsIndicators: false) {
                VStack(spacing: 0) {
                    if personGroups.isEmpty {
                        EmptyLendView { showAddSheet = true }
                    } else {
                        // Outstanding Lends/Borrows
                        if !activeGroups.isEmpty {
                            SectionLabel("OUTSTANDING")
                            ForEach(activeGroups) { group in
                                PersonCard(group: group) { selectedPerson = group }
                                    .padding(.horizontal, 20).padding(.bottom, 10)
                            }
                        }
                        // Settled or Contacts with only standard transfers
                        if !settledGroups.isEmpty {
                            SectionLabel("OTHER CONTACTS")
                            ForEach(settledGroups) { group in
                                PersonCard(group: group) { selectedPerson = group }
                                    .padding(.horizontal, 20).padding(.bottom, 10)
                            }
                        }
                    }
                    Color.clear.frame(height: 20)
                }
            }
        }
        // Pay-to-contact picker → name edit → SendMoneySheet
        .sheet(isPresented: $showPayContactPicker) {
            ContactPicker { name, phone, imageData in
                sendMoneyName  = name
                sendMoneyPhone = phone
                if let data = imageData { store.saveAvatar(name: name, data: data) }
                showPayContactPicker = false
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.35) {
                    showEditName = true   // show name-edit step first
                }
            }
        }
        // Edit name before opening SendMoneySheet
        .sheet(isPresented: $showEditName) {
            EditContactNameSheet(name: $sendMoneyName) {
                showEditName = false
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.35) {
                    showSendMoneyDirect = true
                }
            }
        }
        // Direct SendMoneySheet to contact
        .sheet(isPresented: $showSendMoneyDirect) {
            SendMoneySheet(
                personName: sendMoneyName,
                phone: sendMoneyPhone,
                prefilledAmount: 0
            ) {
                withAnimation { showClipboardHint = true }
                DispatchQueue.main.asyncAfter(deadline: .now() + 3) {
                    withAnimation { showClipboardHint = false }
                }
            }
            .environmentObject(store)
        }
        // Contact picker for Add lend entry
        .sheet(isPresented: $showContactPick) {
            ContactPicker { name, phone, imageData in
                prefilledName  = name
                prefilledPhone = phone
                prefilledImageData = imageData
                showContactPick = false
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.35) { showAddSheet = true }
            }
        }
        // Add entry sheet
        .sheet(isPresented: $showAddSheet, onDismiss: {
            prefilledName = ""; prefilledPhone = nil; prefilledImageData = nil
        }) {
            LBAddSheet(
                prefilledName: prefilledName,
                prefilledPhone: prefilledPhone,
                prefilledImageData: prefilledImageData,
                prefilledType: prefilledType,
                onContactPick: {
                    showAddSheet = false
                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.35) { showContactPick = true }
                }
            )
        }
        // Person detail sheet
        .sheet(item: $selectedPerson) { group in
            PersonDetailView(personName: group.name, phone: group.phone)
                .environmentObject(store)
        }
        .sheet(isPresented: $showSplitExpense) {
            SplitExpenseView()
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
    }
}

// MARK: - Edit Contact Name Sheet

struct EditContactNameSheet: View {
    @Binding var name: String
    let onConfirm: () -> Void

    @Environment(\.dismiss) var dismiss
    @FocusState private var focused: Bool

    var body: some View {
        NavigationView {
            ZStack {
                Color.bgPrimary.ignoresSafeArea()
                VStack(spacing: 24) {
                    // Icon
                    ZStack {
                        Circle().fill(Color.accentBlue.opacity(0.15)).frame(width: 72, height: 72)
                        Image(systemName: "person.crop.circle").font(.system(size: 34)).foregroundColor(.accentBlue)
                    }

                    VStack(spacing: 8) {
                        Text("Confirm Name").font(.system(size: 20, weight: .bold)).foregroundColor(.white)
                        Text("Edit how this contact's name\nappears in your payment history")
                            .font(.system(size: 13)).foregroundColor(.textSecondary)
                            .multilineTextAlignment(.center)
                    }

                    // Name field
                    HStack(spacing: 10) {
                        Image(systemName: "person.fill").foregroundColor(.textSecondary)
                        TextField("Contact name", text: $name)
                            .focused($focused)
                            .font(.system(size: 16)).foregroundColor(.white)
                            .autocapitalization(.words)
                    }
                    .padding(16).glassCard(radius: 14)

                    // Confirm button
                    Button {
                        UIApplication.shared.hideKeyboard()
                        onConfirm()
                    } label: {
                        Text("Continue →")
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
                .padding(28)
            }
            .navigationTitle("Edit Contact Name")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("Cancel") { dismiss() }.foregroundColor(.accent1)
                }
                ToolbarItemGroup(placement: .keyboard) {
                    Spacer()
                    Button("Done") { focused = false; UIApplication.shared.hideKeyboard() }
                        .foregroundColor(.accent1)
                }
            }
            .onAppear { focused = true }
        }
    }
}

// MARK: - Person Card

struct PersonCard: View {
    let group: PersonGroup
    let onTap: () -> Void

    @EnvironmentObject var store: CloudDataStore

    var body: some View {
        Button(action: onTap) {
            HStack(spacing: 14) {
                // Avatar
                ZStack {
                    if let data = store.savedAvatars[group.name], let uiImage = UIImage(data: data) {
                        Image(uiImage: uiImage)
                            .resizable()
                            .scaledToFill()
                            .frame(width: 48, height: 48)
                            .clipShape(Circle())
                    } else {
                        Circle().fill(group.color.opacity(0.2)).frame(width: 48, height: 48)
                        Text(group.initial)
                            .font(.system(size: 18, weight: .bold)).foregroundColor(group.color)
                    }
                }

                VStack(alignment: .leading, spacing: 4) {
                    Text(group.name).font(.system(size: 15, weight: .semibold)).foregroundColor(.white)
                    if let phone = group.phone {
                        Text(phone).font(.system(size: 12)).foregroundColor(.textSecondary)
                    }
                    // Pills
                    HStack(spacing: 6) {
                        if group.totalLentOut > 0 {
                            MiniPill(text: "Lent \(fmt(group.totalLentOut))", color: .incomeGreen)
                        }
                        if group.totalBorrowed > 0 {
                            MiniPill(text: "Owe \(fmt(group.totalBorrowed))", color: .expenseRed)
                        }
                        if group.totalLentOut == 0 && group.totalBorrowed == 0 {
                            MiniPill(text: group.entries.isEmpty ? "No active lends" : "Settled ✓", color: .textSecondary)
                        }
                    }
                }

                Spacer()

                // Net indicator
                let net = group.totalLentOut - group.totalBorrowed
                if net != 0 {
                    VStack(alignment: .trailing, spacing: 2) {
                        Text(net > 0 ? "+" : "")
                            .font(.system(size: 10)).foregroundColor(net > 0 ? .incomeGreen : .expenseRed)
                            + Text(fmt(abs(net)))
                            .font(.system(size: 14, weight: .bold)).foregroundColor(net > 0 ? .incomeGreen : .expenseRed)
                        Text(net > 0 ? "they owe you" : "you owe them")
                            .font(.system(size: 9)).foregroundColor(.textSecondary)
                    }
                }

                Image(systemName: "chevron.right")
                    .font(.system(size: 12, weight: .medium)).foregroundColor(.textTertiary)
            }
            .padding(16).glassCard(radius: 18)
        }
    }
}

// MARK: - Add Sheet

struct LBAddSheet: View {
    @EnvironmentObject var store: CloudDataStore
    @Environment(\.dismiss) var dismiss
    @FocusState private var focusedField: LBField?

    var prefilledName  : String        = ""
    var prefilledPhone : String?       = nil
    var prefilledImageData : Data?     = nil
    var prefilledType  : LendBorrowType = .lent
    var onContactPick  : (() -> Void)? = nil

    @State private var personName = ""
    @State private var phone      : String? = nil
    @State private var amountStr  = ""
    @State private var note       = ""
    @State private var type       : LendBorrowType = .lent
    @State private var hasDueDate = false
    @State private var dueDate    = Calendar.current.date(byAdding: .day, value: 30, to: Date())!

    enum LBField { case name, amount, note }

    var canSave: Bool { !personName.isEmpty && (Double(amountStr) ?? 0) > 0 }

    var body: some View {
        NavigationView {
            ZStack {
                Color.bgPrimary.ignoresSafeArea()
                ScrollView {
                    VStack(spacing: 16) {

                        // Type segmented
                        Picker("Type", selection: $type) {
                            Text("I Lent").tag(LendBorrowType.lent)
                            Text("I Borrowed").tag(LendBorrowType.borrowed)
                        }
                        .pickerStyle(.segmented)
                        .padding(.horizontal, 20).padding(.top, 8)

                        // Person
                        VStack(alignment: .leading, spacing: 8) {
                            Label("Person", systemImage: "person").font(.system(size: 12, weight: .semibold)).foregroundColor(.textSecondary)
                            HStack(spacing: 10) {
                                TextField("Name", text: $personName)
                                    .focused($focusedField, equals: .name)
                                    .font(.system(size: 15)).foregroundColor(.white)
                                if let pick = onContactPick {
                                    Button(action: pick) {
                                        Image(systemName: "person.crop.circle.badge.plus")
                                            .font(.system(size: 20)).foregroundColor(.accent1)
                                    }
                                }
                            }
                            .padding(14).glassCard()
                        }
                        .padding(.horizontal, 20)

                        // Amount
                        VStack(alignment: .leading, spacing: 8) {
                            Label("Amount", systemImage: "indianrupeesign").font(.system(size: 12, weight: .semibold)).foregroundColor(.textSecondary)
                            HStack {
                                Text(store.currencySymbol).foregroundColor(.accent1).font(.system(size: 18, weight: .bold))
                                TextField("0", text: $amountStr)
                                    .focused($focusedField, equals: .amount)
                                    .keyboardType(.decimalPad)
                                    .font(.system(size: 22, weight: .bold)).foregroundColor(.white)
                            }
                            .padding(14).glassCard()
                        }
                        .padding(.horizontal, 20)

                        // Note
                        VStack(alignment: .leading, spacing: 8) {
                            Label("Note", systemImage: "note.text").font(.system(size: 12, weight: .semibold)).foregroundColor(.textSecondary)
                            TextField("Reason (concert, emergency…)", text: $note)
                                .focused($focusedField, equals: .note)
                                .font(.system(size: 15)).foregroundColor(.white)
                                .padding(14).glassCard()
                        }
                        .padding(.horizontal, 20)

                        // Due date
                        Toggle(isOn: $hasDueDate) {
                            Label("Set Due Date", systemImage: "calendar")
                                .font(.system(size: 14)).foregroundColor(.white)
                        }
                        .toggleStyle(SwitchToggleStyle(tint: .accent1))
                        .padding(.horizontal, 20)

                        if hasDueDate {
                            DatePicker("Due Date", selection: $dueDate, displayedComponents: .date)
                                .datePickerStyle(.compact)
                                .colorScheme(.dark)
                                .padding(.horizontal, 20)
                        }

                        // Save
                        Button {
                            let lb = LendBorrow(
                                type: type, personName: personName.trimmingCharacters(in: .whitespaces),
                                contactPhone: prefilledPhone ?? phone,
                                amount: Double(amountStr) ?? 0,
                                note: note, date: Date(),
                                dueDate: hasDueDate ? dueDate : nil
                            )
                            if let pd = prefilledImageData {
                                store.savedAvatars[lb.personName] = pd
                            }
                            store.addLendBorrow(lb)
                            dismiss()
                        } label: {
                            Text(type == .lent ? "Record Lend" : "Record Borrow")
                                .font(.system(size: 16, weight: .bold)).foregroundColor(.white)
                                .frame(maxWidth: .infinity).frame(height: 52)
                                .background(canSave ? LinearGradient.accentGradient : LinearGradient(colors: [Color.bgCard], startPoint: .leading, endPoint: .trailing))
                                .clipShape(RoundedRectangle(cornerRadius: 16))
                        }
                        .disabled(!canSave)
                        .padding(.horizontal, 20).padding(.bottom, 40)
                    }
                }
            }
            .navigationTitle(type == .lent ? "Lend Money" : "Borrow Money")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("Cancel") { dismiss() }.foregroundColor(.accent1)
                }
                ToolbarItemGroup(placement: .keyboard) {
                    Spacer()
                    Button("Done") {
                        focusedField = nil
                        UIApplication.shared.hideKeyboard()
                    }.foregroundColor(.accent1)
                }
            }
            .onAppear {
                personName = prefilledName
                phone      = prefilledPhone
                type       = prefilledType
            }
        }
    }
}

// MARK: - Helpers

struct SummaryPill: View {
    let label: String; let amount: String; let color: Color
    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(label).font(.system(size: 9, weight: .bold)).foregroundColor(.textSecondary).tracking(1.2)
            Text(amount).font(.system(size: 20, weight: .bold, design: .rounded)).foregroundColor(.white)
            Circle().fill(color).frame(width: 8, height: 8)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16).glassCard(radius: 18)
    }
}

struct MiniPill: View {
    let text: String; let color: Color
    var body: some View {
        Text(text).font(.system(size: 10, weight: .semibold)).foregroundColor(color)
            .padding(.horizontal, 8).padding(.vertical, 3)
            .background(Capsule().fill(color.opacity(0.12)))
    }
}

struct SectionLabel: View {
    let label: String
    init(_ label: String) { self.label = label }
    var body: some View {
        Text(label).font(.system(size: 10, weight: .bold)).foregroundColor(.textSecondary).tracking(1.2)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 24).padding(.bottom, 8)
    }
}

struct EmptyLendView: View {
    let onAdd: () -> Void
    var body: some View {
        VStack(spacing: 20) {
            Image(systemName: "person.2").font(.system(size: 52)).foregroundColor(.textSecondary.opacity(0.3))
            Text("No lend records yet").font(.system(size: 16, weight: .semibold)).foregroundColor(.white)
            Text("Track who owes you and who you owe")
                .font(.system(size: 13)).foregroundColor(.textSecondary).multilineTextAlignment(.center)
            Button("Add First Record", action: onAdd)
                .font(.system(size: 14, weight: .bold)).foregroundColor(.white)
                .padding(.horizontal, 24).padding(.vertical, 12)
                .background(RoundedRectangle(cornerRadius: 12).fill(Color.accent1))
        }
        .padding(.top, 80).padding(.horizontal, 40)
    }
}

#Preview {
    LendBorrowView().environmentObject(CloudDataStore()).preferredColorScheme(.dark)
}
