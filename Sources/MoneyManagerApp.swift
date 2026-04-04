import SwiftUI
import FirebaseCore
import FirebaseAuth

class AppDelegate: NSObject, UIApplicationDelegate {
    var window: UIWindow?

    func application(_ application: UIApplication,
                     didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil) -> Bool {
        FirebaseApp.configure()
        return true
    }

    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        Auth.auth().setAPNSToken(deviceToken, type: .unknown)
    }

    func application(_ application: UIApplication, didReceiveRemoteNotification userInfo: [AnyHashable : Any], fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void) {
        if Auth.auth().canHandleNotification(userInfo) {
            completionHandler(.noData)
            return
        }
        completionHandler(.newData)
    }

    func application(_ app: UIApplication, open url: URL, options: [UIApplication.OpenURLOptionsKey : Any] = [:]) -> Bool {
        if Auth.auth().canHandle(url) {
            return true
        }
        return false
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
                .onOpenURL { url in
                    let _ = Auth.auth().canHandle(url)
                }
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
