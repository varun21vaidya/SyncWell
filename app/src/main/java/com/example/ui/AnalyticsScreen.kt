package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DailyAnalytics
import com.example.data.WeightLog
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import com.example.ui.theme.*
import com.example.viewmodel.SyncWellViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

data class ChartDataItem(
    val label: String,
    val studyMinutes: Int,
    val waterMl: Int,
    val weightKg: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(viewModel: SyncWellViewModel) {
    val weeklyAnalytics by viewModel.weeklyAnalytics.collectAsState(initial = emptyList())
    val monthlyAnalytics by viewModel.monthlyAnalytics.collectAsState(initial = emptyList())
    val weightLogs by viewModel.weightLogs.collectAsState(initial = emptyList())
    val analyticsLastRefreshed by viewModel.analyticsLastRefreshed.collectAsState()
    val activeRoutineItems by viewModel.activeRoutineItems.collectAsState()
    
    val isGymDoneToday = remember(activeRoutineItems) {
        activeRoutineItems.any { (it.title.contains("Gym", ignoreCase = true) || it.title.contains("Workout", ignoreCase = true)) && it.isDone }
    }

    val proteinItems = remember(activeRoutineItems) {
        activeRoutineItems.filter {
            it.title.contains("Breakfast", ignoreCase = true) ||
            it.title.contains("Lunch", ignoreCase = true) ||
            it.title.contains("Dinner", ignoreCase = true) ||
            it.title.contains("Protein", ignoreCase = true)
        }
    }
    val proteinPointsToday = proteinItems.count { it.isDone }
    val isDietDoneToday = proteinPointsToday >= 4

    var selectedPeriod by remember { mutableStateOf("DAILY") }
    var weightInput by remember { mutableStateOf("70.0") }
    val scope = rememberCoroutineScope()

    // FEATURE 13: Always refresh on screen mount
    LaunchedEffect(Unit) { viewModel.refreshAnalytics() }

    // Resolve analytics list: merge database values + weight logs with mock fallback data.
    val resolvedList = remember(monthlyAnalytics, weightLogs) {
        getResolvedAnalytics(monthlyAnalytics, weightLogs)
    }

    val aggregatedData = remember(resolvedList, selectedPeriod) {
        aggregateAnalytics(resolvedList, selectedPeriod)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Header Row (FEATURE 13)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "CONSOLIDATED PERFORMANCE METRICS",
                    color = Slate400,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Executive Statistics",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black
                )
                // FEATURE 13: Last refreshed timestamp
                analyticsLastRefreshed?.let { ts ->
                    val time = java.text.SimpleDateFormat("HH:mm", java.util.Locale.US).format(java.util.Date(ts))
                    Text("Last updated: $time", color = Slate400, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
                }
            }
            IconButton(
                onClick = { viewModel.refreshAnalytics() },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh Analytics", tint = Emerald500)
            }
        }

        // Gym Streak Card
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = if (isGymDoneToday) EmeraldDim else SurfaceTier1),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, OledBorder)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Gym Streak",
                        tint = if (isGymDoneToday) EmeraldGlow else TextMuted,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "GYM STREAK",
                        color = Slate400,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = if (isGymDoneToday) "🔥 Active" else "Pending",
                        color = if (isGymDoneToday) EmeraldGlow else MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = if (isDietDoneToday) EmeraldDim else SurfaceTier1),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, OledBorder)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Diet Streak",
                        tint = if (isDietDoneToday) EmeraldGlow else TextMuted,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "DIET STREAK",
                        color = Slate400,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = if (isDietDoneToday) "🔥 Active" else "$proteinPointsToday/4 Pts",
                        color = if (isDietDoneToday) EmeraldGlow else MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        // Period Aggregation Selector (Segmented buttons)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceTier1)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val periods = listOf("DAILY", "WEEKLY", "MONTHLY")
            periods.forEach { period ->
                val isSelected = selectedPeriod == period
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) EmeraldGlow else Color.Transparent)
                        .clickable { selectedPeriod = period }
                        .defaultMinSize(minHeight = 44.dp)
                        .padding(vertical = 10.dp)
                        .testTag("period_tab_$period"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = period,
                        color = if (isSelected) OledBlack else TextSecondary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Section A: SCALE BODY COMPOSITION SUBMISSION TARGET (Daily period only)
        if (selectedPeriod == "DAILY") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Scales",
                            tint = Emerald500,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "REGISTER BODYWEIGHT MEASUREMENT",
                            color = Slate400,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    Text(
                        text = "Goal is strictly pinned at 70.0 kg. Log active weights regularly to track consistency vectors:",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                        fontSize = 12.sp
                    )

                    // BUG 4 FIX: Full-width stacked layout with loading + confirmation state
                    var isWeightLogging by remember { mutableStateOf(false) }
                    var weightLogSuccess by remember { mutableStateOf(false) }

                    OutlinedTextField(
                        value = weightInput,
                        onValueChange = { weightInput = it },
                        placeholder = { Text("70.0", fontSize = 14.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("analytics_weight_input"),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Emerald500),
                        label = { Text("Body Weight (kg)", fontSize = 11.sp) },
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            if (!isWeightLogging && !weightLogSuccess) {
                                val wt = weightInput.toDoubleOrNull() ?: 70.0
                                isWeightLogging = true
                                scope.launch {
                                    viewModel.logWeight(wt)
                                    kotlinx.coroutines.delay(800)
                                    isWeightLogging = false
                                    weightLogSuccess = true
                                    kotlinx.coroutines.delay(1500)
                                    weightLogSuccess = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (weightLogSuccess) Emerald700 else Emerald600
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("submit_weight_log"),
                        enabled = !isWeightLogging
                    ) {
                        if (isWeightLogging) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Saving...", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                        } else if (weightLogSuccess) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = "Saved", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Weight Logged!", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = "Log", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                                Text("Submit Log (70.0 kg Target)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    }
                }
            }
        }

        // Section B: STUDY WINDOW (Vertical Column Chart for WEEKLY / MONTHLY)
        if (selectedPeriod == "WEEKLY" || selectedPeriod == "MONTHLY") {
            val studyChartData = remember(resolvedList, selectedPeriod) {
                getChartData(resolvedList, selectedPeriod) { it.totalStudyMinutes }
            }
            val gymChartData = remember(resolvedList, selectedPeriod) {
                getChartData(resolvedList, selectedPeriod) { if (it.isGymCompleted) 100 else 0 }
            }
            val dietChartData = remember(resolvedList, selectedPeriod) {
                getChartData(resolvedList, selectedPeriod) { it.dietProteinPoints * 25 } // Scale to 100
            }

            VerticalColumnChart(title = "STUDY FOCUS (MINUTES)", data = studyChartData, color = PurpleAccent)
            VerticalColumnChart(title = "GYM PERFORMANCE (%)", data = gymChartData, color = Emerald500)
            VerticalColumnChart(title = "DIET PERFORMANCE (%)", data = dietChartData, color = GoldAccent)
        } else {
            // DAILY Study effort bar
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "STUDY EFFORT (DAILY MINUTES)",
                        color = Slate400,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        aggregatedData.forEach { chartItem ->
                            ActualBarRow(
                                label = chartItem.label,
                                value = chartItem.studyMinutes,
                                maxValue = 480.0f
                            )
                        }
                    }

                    Text(
                        text = "Target Focus: ~475 mins daily blocks",
                        color = PurpleAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Section C: WEIGHT HISTORY (strictly informational, no inputs for WEEKLY / MONTHLY)
        if (selectedPeriod == "WEEKLY" || selectedPeriod == "MONTHLY") {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("weight_history_card"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "WEIGHT HISTORY ($selectedPeriod TRENDS)",
                        color = Slate400,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    val weightData = remember(resolvedList, selectedPeriod) {
                        if (selectedPeriod == "WEEKLY") {
                            resolvedList.chunked(7).mapIndexed { index, chunk ->
                                val avg = chunk.mapNotNull { it.weightKg }.average()
                                "Week ${index + 1}" to if (avg.isNaN()) 70.0 else avg
                            }.takeLast(4)
                        } else {
                            resolvedList.chunked(7).mapIndexed { index, chunk ->
                                val avg = chunk.mapNotNull { it.weightKg }.average()
                                "Week ${index + 1}" to if (avg.isNaN()) 70.0 else avg
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        weightData.forEach { (label, weight) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = label,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = String.format(java.util.Locale.US, "%.1f kg", weight),
                                    color = Emerald700,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section D: HYDRATION METERS (Cap daily target at 4 Liters)
        val maxWater = when (selectedPeriod) {
            "DAILY" -> 4000.0f
            "WEEKLY" -> 28000.0f
            "MONTHLY" -> 120000.0f
            else -> 4000.0f
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "HYDRATION METERS ($selectedPeriod TRENDS)",
                    color = Slate400,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    aggregatedData.forEach { chartItem ->
                        ActualWaterMeter(
                            label = chartItem.label,
                            waterMl = chartItem.waterMl,
                            maxValue = maxWater
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VerticalColumnChart(title: String, data: List<Pair<String, Int>>, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = title,
                color = Slate400,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                val maxVal = Math.max(1, data.maxOfOrNull { it.second } ?: 1).toFloat()
                data.forEach { (label, value) ->
                    val fraction = value.toFloat() / maxVal
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.Bottom,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val formattedValue = if (value >= 60 && title.contains("STUDY")) "${value / 60}h" else "$value"
                        Text(
                            text = formattedValue,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = color,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Box(
                            modifier = Modifier
                                .width(18.dp)
                                .fillMaxHeight(fraction * 0.7f) // Scale to leave space for text
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(color)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

fun getChartData(list: List<DailyAnalytics>, period: String, selector: (DailyAnalytics) -> Int): List<Pair<String, Int>> {
    return if (period == "WEEKLY") {
        val last7 = list.takeLast(7)
        val daysOfWeekOrder = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = java.util.Calendar.getInstance()

        val dayMap = last7.associate { item ->
            val dayName = try {
                val date = sdf.parse(item.dateKey) ?: Date()
                cal.time = date
                val dayNum = cal.get(java.util.Calendar.DAY_OF_WEEK)
                val labels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                labels[dayNum - 1]
            } catch (e: Exception) {
                "Mon"
            }
            dayName to selector(item)
        }

        daysOfWeekOrder.map { dayName ->
            dayName to (dayMap[dayName] ?: 0)
        }
    } else {
        if (list.size >= 28) {
            val w1 = list.subList(0, 7).sumOf { selector(it) }
            val w2 = list.subList(7, 14).sumOf { selector(it) }
            val w3 = list.subList(14, 21).sumOf { selector(it) }
            val w4 = list.subList(21, list.size).sumOf { selector(it) }
            listOf("Week 1" to w1, "Week 2" to w2, "Week 3" to w3, "Week 4" to w4)
        } else {
            listOf("Week 1" to 0, "Week 2" to 0, "Week 3" to 0, "Week 4" to 0)
        }
    }
}

@Composable
fun ActualBarRow(label: String, value: Int, maxValue: Float) {
    val barFraction = Math.max(0.02f, Math.min(1.0f, value.toFloat() / maxValue))
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(55.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .height(14.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(SurfaceTier2)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(barFraction)
                    .clip(RoundedCornerShape(6.dp))
                    .background(PurpleAccent)
            )
        }

        val formattedText = if (value >= 60) "${value / 60}h ${value % 60}m" else "${value}m"
        Text(
            text = formattedText,
            color = PurpleAccent,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.width(60.dp)
        )
    }
}

@Composable
fun ActualWaterMeter(label: String, waterMl: Int, maxValue: Float) {
    val fraction = Math.max(0.02f, Math.min(1.0f, waterMl.toFloat() / maxValue))
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(55.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(SurfaceTier2)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Emerald500)
            )
        }

        Text(
            text = String.format(java.util.Locale.US, "%.1fL", waterMl / 1000f),
            color = Emerald700,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.width(50.dp)
        )
    }
}

fun getResolvedAnalytics(dbAnalytics: List<DailyAnalytics>, dbWeightLogs: List<WeightLog>): List<DailyAnalytics> {
    val weightMap = dbWeightLogs.associateBy { it.dateKey }

    // Solely return actual DB analytics, optionally filling in weight from weight logs if missing
    return dbAnalytics.map { dbItem ->
        val loggedWeight = weightMap[dbItem.dateKey]?.weightKg
        dbItem.copy(
            weightKg = loggedWeight ?: dbItem.weightKg
        )
    }.sortedBy { it.dateKey }
}



fun aggregateAnalytics(dataList: List<DailyAnalytics>, period: String): List<ChartDataItem> {
    if (dataList.isEmpty()) return emptyList()
    
    return when (period) {
        "DAILY" -> {
            // Take last 7 days sorted in order
            dataList.takeLast(7).map {
                val dateParts = it.dateKey.split("-")
                val shortLabel = if (dateParts.size >= 3) "${dateParts[1]}/${dateParts[2]}" else it.dateKey
                ChartDataItem(
                    label = shortLabel,
                    studyMinutes = it.totalStudyMinutes,
                    waterMl = it.totalWaterMl,
                    weightKg = it.weightKg ?: 70.0
                )
            }
        }
        "WEEKLY" -> {
            // Chunk historical days in weeks
            dataList.chunked(7).mapIndexed { index, chunk ->
                val avgWeight = chunk.mapNotNull { it.weightKg }.average()
                ChartDataItem(
                    label = "Week ${index + 1}",
                    studyMinutes = chunk.sumOf { it.totalStudyMinutes },
                    waterMl = chunk.sumOf { it.totalWaterMl },
                    weightKg = if (avgWeight.isNaN()) 70.0 else avgWeight
                )
            }.takeLast(5)
        }
        "MONTHLY" -> {
            val grouped = dataList.groupBy { it.dateKey.substring(0, 7) }
            grouped.map { (monthKey, items) ->
                val monthParts = monthKey.split("-")
                val monthLabel = when (monthParts.getOrNull(1)) {
                    "01" -> "Jan"
                    "02" -> "Feb"
                    "03" -> "Mar"
                    "04" -> "Apr"
                    "05" -> "May"
                    "06" -> "Jun"
                    "07" -> "Jul"
                    "08" -> "Aug"
                    "09" -> "Sep"
                    "10" -> "Oct"
                    "11" -> "Nov"
                    "12" -> "Dec"
                    else -> monthKey
                }
                val avgWeight = items.mapNotNull { it.weightKg }.average()
                ChartDataItem(
                    label = monthLabel,
                    studyMinutes = items.sumOf { it.totalStudyMinutes },
                    waterMl = items.sumOf { it.totalWaterMl },
                    weightKg = if (avgWeight.isNaN()) 70.0 else avgWeight
                )
            }.takeLast(4)
        }
        else -> emptyList()
    }
}
