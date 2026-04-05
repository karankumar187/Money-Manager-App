package com.moneymanager.app.ui.screens.analytics

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moneymanager.app.ui.theme.*
import com.moneymanager.app.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

enum class AnalyticsPeriod(val label: String) {
    DAILY("Daily"), WEEKLY("Weekly"), MONTHLY("Monthly")
}

data class ChartPoint(val label: String, val amount: Double)

@Composable
fun AnalyticsScreen(viewModel: MainViewModel) {
    val transactions by viewModel.transactions.collectAsState()
    val categories   by viewModel.categories.collectAsState()
    var period       by remember { mutableStateOf(AnalyticsPeriod.MONTHLY) }

    // ── Build chart data ────────────────────────────────────────────────────
    val barData: List<ChartPoint> = remember(transactions, period) {
        when (period) {
            AnalyticsPeriod.DAILY -> {
                val fmt = SimpleDateFormat("d MMM", Locale.getDefault())
                (13 downTo 0).map { daysAgo ->
                    val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -daysAgo) }
                    val label = fmt.format(cal.time)
                    val sum = transactions.filter {
                        val c = Calendar.getInstance().apply { time = it.date }
                        c.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR) &&
                        c.get(Calendar.YEAR) == cal.get(Calendar.YEAR)
                    }.sumOf { it.amount }
                    ChartPoint(label, sum)
                }
            }
            AnalyticsPeriod.WEEKLY -> {
                val fmt = SimpleDateFormat("d MMM", Locale.getDefault())
                (7 downTo 0).map { weeksAgo ->
                    val cal = Calendar.getInstance().apply { add(Calendar.WEEK_OF_YEAR, -weeksAgo) }
                    val weekNum = cal.get(Calendar.WEEK_OF_YEAR)
                    val year    = cal.get(Calendar.YEAR)
                    val label   = "W${weekNum}"
                    val sum = transactions.filter {
                        val c = Calendar.getInstance().apply { time = it.date }
                        c.get(Calendar.WEEK_OF_YEAR) == weekNum && c.get(Calendar.YEAR) == year
                    }.sumOf { it.amount }
                    ChartPoint(label, sum)
                }
            }
            AnalyticsPeriod.MONTHLY -> {
                val fmt = SimpleDateFormat("MMM", Locale.getDefault())
                (5 downTo 0).map { monthsAgo ->
                    val cal = Calendar.getInstance().apply { add(Calendar.MONTH, -monthsAgo) }
                    val month = cal.get(Calendar.MONTH)
                    val year  = cal.get(Calendar.YEAR)
                    val label = fmt.format(cal.time)
                    val sum = transactions.filter {
                        val c = Calendar.getInstance().apply { time = it.date }
                        c.get(Calendar.MONTH) == month && c.get(Calendar.YEAR) == year
                    }.sumOf { it.amount }
                    ChartPoint(label, sum)
                }
            }
        }
    }

    // Category spend for this period
    val catSpend: List<Pair<String, Double>> = remember(transactions, categories, period) {
        val filtered = transactions.filter { tx ->
            val c = Calendar.getInstance().apply { time = tx.date }
            val now = Calendar.getInstance()
            when (period) {
                AnalyticsPeriod.DAILY   -> c.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR) && c.get(Calendar.YEAR) == now.get(Calendar.YEAR)
                AnalyticsPeriod.WEEKLY  -> c.get(Calendar.WEEK_OF_YEAR) == now.get(Calendar.WEEK_OF_YEAR) && c.get(Calendar.YEAR) == now.get(Calendar.YEAR)
                AnalyticsPeriod.MONTHLY -> c.get(Calendar.MONTH) == now.get(Calendar.MONTH) && c.get(Calendar.YEAR) == now.get(Calendar.YEAR)
            }
        }
        filtered.groupBy { it.categoryName }
            .mapValues { it.value.sumOf { t -> t.amount } }
            .entries.sortedByDescending { it.value }
            .map { Pair(it.key, it.value) }
    }

    val totalSpent = catSpend.sumOf { it.second }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(BgPrimary)
            .systemBarsPadding(),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        // ── Header ──────────────────────────────────────────────────────────
        item {
            Text(
                "Analytics",
                fontSize = 30.sp, fontWeight = FontWeight.Bold, color = TextPrimary,
                modifier = Modifier.padding(horizontal = 24.dp).padding(top = 16.dp, bottom = 24.dp)
            )
        }

        // ── Period toggle — capsule segmented control ─────────────────────
        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp)
                    .clip(CircleShape)
                    .background(BgCard)
                    .padding(4.dp)
            ) {
                Row(Modifier.fillMaxWidth()) {
                    AnalyticsPeriod.values().forEach { p ->
                        val isSelected = period == p
                        Box(
                            Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) Brush.linearGradient(listOf(Accent1, Color(0xFFFF6B35)))
                                    else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                                )
                                .clickable { period = p },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                p.label,
                                fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) Color.White else TextSecondary
                            )
                        }
                    }
                }
            }
        }

        // ── Bar Chart ────────────────────────────────────────────────────────
        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(BgCard)
                    .border(BorderStroke(0.6.dp, Color.White.copy(0.055f)), RoundedCornerShape(22.dp))
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        when (period) {
                            AnalyticsPeriod.DAILY   -> "Last 14 Days"
                            AnalyticsPeriod.WEEKLY  -> "Last 8 Weeks"
                            AnalyticsPeriod.MONTHLY -> "Last 6 Months"
                        },
                        fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary
                    )
                    BarChart(data = barData, modifier = Modifier.fillMaxWidth().height(180.dp))
                }
            }
        }

        // ── Donut + Category breakdown ────────────────────────────────────
        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 20.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(BgCard)
                    .border(BorderStroke(0.6.dp, Color.White.copy(0.055f)), RoundedCornerShape(22.dp))
                    .padding(20.dp)
            ) {
                Column(
                    Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Donut
                    val catColors = catSpend.mapIndexed { i, (name, _) ->
                        val cat = categories.firstOrNull { it.name == name }
                        hexColor(cat?.colorHex ?: catColorHex(i))
                    }
                    Box(Modifier.size(200.dp), contentAlignment = Alignment.Center) {
                        DonutChart(
                            data      = catSpend.map { it.second },
                            colors    = catColors,
                            modifier  = Modifier.fillMaxSize()
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Spent", fontSize = 11.sp, color = TextSecondary)
                            Text(
                                viewModel.formatted(totalSpent),
                                fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary
                            )
                        }
                    }

                    // Category bars
                    if (catSpend.isEmpty()) {
                        Text("No data for this period", fontSize = 14.sp, color = TextSecondary)
                    } else {
                        Column(
                            Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            catSpend.take(8).forEachIndexed { i, (name, amount) ->
                                val cat   = categories.firstOrNull { it.name == name }
                                val color = hexColor(cat?.colorHex ?: catColorHex(i))
                                val pct   = if (totalSpent > 0) amount / totalSpent else 0.0
                                CategoryBarRow(
                                    emoji  = cat?.emoji ?: "📦",
                                    name   = name,
                                    amount = viewModel.formatted(amount),
                                    pct    = pct,
                                    color  = color
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Bar Chart (Canvas) ────────────────────────────────────────────────────────
@Composable
private fun BarChart(data: List<ChartPoint>, modifier: Modifier = Modifier) {
    if (data.isEmpty()) return
    val maxVal = data.maxOf { it.amount }.coerceAtLeast(1.0)

    Canvas(modifier) {
        val barWidth  = (size.width - 16.dp.toPx()) / data.size
        val maxHeight = size.height - 28.dp.toPx()

        data.forEachIndexed { i, point ->
            val barH  = (point.amount / maxVal * maxHeight).toFloat().coerceAtLeast(2f)
            val left  = i * barWidth + barWidth * 0.15f
            val right = left + barWidth * 0.7f
            val top   = size.height - 24.dp.toPx() - barH

            // Red gradient bar
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFFF6B35), Color(0xFFFF3B30)),
                    start  = Offset(left, top),
                    end    = Offset(left, size.height)
                ),
                topLeft     = Offset(left, top),
                size        = Size(right - left, barH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
            )
        }

        // X-axis labels — draw every other one to avoid crowding
        val step = if (data.size > 8) 3 else if (data.size > 5) 2 else 1
        data.forEachIndexed { i, point ->
            if (i % step == 0) {
                drawContext.canvas.nativeCanvas.drawText(
                    point.label,
                    i * barWidth + barWidth / 2,
                    size.height,
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.argb(100, 255, 255, 255)
                        textSize = 22f
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                )
            }
        }
    }
}

// ── Donut Chart (Canvas arc) ──────────────────────────────────────────────────
@Composable
private fun DonutChart(data: List<Double>, colors: List<Color>, modifier: Modifier = Modifier) {
    val total = data.sum().coerceAtLeast(1.0)
    val animPct by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "donut"
    )

    Canvas(modifier) {
        val stroke    = 32.dp.toPx()
        val inset     = stroke / 2 + 10.dp.toPx()
        val arcSize   = Size(size.width - inset * 2, size.height - inset * 2)
        var startAngle = -90f

        if (data.isEmpty()) {
            drawArc(
                color       = BgCardAlt,
                startAngle  = 0f,
                sweepAngle  = 360f,
                useCenter   = false,
                topLeft     = Offset(inset, inset),
                size        = arcSize,
                style       = Stroke(stroke, cap = StrokeCap.Butt)
            )
        } else {
            data.forEachIndexed { i, amount ->
                val sweep = (amount / total * 360f * animPct).toFloat()
                drawArc(
                    color      = colors.getOrElse(i) { Accent1 },
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter  = false,
                    topLeft    = Offset(inset, inset),
                    size       = arcSize,
                    style      = Stroke(stroke, cap = StrokeCap.Butt)
                )
                startAngle += sweep
            }
        }
    }
}

// ── Category bar row (matches iOS CategoryBar) ────────────────────────────────
@Composable
private fun CategoryBarRow(emoji: String, name: String, amount: String, pct: Double, color: Color) {
    var animated by remember { mutableStateOf(false) }
    val animPct  by animateFloatAsState(
        targetValue  = if (animated) pct.toFloat() else 0f,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label        = "bar"
    )
    LaunchedEffect(Unit) { animated = true }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 16.sp)
            Spacer(Modifier.width(8.dp))
            Text(name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, modifier = Modifier.weight(1f))
            Text(amount, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.width(8.dp))
            Text("${(pct * 100).toInt()}%", fontSize = 11.sp, color = TextSecondary, modifier = Modifier.width(34.dp))
        }
        BoxWithConstraints(Modifier.fillMaxWidth().height(7.dp)) {
            Box(
                Modifier.fillMaxWidth().height(7.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color.White.copy(0.05f))
            )
            Box(
                Modifier.width(maxWidth * animPct).height(7.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(color)
            )
        }
    }
}

private fun catColorHex(i: Int): String {
    val colors = listOf("#FF9F0A","#7B61FF","#5AC8FA","#30D158","#FF453A","#FFD60A","#FF375F","#636366")
    return colors[i % colors.size]
}
