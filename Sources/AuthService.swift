import Foundation
import FirebaseAuth
import Combine

// MARK: - AuthService
// Handles phone OTP sign-in via Firebase Authentication.

@MainActor
class AuthService: ObservableObject {

    @Published var currentUser: FirebaseAuth.User? = Auth.auth().currentUser
    @Published var error: String? = nil
    @Published var isLoading = false

    private var verificationID: String? = nil
    private var handle: AuthStateDidChangeListenerHandle?

    init() {
        // Listen for auth state changes (handles auto-restore on app launch)
        handle = Auth.auth().addStateDidChangeListener { [weak self] _, user in
            self?.currentUser = user
        }
    }

    deinit {
        if let h = handle { Auth.auth().removeStateDidChangeListener(h) }
    }

    var isSignedIn: Bool { currentUser != nil }

    // MARK: – Step 1: Send OTP
    func sendOTP(phoneNumber: String) async {
        isLoading = true
        error = nil
        do {
            let vid = try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<String, Error>) in
                PhoneAuthProvider.provider().verifyPhoneNumber("+91\(phoneNumber)", uiDelegate: nil) { verificationID, error in
                    if let error = error {
                        continuation.resume(throwing: error)
                        return
                    }
                    if let vid = verificationID {
                        continuation.resume(returning: vid)
                    } else {
                        let err = NSError(domain: "AuthError", code: -1, userInfo: [NSLocalizedDescriptionKey: "Internal Error: No verification ID"])
                        continuation.resume(throwing: err)
                    }
                }
            }
            verificationID = vid
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    // MARK: – Step 2: Verify OTP and sign in
    func verifyOTP(_ code: String) async {
        guard let vid = verificationID else {
            error = "Please request an OTP first."
            return
        }
        isLoading = true
        error = nil
        let credential = PhoneAuthProvider.provider().credential(
            withVerificationID: vid,
            verificationCode: code
        )
        do {
            let result = try await Auth.auth().signIn(with: credential)
            currentUser = result.user
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    func signOut() {
        try? Auth.auth().signOut()
        currentUser = nil
    }
}
