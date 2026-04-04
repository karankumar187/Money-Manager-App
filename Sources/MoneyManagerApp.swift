import SwiftUI
import FirebaseCore
import FirebaseAuth

class AppDelegate: NSObject, UIApplicationDelegate {
    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil) -> Bool {
        FirebaseApp.configure()
        return true
    }
}

@main
struct MoneyManagerApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate
    
    @StateObject private var auth = AuthService()
    @StateObject private var store = CloudDataStore()

    var body: some Scene {
        WindowGroup {
            AuthRouter()
                .environmentObject(auth)
                .environmentObject(store)
                .preferredColorScheme(.dark)
        }
    }
}

struct AuthRouter: View {
    @EnvironmentObject var auth: AuthService
    @EnvironmentObject var store: CloudDataStore
    
    var body: some View {
        Group {
            if auth.isSignedIn {
                if store.isLoading {
                    ZStack {
                        Color.bgPrimary.ignoresSafeArea()
                        ProgressView().tint(.accent1)
                    }
                    .onAppear {
                        // Once signed in, load DataStore
                        store.startListening()
                    }
                } else if store.userProfile == nil {
                    ProfileSetupView()
                } else {
                    ContentView()
                        .onAppear {
                            // Trigger migration checking after profile exists
                            Task {
                                let m = MigrationService()
                                await m.migrateIfNeeded(uid: Auth.auth().currentUser?.uid ?? "")
                            }
                        }
                }
            } else {
                LoginView()
                    .onAppear { store.stopListening() }
            }
        }
        .animation(.easeInOut, value: auth.isSignedIn)
        .animation(.easeInOut, value: store.isLoading)
        .animation(.easeInOut, value: store.userProfile != nil)
    }
}
