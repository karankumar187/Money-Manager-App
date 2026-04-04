import SwiftUI
import PhotosUI

// MARK: - ProfileSetupView
// Shown after first-time login before showing the main app.

struct ProfileSetupView: View {
    @EnvironmentObject var auth  : AuthService
    @EnvironmentObject var store : CloudDataStore

    @State private var displayName  = ""
    @State private var upiId        = ""
    @State private var photoItem    : PhotosPickerItem? = nil
    @State private var imageData    : Data? = nil
    @State private var isLoading    = false
    @FocusState private var focused : ProfileField?

    enum ProfileField { case name, upi }

    private var phone: String { auth.currentUser?.phoneNumber ?? "" }

    var body: some View {
        ZStack {
            Color.bgPrimary.ignoresSafeArea()

            ScrollView {
                VStack(spacing: 32) {
                    // Header
                    VStack(spacing: 8) {
                        Text("Set Up Your Profile")
                            .font(.system(size: 26, weight: .bold))
                            .foregroundColor(.white)
                        Text("This helps your friends find and pay you.")
                            .font(.system(size: 14))
                            .foregroundColor(.textSecondary)
                    }
                    .padding(.top, 50)

                    // Avatar picker
                    PhotosPicker(selection: $photoItem, matching: .images) {
                        ZStack(alignment: .bottomTrailing) {
                            if let data = imageData, let ui = UIImage(data: data) {
                                Image(uiImage: ui)
                                    .resizable().scaledToFill()
                                    .frame(width: 100, height: 100)
                                    .clipShape(Circle())
                            } else {
                                Circle()
                                    .fill(LinearGradient(colors: [Color.accent1.opacity(0.5), Color.catPurple.opacity(0.5)], startPoint: .topLeading, endPoint: .bottomTrailing))
                                    .frame(width: 100, height: 100)
                                    .overlay(
                                        Image(systemName: "person.fill")
                                            .font(.system(size: 40))
                                            .foregroundColor(.white.opacity(0.7))
                                    )
                            }
                            Circle()
                                .fill(Color.accent1)
                                .frame(width: 30, height: 30)
                                .overlay(Image(systemName: "camera.fill").font(.system(size: 13)).foregroundColor(.white))
                                .overlay(Circle().stroke(Color.bgPrimary, lineWidth: 2))
                        }
                    }
                    .onChange(of: photoItem) { item in
                        Task {
                            imageData = try? await item?.loadTransferable(type: Data.self)
                        }
                    }

                    // Fields
                    VStack(spacing: 14) {
                        profileField(icon: "person.fill", placeholder: "Your name", text: $displayName, field: .name)

                        profileField(icon: "indianrupeesign.circle.fill", placeholder: "UPI ID (e.g. karan@ybl)", text: $upiId, field: .upi)
                            .keyboardType(.emailAddress)
                            .autocapitalization(.none)
                            .disableAutocorrection(true)

                        // Phone (read-only)
                        HStack(spacing: 14) {
                            Image(systemName: "phone.fill")
                                .foregroundColor(.incomeGreen)
                                .frame(width: 22)
                            Text(phone)
                                .font(.system(size: 15))
                                .foregroundColor(.textSecondary)
                            Spacer()
                            Text("Verified ✓")
                                .font(.system(size: 12, weight: .semibold))
                                .foregroundColor(.incomeGreen)
                        }
                        .padding(16)
                        .background(Color.white.opacity(0.05))
                        .clipShape(RoundedRectangle(cornerRadius: 14))
                    }
                    .padding(.horizontal, 24)

                    // Save button
                    Button {
                        guard !displayName.trimmingCharacters(in: .whitespaces).isEmpty else { return }
                        focused = nil
                        isLoading = true
                        Task {
                            await store.saveProfile(name: displayName.trimmingCharacters(in: .whitespaces),
                                                    upiId: upiId.trimmingCharacters(in: .whitespaces),
                                                    phone: phone,
                                                    imageData: imageData)
                            isLoading = false
                        }
                    } label: {
                        Group {
                            if isLoading {
                                ProgressView().tint(.white)
                            } else {
                                Text("Save & Continue →")
                                    .font(.system(size: 16, weight: .bold))
                                    .foregroundColor(.white)
                            }
                        }
                        .frame(maxWidth: .infinity).frame(height: 54)
                        .background(
                            displayName.isEmpty
                            ? AnyShapeStyle(Color.bgCardAlt)
                            : AnyShapeStyle(LinearGradient.accentGradient)
                        )
                        .clipShape(RoundedRectangle(cornerRadius: 18))
                        .shadow(color: displayName.isEmpty ? .clear : Color.accent1.opacity(0.35), radius: 12, y: 6)
                    }
                    .disabled(displayName.trimmingCharacters(in: .whitespaces).isEmpty || isLoading)
                    .padding(.horizontal, 24)

                    Button("Skip for now") {
                        store.userName = auth.currentUser?.phoneNumber ?? "User"
                        Task {
                            await store.saveProfile(name: store.userName, upiId: "", phone: phone, imageData: nil)
                        }
                    }
                    .font(.system(size: 14))
                    .foregroundColor(.textSecondary)
                    .padding(.bottom, 40)
                }
            }
        }
        .toolbar {
            ToolbarItemGroup(placement: .keyboard) {
                Spacer()
                Button("Done") { focused = nil }
                    .foregroundColor(.accent1)
                    .font(.system(size: 15, weight: .semibold))
            }
        }
    }

    @ViewBuilder
    private func profileField(icon: String, placeholder: String, text: Binding<String>, field: ProfileField) -> some View {
        HStack(spacing: 14) {
            Image(systemName: icon)
                .foregroundColor(.accent1)
                .frame(width: 22)
            TextField(placeholder, text: text)
                .focused($focused, equals: field)
                .font(.system(size: 15))
                .foregroundColor(.white)
        }
        .padding(16)
        .background(Color.white.opacity(0.05))
        .clipShape(RoundedRectangle(cornerRadius: 14))
        .overlay(RoundedRectangle(cornerRadius: 14).stroke(Color.accent1.opacity(focused == field ? 0.5 : 0.0), lineWidth: 1.5))
    }
}
