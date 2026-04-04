import SwiftUI

struct CustomSplitSheet: View {
    @EnvironmentObject var store: CloudDataStore
    @Environment(\.dismiss) var dismiss

    let totalAmount: Double
    
    @Binding var isSplitEnabled: Bool
    @Binding var groupName: String
    @Binding var finalFriends: [(name: String, phone: String, share: Double)]

    @State private var selectedFriends: Set<String> = []
    @State private var customAmounts: [String: String] = [:]
    
    // Track if a user has manually edited a field
    @State private var hasManuallyEdited: Set<String> = []

    private var friendsWithPhones: [(name: String, phone: String)] {
        store.savedContacts.compactMap { name, phone in
            guard let p = phone, !p.isEmpty else { return nil }
            return (name: name, phone: p)
        }.sorted(by: { $0.name < $1.name })
    }

    private var myShare: Double {
        let friendsSum = selectedFriends.reduce(0.0) { sum, phone in
            sum + (Double(customAmounts[phone] ?? "") ?? 0.0)
        }
        return max(0, totalAmount - friendsSum)
    }

    private var isValidSplit: Bool {
        let friendsSum = selectedFriends.reduce(0.0) { sum, phone in
            sum + (Double(customAmounts[phone] ?? "") ?? 0.0)
        }
        return totalAmount >= 0 && friendsSum <= totalAmount && !selectedFriends.isEmpty
    }

    var body: some View {
        NavigationView {
            ZStack {
                Color.bgPrimary.ignoresSafeArea()

                ScrollView {
                    VStack(spacing: 24) {
                        
                        // Summary Hero
                        VStack(spacing: 8) {
                            Text("TOTAL AMOUNT")
                                .font(.system(size: 11, weight: .bold))
                                .foregroundColor(.textSecondary).tracking(1.5)
                            Text(store.formatted(totalAmount))
                                .font(.system(size: 40, weight: .bold, design: .rounded))
                                .foregroundColor(.white)
                            
                            HStack(spacing: 6) {
                                Text("Your Share:")
                                    .font(.system(size: 14)).foregroundColor(.textSecondary)
                                Text(store.formatted(myShare))
                                    .font(.system(size: 15, weight: .bold))
                                    .foregroundColor(myShare < 0 ? .red : .accent1)
                            }
                            .padding(.top, 4)
                        }
                        .padding(.vertical, 24).padding(.horizontal, 20)
                        .frame(maxWidth: .infinity)
                        .background(Color.white.opacity(0.04))
                        .clipShape(RoundedRectangle(cornerRadius: 24))
                        .overlay(RoundedRectangle(cornerRadius: 24).stroke(Color.white.opacity(0.08), lineWidth: 1))
                        .padding(.horizontal, 20)

                        // Split Group Name
                        VStack(spacing: 0) {
                            HStack(spacing: 14) {
                                Image(systemName: "tag.fill").foregroundColor(.accent1).frame(width: 20)
                                TextField("Group Name (Optional)", text: $groupName)
                                    .font(.system(size: 15)).foregroundColor(.white)
                            }
                            .padding(.horizontal, 16).padding(.vertical, 16)
                        }
                        .glassCard(radius: 20)
                        .padding(.horizontal, 20)

                        // Contacts list
                        VStack(alignment: .leading, spacing: 16) {
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
                                VStack(spacing: 12) {
                                    ForEach(friendsWithPhones, id: \.phone) { friend in
                                        let isSel = selectedFriends.contains(friend.phone)
                                        HStack(spacing: 12) {
                                            Button {
                                                toggleFriend(friend.phone)
                                            } label: {
                                                HStack {
                                                    Circle()
                                                        .fill(isSel ? Color.accent1 : Color.white.opacity(0.1))
                                                        .frame(width: 24, height: 24)
                                                        .overlay(Image(systemName: "checkmark").font(.system(size: 11, weight: .bold)).foregroundColor(.white).opacity(isSel ? 1 : 0))
                                                    Text(friend.name)
                                                        .font(.system(size: 15, weight: .medium))
                                                        .foregroundColor(isSel ? .white : .textSecondary)
                                                        .lineLimit(1)
                                                    Spacer(minLength: 0)
                                                }
                                            }
                                            
                                            if isSel {
                                                HStack(spacing: 6) {
                                                    Text(store.currencySymbol)
                                                        .font(.system(size: 14, weight: .medium)).foregroundColor(.textSecondary)
                                                    TextField("0", text: Binding(
                                                        get: { customAmounts[friend.phone] ?? "" },
                                                        set: { val in
                                                            customAmounts[friend.phone] = val
                                                            hasManuallyEdited.insert(friend.phone)
                                                        }
                                                    ))
                                                    .keyboardType(.decimalPad)
                                                    .font(.system(size: 16, weight: .bold))
                                                    .foregroundColor(.white)
                                                    .multilineTextAlignment(.trailing)
                                                    .frame(width: 80)
                                                }
                                                .padding(.horizontal, 12).padding(.vertical, 8)
                                                .background(Color.white.opacity(0.08))
                                                .clipShape(RoundedRectangle(cornerRadius: 10))
                                            }
                                        }
                                        .padding(.horizontal, 16).padding(.vertical, 12)
                                        .background(isSel ? Color.accent1.opacity(0.1) : Color.white.opacity(0.03))
                                        .clipShape(RoundedRectangle(cornerRadius: 16))
                                        .overlay(RoundedRectangle(cornerRadius: 16).stroke(isSel ? Color.accent1.opacity(0.5) : Color.white.opacity(0.05), lineWidth: 1))
                                    }
                                }
                                .padding(.horizontal, 20)
                            }
                        }

                        // Save Button
                        Button {
                            confirmSplit()
                        } label: {
                            Text("Confirm Split")
                                .font(.system(size: 17, weight: .bold))
                                .foregroundColor(.white)
                                .frame(maxWidth: .infinity).frame(height: 58)
                                .background(isValidSplit ? LinearGradient.accentGradient : LinearGradient(colors: [Color.bgCardAlt], startPoint: .leading, endPoint: .trailing))
                                .clipShape(RoundedRectangle(cornerRadius: 20))
                                .shadow(color: isValidSplit ? Color.accent1.opacity(0.4) : .clear, radius: 12, y: 6)
                        }
                        .disabled(!isValidSplit)
                        .padding(.horizontal, 20)
                        
                        if !isValidSplit && selectedFriends.count > 0 {
                            Text("Total split shares exceed the payment amount!")
                                .font(.system(size: 12))
                                .foregroundColor(.red)
                        }

                        Spacer().frame(height: 40)
                    }
                    .padding(.top, 20)
                }
            }
            .navigationTitle("Split Details")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("Clear") { 
                        isSplitEnabled = false
                        finalFriends = []
                        dismiss()
                    }.foregroundColor(.textSecondary)
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button(action: distributeEqually) {
                        Text("Equal Split").font(.system(size: 13, weight: .semibold))
                    }.foregroundColor(.accent1)
                }
            }
            .onAppear {
                initFromFinalFriends()
            }
        }
    }

    private func toggleFriend(_ phone: String) {
        if selectedFriends.contains(phone) {
            selectedFriends.remove(phone)
            customAmounts.removeValue(forKey: phone)
            hasManuallyEdited.remove(phone)
        } else {
            selectedFriends.insert(phone)
        }
        recalculateUnedited()
    }
    
    private func initFromFinalFriends() {
        if !finalFriends.isEmpty {
            for f in finalFriends {
                selectedFriends.insert(f.phone)
                customAmounts[f.phone] = String(format: "%.2f", f.share)
                hasManuallyEdited.insert(f.phone)
            }
        }
    }

    private func distributeEqually() {
        guard !selectedFriends.isEmpty else { return }
        let share = totalAmount / Double(selectedFriends.count + 1) // Everyone pays equal
        let shareStr = String(format: "%.2f", share)
        for phone in selectedFriends {
            customAmounts[phone] = shareStr
        }
        hasManuallyEdited.removeAll()
    }

    private func recalculateUnedited() {
        guard !selectedFriends.isEmpty else { return }
        
        let editedSum = hasManuallyEdited.reduce(0.0) { sum, phone in 
            sum + (Double(customAmounts[phone] ?? "") ?? 0.0)
        }
        
        let uneditedFriends = selectedFriends.subtracting(hasManuallyEdited)
        if uneditedFriends.isEmpty { return }
        
        let remainingForEquitable = max(0, totalAmount - editedSum)
        let equitableShare = remainingForEquitable / Double(uneditedFriends.count + 1)
        let shareStr = String(format: "%.2f", equitableShare)
        
        for phone in uneditedFriends {
            customAmounts[phone] = shareStr
        }
    }

    private func confirmSplit() {
        guard isValidSplit else { return }
        isSplitEnabled = true
        var result: [(name: String, phone: String, share: Double)] = []
        
        for friendPhone in selectedFriends {
            let amount = Double(customAmounts[friendPhone] ?? "") ?? 0.0
            if let fData = friendsWithPhones.first(where: { $0.phone == friendPhone }) {
                result.append((name: fData.name, phone: friendPhone, share: amount))
            }
        }
        
        finalFriends = result
        dismiss()
    }
}
