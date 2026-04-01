import SwiftUI

// MARK: - Color Palette (Dark Minimal Fintech)

extension Color {
    // Backgrounds
    static let bgPrimary   = Color(hex: "#161618")   // near-black canvas
    static let bgCard      = Color(hex: "#222226")   // card surface
    static let bgCardAlt   = Color(hex: "#2C2C30")   // elevated card / input
    static let navBg       = Color(hex: "#1A1A1E")   // tab bar

    // Text
    static let textPrimary   = Color(hex: "#FFFFFF")
    static let textSecondary = Color(hex: "#8E8E93")
    static let textTertiary  = Color(hex: "#3A3A44")

    // Accents
    static let accent1   = Color(hex: "#FF3B30")    // red — primary actions
    static let accent2   = Color(hex: "#FF6961")    // light red highlight
    static let accentBlue = Color(hex: "#5AC8FA")

    // Finance
    static let incomeGreen = Color(hex: "#30D158")
    static let expenseRed  = Color(hex: "#FF453A")

    // Category Palette (vivid but refined)
    static let catOrange  = Color(hex: "#FF9F0A")
    static let catPurple  = Color(hex: "#7B61FF")
    static let catBlue    = Color(hex: "#5AC8FA")
    static let catGreen   = Color(hex: "#30D158")
    static let catRed     = Color(hex: "#FF453A")
    static let catYellow  = Color(hex: "#FFD60A")
    static let catPink    = Color(hex: "#FF375F")
    static let catTeal    = Color(hex: "#5AC8FA")
    static let catGray    = Color(hex: "#636366")

    init(hex: String) {
        var h = hex.trimmingCharacters(in: .whitespacesAndNewlines)
        if h.hasPrefix("#") { h.removeFirst() }
        var n: UInt64 = 0
        Scanner(string: h).scanHexInt64(&n)
        self.init(red: Double((n>>16)&0xFF)/255,
                  green: Double((n>>8)&0xFF)/255,
                  blue: Double(n&0xFF)/255)
    }
}

// MARK: - Gradients (subtle, used sparingly)

extension LinearGradient {
    static let accentGradient = LinearGradient(
        colors: [Color(hex: "#FF3B30"), Color(hex: "#FF6B35")],
        startPoint: .topLeading, endPoint: .bottomTrailing)

    static let incomeGradient = LinearGradient(
        colors: [Color(hex: "#30D158"), Color(hex: "#34C759")],
        startPoint: .topLeading, endPoint: .bottomTrailing)

    static let expenseGradient = LinearGradient(
        colors: [Color(hex: "#FF453A"), Color(hex: "#FF3B30")],
        startPoint: .topLeading, endPoint: .bottomTrailing)
}

// MARK: - Card Modifier (flat dark, no glass/blur)

struct DarkCard: ViewModifier {
    var radius: CGFloat = 18
    func body(content: Content) -> some View {
        content
            .background(
                RoundedRectangle(cornerRadius: radius, style: .continuous)
                    .fill(Color.bgCard)
                    .overlay(
                        RoundedRectangle(cornerRadius: radius, style: .continuous)
                            .stroke(Color.white.opacity(0.055), lineWidth: 0.6)
                    )
            )
    }
}

extension View {
    func glassCard(radius: CGFloat = 18) -> some View {
        modifier(DarkCard(radius: radius))
    }
}

extension Color {
    static let bgCardGlass = Color.bgCard   // alias used in some views
}

// MARK: - Number Formatting

func fmt(_ value: Double, symbol: String = "₹") -> String {
    let f = NumberFormatter()
    f.numberStyle = .currency
    f.currencySymbol = symbol
    f.maximumFractionDigits = 0
    return f.string(from: NSNumber(value: value)) ?? "\(symbol)\(Int(value))"
}

// MARK: - Global Keyboard Dismiss

extension UIApplication {
    /// Dismiss any active keyboard from any text field in the app.
    func hideKeyboard() {
        sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
    }
}

extension View {
    /// Call on a SwiftUI button/gesture to dismiss keyboard first.
    func hideKeyboard() {
        UIApplication.shared.hideKeyboard()
    }
}
