import SwiftUI
import Charts

struct AnalyticsView: View {
    @EnvironmentObject var store: DataStore
    @State private var period: AnalyticsPeriod = .monthly
    @State private var selectedCategory: AppCategory? = nil

    var body: some View {
        ScrollView(showsIndicators: false) {
            VStack(spacing: 0) {

                // ── Header ──────────────────────────────────────
                HStack {
                    Text("Analytics")
                        .font(.system(size: 30, weight: .bold)).foregroundColor(.white)
                    Spacer()
                }
                .padding(.horizontal, 24).padding(.top, 60).padding(.bottom, 24)

                // ── Period Toggle ───────────────────────────────
                HStack(spacing: 6) {
                    ForEach(AnalyticsPeriod.allCases, id: \.self) { p in
                        Button {
                            withAnimation(.spring(response: 0.3)) { period = p }
                        } label: {
                            Text(p.rawValue)
                                .font(.system(size: 13, weight: .semibold))
                                .foregroundColor(period == p ? .white : .textSecondary)
                                .frame(maxWidth: .infinity).frame(height: 36)
                                .background(
                                    Group {
                                        if period == p {
                                            LinearGradient.accentGradient
                                        } else {
                                            LinearGradient(colors: [Color.clear], startPoint: .top, endPoint: .bottom)
                                        }
                                    }
                                )
                                .clipShape(Capsule())
                        }
                    }
                }
                .padding(4).background(Capsule().fill(Color.bgCard))
                .padding(.horizontal, 20).padding(.bottom, 28)

                // ── Bar Chart ───────────────────────────────────
                VStack(alignment: .leading, spacing: 12) {
                    Text(barChartTitle).font(.system(size: 15, weight: .bold)).foregroundColor(.white)
                        .padding(.horizontal, 4)
                    barChart
                }
                .padding(20).glassCard(radius: 22)
                .padding(.horizontal, 20).padding(.bottom, 24)

                // ── Donut + Top Stats ───────────────────────────
                let catData = store.categoryData(for: period)
                let totalSpent = catData.reduce(0) { $0 + $1.amount }

                VStack(spacing: 20) {
                    // Donut
                    ZStack {
                        DonutChart(data: catData)
                            .frame(width: 200, height: 200)
                        VStack(spacing: 2) {
                            Text("Spent").font(.system(size: 11)).foregroundColor(.textSecondary)
                            Text(store.formatted(totalSpent))
                                .font(.system(size: 20, weight: .bold)).foregroundColor(.white)
                        }
                    }

                    // Category bars
                    if catData.isEmpty {
                        Text("No data for this period")
                            .font(.system(size: 14)).foregroundColor(.textSecondary).padding(20)
                    } else {
                        ForEach(catData.prefix(8)) { item in
                            CategoryBar(item: item, total: totalSpent)
                                .onTapGesture { selectedCategory = item.category }
                        }
                    }
                }
                .padding(20).glassCard(radius: 22)
                .padding(.horizontal, 20).padding(.bottom, 20)
            }
        }
        .sheet(item: $selectedCategory) { cat in
            CategoryDetailView(category: cat)
                .environmentObject(store)
        }
    }

    // MARK: - Bar Chart

    private var barChartTitle: String {
        switch period {
        case .daily:   return "Last 14 Days"
        case .weekly:  return "Last 8 Weeks"
        case .monthly: return "Last 6 Months"
        }
    }

    @ViewBuilder
    private var barChart: some View {
        switch period {
        case .daily:
            let data = store.dailyData(days: 14)
            Chart {
                ForEach(data) { d in
                    BarMark(x: .value("Day", d.label), y: .value("₹", d.amount))
                        .foregroundStyle(LinearGradient.accentGradient)
                        .cornerRadius(6)
                }
            }
            .chartXAxis {
                AxisMarks(values: .stride(by: 2)) { _ in
                    AxisValueLabel().font(.system(size: 9)).foregroundStyle(Color.white.opacity(0.4))
                }
            }
            .chartYAxis {
                AxisMarks { _ in
                    AxisValueLabel().font(.system(size: 9)).foregroundStyle(Color.white.opacity(0.4))
                    AxisGridLine().foregroundStyle(Color.white.opacity(0.05))
                }
            }
            .frame(height: 180)

        case .weekly:
            let data = store.weeklyData(weeks: 8)
            Chart {
                ForEach(data) { d in
                    BarMark(x: .value("Week", d.label), y: .value("₹", d.amount))
                        .foregroundStyle(LinearGradient.accentGradient)
                        .cornerRadius(6)
                }
            }
            .chartXAxis {
                AxisMarks { _ in
                    AxisValueLabel().font(.system(size: 9)).foregroundStyle(Color.white.opacity(0.4))
                }
            }
            .chartYAxis {
                AxisMarks { _ in
                    AxisValueLabel().font(.system(size: 9)).foregroundStyle(Color.white.opacity(0.4))
                    AxisGridLine().foregroundStyle(Color.white.opacity(0.05))
                }
            }
            .frame(height: 180)

        case .monthly:
            let data = store.monthlyData(months: 6)
            Chart {
                ForEach(data) { d in
                    BarMark(x: .value("Month", d.label), y: .value("₹", d.amount))
                        .foregroundStyle(LinearGradient.accentGradient)
                        .cornerRadius(6)
                }
            }
            .chartXAxis {
                AxisMarks { _ in
                    AxisValueLabel().font(.system(size: 11)).foregroundStyle(Color.white.opacity(0.4))
                }
            }
            .chartYAxis {
                AxisMarks { _ in
                    AxisValueLabel().font(.system(size: 9)).foregroundStyle(Color.white.opacity(0.4))
                    AxisGridLine().foregroundStyle(Color.white.opacity(0.05))
                }
            }
            .frame(height: 180)
        }
    }
}

// MARK: - Donut Chart (custom Path-based, iOS 16 compatible)

struct DonutChart: View {
    let data: [CategorySpend]
    @State private var appear = false

    var body: some View {
        GeometryReader { geo in
            let size = min(geo.size.width, geo.size.height)
            let cx = geo.size.width / 2
            let cy = geo.size.height / 2
            let r = size / 2 - 10
            let lineW: CGFloat = 32

            ZStack {
                if data.isEmpty {
                    Circle().stroke(Color.bgCard, lineWidth: lineW)
                } else {
                    ForEach(0..<min(data.count, 8), id: \.self) { i in
                        let start = startAngle(i)
                        let end   = appear ? endAngle(i) : start
                        Path { p in
                            p.addArc(center: CGPoint(x: cx, y: cy), radius: r,
                                     startAngle: .degrees(start), endAngle: .degrees(end), clockwise: false)
                        }
                        .stroke(data[i].category.color,
                                style: StrokeStyle(lineWidth: lineW, lineCap: .butt))
                    }
                }
            }
            .onAppear {
                withAnimation(.easeOut(duration: 1.0)) { appear = true }
            }
        }
    }

    private func total() -> Double { data.reduce(0) { $0 + $1.amount } }
    private func startAngle(_ i: Int) -> Double {
        let t = total(); guard t > 0 else { return -90 }
        return data.prefix(i).reduce(0) { $0 + $1.amount } / t * 360 - 90
    }
    private func endAngle(_ i: Int) -> Double {
        let t = total(); guard t > 0 else { return -90 }
        return data.prefix(i+1).reduce(0) { $0 + $1.amount } / t * 360 - 90
    }
}

// MARK: - Category Bar Row

struct CategoryBar: View {
    let item: CategorySpend; let total: Double
    @EnvironmentObject var store: DataStore
    @State private var animate = false

    var body: some View {
        VStack(spacing: 8) {
            HStack {
                Text(item.category.emoji)
                Text(item.category.name).font(.system(size: 13, weight: .semibold)).foregroundColor(.white)
                Spacer()
                Text(store.formatted(item.amount)).font(.system(size: 13, weight: .bold)).foregroundColor(.white)
                Text("\(Int(item.percentage * 100))%").font(.system(size: 11)).foregroundColor(.textSecondary).frame(width: 34, alignment: .trailing)
            }
            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    RoundedRectangle(cornerRadius: 5).fill(Color.white.opacity(0.05)).frame(height: 7)
                    RoundedRectangle(cornerRadius: 5).fill(item.category.color)
                        .frame(width: animate ? geo.size.width * CGFloat(item.percentage) : 0, height: 7)
                        .animation(.easeOut(duration: 0.9).delay(0.05), value: animate)
                }
            }.frame(height: 7)
        }
        .onAppear { animate = true }
    }
}

#Preview {
    AnalyticsView().environmentObject(DataStore()).preferredColorScheme(.dark)
}
