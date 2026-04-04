import SwiftUI

// MARK: - LoginView
// Phone number entry + OTP verification with a clean dark UI.

struct LoginView: View {
    @EnvironmentObject var auth: AuthService

    @State private var phone      = ""
    @State private var otp        = ""
    @State private var step       = 0   // 0 = phone, 1 = OTP
    @FocusState private var focus : LoginField?

    enum LoginField { case phone, otp }

    var body: some View {
        ZStack {
            Color.bgPrimary.ignoresSafeArea()

            // Background gradient orbs
            Circle()
                .fill(Color.accent1.opacity(0.12))
                .frame(width: 340, height: 340)
                .blur(radius: 80)
                .offset(x: 100, y: -200)
            Circle()
                .fill(Color.catPurple.opacity(0.10))
                .frame(width: 280, height: 280)
                .blur(radius: 80)
                .offset(x: -100, y: 300)

            VStack(spacing: 0) {
                Spacer()

                // Logo / Brand
                VStack(spacing: 14) {
                    ZStack {
                        Circle()
                            .fill(LinearGradient(colors: [Color.accent1, Color.catPurple], startPoint: .topLeading, endPoint: .bottomTrailing))
                            .frame(width: 80, height: 80)
                        Image(systemName: "indianrupeesign")
                            .font(.system(size: 34, weight: .bold))
                            .foregroundColor(.white)
                    }
                    .shadow(color: Color.accent1.opacity(0.4), radius: 20, y: 8)

                    Text("MoneyManager")
                        .font(.system(size: 28, weight: .bold))
                        .foregroundStyle(
                            LinearGradient(colors: [.white, Color.white.opacity(0.75)], startPoint: .leading, endPoint: .trailing)
                        )
                    Text("Your finances, synced & shared")
                        .font(.system(size: 14))
                        .foregroundColor(.textSecondary)
                }
                .padding(.bottom, 56)

                // Card
                VStack(spacing: 24) {
                    if step == 0 {
                        phoneStep
                    } else {
                        otpStep
                    }

                    if let err = auth.error {
                        HStack(spacing: 8) {
                            Image(systemName: "exclamationmark.circle.fill")
                                .foregroundColor(.expenseRed)
                            Text(err)
                                .font(.system(size: 13))
                                .foregroundColor(.expenseRed)
                        }
                        .padding(12)
                        .background(Color.expenseRed.opacity(0.1))
                        .clipShape(RoundedRectangle(cornerRadius: 10))
                    }
                }
                .padding(28)
                .background(
                    RoundedRectangle(cornerRadius: 28, style: .continuous)
                        .fill(Color.bgCard.opacity(0.7))
                        .overlay(RoundedRectangle(cornerRadius: 28).stroke(Color.white.opacity(0.07), lineWidth: 1))
                )
                .padding(.horizontal, 20)

                Spacer()

                Text("Your data is encrypted and stored securely\nby Firebase / Google.")
                    .font(.system(size: 11))
                    .foregroundColor(.textTertiary)
                    .multilineTextAlignment(.center)
                    .padding(.bottom, 32)
            }
        }
        .toolbar {
            ToolbarItemGroup(placement: .keyboard) {
                Spacer()
                Button("Done") { focus = nil }
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundColor(.accent1)
            }
        }
    }

    // ── Step 0: Enter phone number ───────────────────────────────────
    private var phoneStep: some View {
        VStack(spacing: 20) {
            VStack(alignment: .leading, spacing: 6) {
                Text("Welcome 👋")
                    .font(.system(size: 22, weight: .bold))
                    .foregroundColor(.white)
                Text("Enter your Indian mobile number to continue.")
                    .font(.system(size: 13))
                    .foregroundColor(.textSecondary)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            // Phone field
            HStack(spacing: 12) {
                HStack(spacing: 6) {
                    Text("🇮🇳").font(.system(size: 18))
                    Text("+91")
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundColor(Color.white.opacity(0.8))
                }
                .padding(.horizontal, 12).padding(.vertical, 14)
                .background(Color.white.opacity(0.06))
                .clipShape(RoundedRectangle(cornerRadius: 12))

                TextField("9876543210", text: $phone)
                    .focused($focus, equals: .phone)
                    .keyboardType(.phonePad)
                    .font(.system(size: 18, weight: .medium))
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
            }
            .padding(4)
            .background(Color.white.opacity(0.05))
            .clipShape(RoundedRectangle(cornerRadius: 14))
            .overlay(RoundedRectangle(cornerRadius: 14).stroke(Color.accent1.opacity(phone.count == 10 ? 0.6 : 0.1), lineWidth: 1.5))

            actionButton("Send OTP", disabled: phone.count < 10 || auth.isLoading) {
                focus = nil
                Task { await auth.sendOTP(phoneNumber: phone) }
            }
        }
        .onChange(of: auth.error) { _ in } // Refresh on error
        .onChange(of: auth.isLoading) { loading in
            if !loading && auth.error == nil && step == 0 {
                withAnimation(.spring()) { step = 1 }
            }
        }
    }

    // ── Step 1: Enter OTP ────────────────────────────────────────────
    private var otpStep: some View {
        VStack(spacing: 20) {
            VStack(alignment: .leading, spacing: 6) {
                Text("Enter OTP 🔐")
                    .font(.system(size: 22, weight: .bold))
                    .foregroundColor(.white)
                Text("We sent a 6-digit code to +91 \(phone)")
                    .font(.system(size: 13))
                    .foregroundColor(.textSecondary)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            TextField("6-digit code", text: $otp)
                .focused($focus, equals: .otp)
                .keyboardType(.numberPad)
                .font(.system(size: 28, weight: .bold))
                .foregroundColor(.white)
                .multilineTextAlignment(.center)
                .padding(18)
                .background(Color.white.opacity(0.05))
                .clipShape(RoundedRectangle(cornerRadius: 14))
                .overlay(RoundedRectangle(cornerRadius: 14).stroke(Color.accent1.opacity(otp.count == 6 ? 0.6 : 0.1), lineWidth: 1.5))
                .onAppear { focus = .otp }

            actionButton("Verify & Login", disabled: otp.count < 6 || auth.isLoading) {
                focus = nil
                Task { await auth.verifyOTP(otp) }
            }

            Button {
                withAnimation { step = 0; otp = "" }
            } label: {
                Text("Wrong number? Go back")
                    .font(.system(size: 13))
                    .foregroundColor(.textSecondary)
            }
        }
    }

    @ViewBuilder
    private func actionButton(_ label: String, disabled: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Group {
                if auth.isLoading {
                    ProgressView().tint(.white)
                } else {
                    Text(label)
                        .font(.system(size: 16, weight: .bold))
                        .foregroundColor(.white)
                }
            }
            .frame(maxWidth: .infinity).frame(height: 52)
            .background(
                disabled
                ? AnyShapeStyle(Color.bgCardAlt)
                : AnyShapeStyle(LinearGradient.accentGradient)
            )
            .clipShape(RoundedRectangle(cornerRadius: 16))
            .shadow(color: disabled ? .clear : Color.accent1.opacity(0.35), radius: 12, y: 6)
        }
        .disabled(disabled)
    }
}
