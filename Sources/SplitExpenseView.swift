import SwiftUI

// MARK: - SplitExpenseView
// Minimal UI for splitting a bill with selected friends

struct SplitExpenseView: View {
    @EnvironmentObject var store: CloudDataStore
    @Environment(\.dismiss) var dismiss

    @State private var amountStr = ""
    @State private var note = ""
    @State private var groupName = ""
    @State private var selectedFriends: Set<String> = [] // Set of phone numbers
    @FocusState private var focus: Field?

    enum Field { case amount, note, groupName }

    private var totalAmount: Double { Double(amountStr) ?? 0 }
    
    // We get friends who have a saved phone number
    private var friendsWithPhones: [(name: String, phone: String)] {
        store.savedContacts.compactMap { name, phone in
            guard let p = phone, !p.isEmpty else { return nil }
            return (name: name, phone: p)
        }.sorted(by: { $0.name < $1.name })
    }

    private var myShare: Double {
        let peopleCount = selectedFriends.count + 1 // friends + me
        return totalAmount / Double(peopleCount)
    }

    private var canSplit: Bool {
        totalAmount > 0 && !selectedFriends.isEmpty && !note.trimmingCharacters(in: .whitespaces).isEmpty
    }

    var body: some View {
        NavigationView {
            ZStack {
                Color.bgPrimary.ignoresSafeArea()

                ScrollView {
                    VStack(spacing: 24) {

                        // Amount Card
                        VStack(spacing: 8) {
                            Text("TOTAL AMOUNT")
                                .font(.system(size: 11, weight: .bold))
                                .foregroundColor(.textSecondary).tracking(1.5)
                            
                            HStack(alignment: .center, spacing: 6) {
                                Text(store.currencySymbol)
                                    .font(.system(size: 32, weight: .semibold))
                                    .foregroundColor(.accent1.opacity(0.8))
                                TextField("0", text: $amountStr)
                                    .focused($focus, equals: .amount)
                                    .keyboardType(.decimalPad)
                                    .font(.system(size: 56, weight: .bold, design: .rounded))
                                    .foregroundColor(.white)
                                    .multilineTextAlignment(.center)
                            }
                        }
                        .padding(.vertical, 24).padding(.horizontal, 20)
                        .background(Color.white.opacity(0.04))
                        .clipShape(RoundedRectangle(cornerRadius: 24))
                        .overlay(RoundedRectangle(cornerRadius: 24).stroke(Color.white.opacity(0.08), lineWidth: 1))
                        .padding(.horizontal, 20)

                        // Details Card
                        VStack(spacing: 0) {
                            detailRow(icon: "note.text", placeholder: "What was this for? (e.g. Dinner)", text: $note, field: .note)
                            Divider().background(Color.white.opacity(0.07)).padding(.horizontal, 16)
                            detailRow(icon: "tag.fill", placeholder: "Group Name (Optional)", text: $groupName, field: .groupName)
                        }
                        .glassCard(radius: 20)
                        .padding(.horizontal, 20)

                        // Split With
                        VStack(alignment: .leading, spacing: 12) {
                            Text("SPLIT WITH")
                                .font(.system(size: 11, weight: .bold))
                                .foregroundColor(.textSecondary).tracking(1.5)
                                .padding(.horizontal, 24)

                            if friendsWithPhones.isEmpty {
                                Text("Add contacts with phone numbers in the Payments tab to split bills.")
                                    .font(.system(size: 14))
                                    .foregroundColor(.textSecondary)
                                    .padding(.horizontal, 24)
                            } else {
                                LazyVGrid(columns: [GridItem(.adaptive(minimum: 140), spacing: 12)], spacing: 12) {
                                    ForEach(friendsWithPhones, id: \.phone) { friend in
                                        let isSel = selectedFriends.contains(friend.phone)
                                        Button {
                                            if isSel { selectedFriends.remove(friend.phone) }
                                            else { selectedFriends.insert(friend.phone) }
                                        } label: {
                                            HStack {
                                                Circle()
                                                    .fill(isSel ? Color.accent1 : Color.white.opacity(0.1))
                                                    .frame(width: 20, height: 20)
                                                    .overlay(Image(systemName: "checkmark").font(.system(size: 10, weight: .bold)).foregroundColor(.white).opacity(isSel ? 1 : 0))
                                                Text(friend.name)
                                                    .font(.system(size: 14, weight: .medium))
                                                    .foregroundColor(isSel ? .white : .textSecondary)
                                                    .lineLimit(1)
                                                Spacer(minLength: 0)
                                            }
                                            .padding(12)
                                            .background(isSel ? Color.accent1.opacity(0.15) : Color.white.opacity(0.04))
                                            .clipShape(RoundedRectangle(cornerRadius: 12))
                                            .overlay(RoundedRectangle(cornerRadius: 12).stroke(isSel ? Color.accent1 : Color.white.opacity(0.08), lineWidth: 1))
                                        }
                                    }
                                }
                                .padding(.horizontal, 20)
                            }
                        }

                        // Summary
                        if canSplit {
                            VStack(spacing: 8) {
                                Text("Split equally among \(selectedFriends.count + 1) people")
                                    .font(.system(size: 13))
                                    .foregroundColor(.textSecondary)
                                Text("Everyone pays \(store.formatted(myShare))")
                                    .font(.system(size: 18, weight: .bold))
                                    .foregroundColor(.white)
                            }
                            .padding(.top, 12)
                            .transition(.opacity)
                        }

                        // Split Action
                        Button {
                            handleSplit()
                        } label: {
                            Text("Split Bill")
                                .font(.system(size: 17, weight: .bold))
                                .foregroundColor(.white)
                                .frame(maxWidth: .infinity).frame(height: 58)
                                .background(canSplit ? LinearGradient.accentGradient : LinearGradient(colors: [Color.bgCardAlt], startPoint: .leading, endPoint: .trailing))
                                .clipShape(RoundedRectangle(cornerRadius: 20))
                                .shadow(color: canSplit ? Color.accent1.opacity(0.4) : .clear, radius: 12, y: 6)
                        }
                        .disabled(!canSplit)
                        .padding(.horizontal, 20)
                        
                        Spacer().frame(height: 40)
                    }
                    .padding(.top, 20)
                }
            }
            .navigationTitle("Split Expense")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("Cancel") { dismiss() }.foregroundColor(.accent1)
                }
                ToolbarItemGroup(placement: .keyboard) {
                    Spacer()
                    Button("Done") { focus = nil }.foregroundColor(.accent1)
                }
            }
        }
    }

    private func detailRow(icon: String, placeholder: String, text: Binding<String>, field: Field) -> some View {
        HStack(spacing: 14) {
            Image(systemName: icon).foregroundColor(.accent1).frame(width: 20)
            TextField(placeholder, text: text)
                .focused($focus, equals: field)
                .font(.system(size: 15))
                .foregroundColor(.white)
        }
        .padding(.horizontal, 16).padding(.vertical, 16)
    }

    private func handleSplit() {
        guard canSplit else { return }
        
        let share = myShare
        let selected = friendsWithPhones.filter { selectedFriends.contains($0.phone) }
        let friendsData = selected.map { (name: $0.name, phone: $0.phone, share: share) }
        
        store.splitBill(
            totalAmount: totalAmount,
            myShare: share,
            note: note.trimmingCharacters(in: .whitespaces),
            groupName: groupName.isEmpty ? nil : groupName.trimmingCharacters(in: .whitespaces),
            friends: friendsData
        )
        dismiss()
    }
}
