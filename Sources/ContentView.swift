import SwiftUI

struct ContentView: View {
    @EnvironmentObject var store: DataStore
    @State private var selectedTab = 0
    @State private var showPay     = false

    var body: some View {
        ZStack(alignment: .bottom) {
            Color.bgPrimary.ignoresSafeArea()

            // ── Pages ──────────────────────────────────────────
            Group {
                switch selectedTab {
                case 0: DashboardView()
                case 1: AnalyticsView()
                case 2: LendBorrowView()
                case 3: HistoryView()
                default: DashboardView()
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .top)
            // Push content above the tab bar — cleaner than magic padding
            .safeAreaInset(edge: .bottom) { Color.clear.frame(height: 96) }

            // ── Tab Bar ─────────────────────────────────────────
            MinimalTabBar(selected: $selectedTab, showPay: $showPay)
                // Stay fixed — don't float up when keyboard appears in sheets
                .ignoresSafeArea(.keyboard)
        }
        .sheet(isPresented: $showPay) {
            PaymentView().environmentObject(store)
        }
    }
}

// MARK: - Minimal Tab Bar

struct MinimalTabBar: View {
    @Binding var selected: Int
    @Binding var showPay: Bool

    struct TabItem { let icon: String; let idx: Int }

    let left  = [TabItem(icon: "house",           idx: 0),
                 TabItem(icon: "chart.bar",        idx: 1)]
    let right = [TabItem(icon: "person.2",  idx: 2),
                 TabItem(icon: "clock",        idx: 3)]

    var body: some View {
        HStack(spacing: 0) {
            // Left tabs
            ForEach(left, id: \.idx) { t in
                MinimalTabItem(icon: t.icon, isSelected: selected == t.idx) {
                    withAnimation(.easeInOut(duration: 0.15)) { selected = t.idx }
                }
            }

            // Red FAB — pay button
            Button { showPay = true } label: {
                ZStack {
                    Circle()
                        .fill(Color.accent1)
                        .frame(width: 54, height: 54)
                    Image(systemName: "indianrupeesign")
                        .font(.system(size: 20, weight: .semibold))
                        .foregroundColor(.white)
                }
                .shadow(color: Color.accent1.opacity(0.45), radius: 12, y: 5)
            }
            .offset(y: -14)
            .frame(width: 80)

            // Right tabs
            ForEach(right, id: \.idx) { t in
                MinimalTabItem(icon: t.icon, isSelected: selected == t.idx) {
                    withAnimation(.easeInOut(duration: 0.15)) { selected = t.idx }
                }
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(
            Capsule()
                .fill(Color.bgCardAlt.opacity(0.85))
                .overlay(Capsule().stroke(Color.white.opacity(0.1), lineWidth: 1))
        )
        .padding(.horizontal, 24)
        .padding(.bottom, 16)
        .shadow(color: Color.black.opacity(0.5), radius: 20, y: 10)
    }
}

struct MinimalTabItem: View {
    let icon: String
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(spacing: 3) {
                Image(systemName: isSelected ? "\(icon).fill" : icon)
                    .font(.system(size: 21, weight: .regular))
                    .foregroundColor(isSelected ? .white : Color(hex: "#3A3A4A"))
                // Dot indicator
                Circle()
                    .fill(isSelected ? Color.accent1 : Color.clear)
                    .frame(width: 4, height: 4)
            }
            .frame(maxWidth: .infinity)
            // Ensure full area is tappable (not just text/icon bounds)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }
}

#Preview {
    ContentView().environmentObject(DataStore())
}
