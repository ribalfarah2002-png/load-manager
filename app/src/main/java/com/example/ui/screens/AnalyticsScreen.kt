package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.entities.AnalyticsLogEntity
import com.example.data.database.entities.DeviceEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.LoadManagerViewModel
import java.util.Locale

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnalyticsScreen(
    viewModel: LoadManagerViewModel,
    devices: List<DeviceEntity>,
    logs: List<AnalyticsLogEntity>
) {
    // Selection state trackers
    // level: 0 = Devices List, 1 = bimonthly periods list for selected, 2 = line charts show for period
    val (uiLevel, setUiLevel) = remember { mutableIntStateOf(0) }
    val (targetId, setTargetId) = remember { mutableIntStateOf(0) } // 0 = Total, others = Device IDs
    val (targetMonthGroup, setTargetMonthGroup) = remember { mutableStateOf("") }

    val monthGroups = listOf(
        "كانون الثاني - شباط",
        "آذار - نيسان",
        "أيار - حزيران",
        "تموز - آب",
        "أيلول - تشرين الأول",
        "تشرين الثاني - كانون الأول"
    )

    AnimatedContent(
        targetState = uiLevel,
        transitionSpec = {
            if (targetState > initialState) {
                slideInHorizontally { width -> width } + fadeIn() with
                        slideOutHorizontally { width -> -width } + fadeOut()
            } else {
                slideInHorizontally { width -> -width } + fadeIn() with
                        slideOutHorizontally { width -> width } + fadeOut()
            }
        },
        label = "AnalyticsNavigation"
    ) { level ->
        when (level) {
            0 -> {
                // LEVEL 0: Master Widgets Grid List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp)
                ) {
                    item {
                        Column {
                            Text(
                                text = "التحليلات والمطابقات المالية",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "اضغط على أي قسم لمعاينة تفاصيل استهلاك الأشهر السابقة",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                    }

                    // Card 1: Total Household Consumption widget (deviceId = 0)
                    val totalWhSum = devices.sumOf { it.energyWh }
                    item {
                        AnalyticsMasterWidgetCard(
                            title = "الاستهلاك الإجمالي للمنزل",
                            subtitle = "يشمل سخان المياه، المكيفات، الإنارة وكافة الأجهزة",
                            energyKWh = totalWhSum / 1000.0,
                            isTotal = true,
                            onClick = {
                                setTargetId(0)
                                setUiLevel(1)
                            }
                        )
                    }

                    // Cards 2+: Individual devices
                    items(devices) { device ->
                        AnalyticsMasterWidgetCard(
                            title = "استهلاك ${device.name}",
                            subtitle = "جهاز مفعل بالوضع ${if (device.isManualMode) "اليدوي" else "التلقائي المجدول"}",
                            energyKWh = device.energyWh / 1000.0,
                            isTotal = false,
                            onClick = {
                                setTargetId(device.id)
                                setUiLevel(1)
                            }
                        )
                    }
                }
            }
            1 -> {
                // LEVEL 1: Six Bimonthly Period Choice List
                val deviceName = if (targetId == 0) "الاستهلاك الكلي" else devices.find { it.id == targetId }?.name ?: "جهاز"
                
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            IconButton(
                                onClick = { setUiLevel(0) },
                                modifier = Modifier.background(CyberSlate, RoundedCornerShape(8.dp)).border(1.dp, BorderDark, RoundedCornerShape(8.dp))
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = ElectricCyan)
                            }
                            Column {
                                Text(
                                    text = "فترات استهلاك $deviceName",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "اختر دورة الفوترة لمعاينة استهلاك وحمل الطاقة المباشر",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    items(monthGroups) { group ->
                        // Filter logs for totals
                        val relevantLogs = logs.filter { it.deviceId == targetId && it.monthGroup == group }
                        val accumulatedKWh = relevantLogs.sumOf { it.kwh }
                        val accumulatedSyp = relevantLogs.sumOf { it.costSyp }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    setTargetMonthGroup(group)
                                    setUiLevel(2)
                                }
                                .testTag("monthly_group_${group}"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = CyberSlate),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = ElectricCyan)
                                
                                Column(
                                    horizontalAlignment = Alignment.End,
                                    modifier = Modifier.weight(1f).padding(end = 12.dp)
                                ) {
                                    Text(text = group, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = String.format(Locale.US, "المجموع: %.1f kWh | %,.0f ل.س", accumulatedKWh, accumulatedSyp),
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(ElectricCyan.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.DateRange, contentDescription = null, tint = ElectricCyan)
                                }
                            }
                        }
                    }
                }
            }
            2 -> {
                // LEVEL 2: Canvas Daily Line Chart + Daily Logs Reports list
                val deviceName = if (targetId == 0) "الاستهلاك الكلي" else devices.find { it.id == targetId }?.name ?: "جهاز"
                val filteredLogs = logs.filter { it.deviceId == targetId && it.monthGroup == targetMonthGroup }
                    .sortedBy { it.dayOfMonthGroup }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            IconButton(
                                onClick = { setUiLevel(1) },
                                modifier = Modifier.background(CyberSlate, RoundedCornerShape(8.dp)).border(1.dp, BorderDark, RoundedCornerShape(8.dp))
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = ElectricCyan)
                            }
                            Column {
                                Text(
                                    text = "$deviceName - خط استبياني",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = targetMonthGroup,
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    // Card encapsulating line chart canvas drawing
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("line_chart_card"),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = CyberSlate),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "تدرج منحني الاستهلاك اليومي (kWh)",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                // The Canvas drawing component
                                DailyAnalyticsLineChart(logs = filteredLogs)

                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("اليوم 60", color = TextSecondary, fontSize = 10.sp)
                                    Text("اليوم 30", color = TextSecondary, fontSize = 10.sp)
                                    Text("اليوم 1", color = TextSecondary, fontSize = 10.sp)
                                }
                            }
                        }
                    }

                    // Title: Daily Log updates
                    item {
                        Text(
                            text = "رسائل التلخيص اليومية ومتابعة الأعطال (كل 24 ساعة):",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    if (filteredLogs.isEmpty()) {
                        item {
                            Text(
                                text = "لا تتوفر رسائل سجلات يومية لهذه المرحلة المحددة.",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Display list of 24-hour log messages
                    items(filteredLogs) { log ->
                        DailyLogUpdateMessageCard(log = log)
                    }
                }
            }
        }
    }
}

@Composable
fun AnalyticsMasterWidgetCard(
    title: String,
    subtitle: String,
    energyKWh: Double,
    isTotal: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("analytics_master_${title}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSlate),
        border = BorderStroke(1.dp, if (isTotal) ElectricCyan.copy(alpha = 0.4f) else BorderDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = ElectricCyan)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(text = title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(text = subtitle, color = TextSecondary, fontSize = 11.sp, textAlign = TextAlign.Right)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = String.format(Locale.US, "%.3f kWh", energyKWh),
                        color = if (isTotal) ElectricTeal else ElectricCyan,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Timeline,
                        contentDescription = null,
                        tint = if (isTotal) ElectricTeal else ElectricCyan,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        if (isTotal) ElectricTeal.copy(alpha = 0.1f) else ElectricCyan.copy(alpha = 0.1f),
                        RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isTotal) Icons.Default.HomeWork else Icons.Default.PowerSettingsNew,
                    contentDescription = null,
                    tint = if (isTotal) ElectricTeal else ElectricCyan
                )
            }
        }
    }
}

// THE CUSTOM VECTOR LINE CHART CANVAS DRAWING
@Composable
fun DailyAnalyticsLineChart(
    logs: List<AnalyticsLogEntity>
) {
    val defaultPoints = (1..60).map { 0f } // fallback empty points
    val dataPoints = if (logs.isNotEmpty()) {
        logs.map { it.kwh.toFloat() }
    } else {
        defaultPoints
    }

    val maxVal = dataPoints.maxOrNull()?.coerceAtLeast(1f) ?: 10f

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
    ) {
        val width = size.width
        val height = size.height
        val stepX = width / (dataPoints.size - 1).coerceAtLeast(1)

        val path = Path()
        val fillPath = Path()

        // Starting point coordinates
        val firstY = height - (dataPoints[0] / maxVal) * height
        path.moveTo(0f, firstY)
        fillPath.moveTo(0f, height)
        fillPath.lineTo(0f, firstY)

        for (i in 1 until dataPoints.size) {
            val cx = i * stepX
            val cy = height - (dataPoints[i] / maxVal) * height
            path.lineTo(cx, cy)
            fillPath.lineTo(cx, cy)
        }
        fillPath.lineTo(width, height)
        fillPath.close()

        // Draw background grid lines (horizontal ticks representation)
        val gridLinesCount = 3
        for (g in 1..gridLinesCount) {
            val gy = (height / (gridLinesCount + 1)) * g
            drawLine(
                color = BorderDark.copy(alpha = 0.5f),
                start = Offset(0f, gy),
                end = Offset(width, gy),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Draw filled gradient under graph
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    ElectricCyan.copy(alpha = 0.35f),
                    ElectricCyan.copy(alpha = 0.0f)
                )
            )
        )

        // Draw line trace
        drawPath(
            path = path,
            color = ElectricCyan,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

// 24 HOUR TELEMETRY CARD LOGS
@Composable
fun DailyLogUpdateMessageCard(
    log: AnalyticsLogEntity
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CyberNavy),
        border = BorderStroke(1.dp, if (log.alertMsg != null) EnergyRed.copy(alpha = 0.4f) else BorderDark)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Warning indicator if log indicates overload limit exceeded or power spike occurred
                if (log.alertMsg != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = EnergyRed.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = EnergyRed, modifier = Modifier.size(10.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تنبيه عطل/زيادة استطاعة", color = EnergyRed, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = ElectricTeal.copy(alpha = 0.15f)
                    ) {
                        Text("تقرير مستقر", color = ElectricTeal, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                    }
                }

                Text(
                    text = "اليوم ${log.dayOfMonthGroup} من الدورة",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Message layout text block
            Text(
                text = "تقرير استهلاك الـ 24 ساعة الماضية: بلغت سعة الطاقة المستهلكة ${String.format(Locale.US, "%.3f", log.kwh)} kWh بكلفة إجمالية تقديرية ${String.format(Locale.US, "%,.0f", log.costSyp)} ليرة سورية. متوسط التدفق المباشر للتيار الحركي بلغ ${String.format(Locale.US, "%.2f", log.avgCurrent)} أمبير باستطاعة متوسطة ${String.format(Locale.US, "%.1f", log.avgPower)} واط.",
                color = Color.White,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )

            // Alert block if any exists
            log.alertMsg?.let { alert ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "الملاحظات: $alert",
                    color = EnergyRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
