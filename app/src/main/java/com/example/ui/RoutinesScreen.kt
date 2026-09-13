package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DailyRoutineItem
import com.example.ui.theme.*
import com.example.viewmodel.SyncWellViewModel

data class RoutineDetailedInfo(
    val description: String,
    val longFormGuide: List<String>,
    val frequency: String,
    val targetMetrics: String
)

fun getRoutineDetailedGuide(item: DailyRoutineItem): RoutineDetailedInfo {
    val title = item.title.lowercase()
    val category = item.category.uppercase()
    return when {
        title.contains("skin") || title.contains("skincare") || category == "SKINCARE" -> {
            RoutineDetailedInfo(
                description = "Stable, high-quality skin routine with dermatologically proven ingredients. Avoids product switching and helps maintain skin barrier safety.",
                longFormGuide = listOf(
                    "Morning: Wash face with Simple Refreshing Facial Wash. Pat face dry gently.",
                    "Apply Minimalist 10% Niacinamide Serum evenly and wait for absorption.",
                    "Apply Deconstruct Oil-Free Moisturizer to establish hydration shield.",
                    "Apply Re'equil Ultra Matte Dry Touch Sunscreen before going outdoors.",
                    "Night: Cleansing with Minimalist 2% Salicylic Acid + LHA Cleanser.",
                    "Apply Niacinamide and Moisturizer. Optional 2% Salicylic Acid on nose/forehead (2-3 nights max)."
                ),
                frequency = "Twice Daily (Morning & Night)",
                targetMetrics = "Clear skin with reduced inflammation and stable product usage."
            )
        }
        title.contains("hair") || title.contains("haircare") || category == "HAIRCARE" -> {
            RoutineDetailedInfo(
                description = "Focused hair and scalp nutrition. Strengthens hair follicles, reduces scalp irritation, and avoids drying of wavy textures.",
                longFormGuide = listOf(
                    "Avoid using hot water. Rinse with cool or lukewarm water only.",
                    "Shampoo limited to 2 times/week max to avoid stripping natural oils.",
                    "Use conditioner on mid-lengths only, never on scalp.",
                    "Pat dry with a soft microfibre towel or cotton T-shirt without aggressive rubbing.",
                    "Detangle gently when wet. Use rosemary + lavender oil massaged into scalp at night on oiling days (2-3 times/week)."
                ),
                frequency = "Daily Care | 2-3x/week Oiling & 2x/week Shampooing",
                targetMetrics = "Reduced scalp irritation, reduced hair shedding, and healthy wave definition."
            )
        }
        title.contains("supplement") || title.contains("multivitamin") || title.contains("magnesium") || title.contains("omega") || title.contains("d3") -> {
            RoutineDetailedInfo(
                description = "Targeted vitamin and mineral assimilation supporting energy production, cognitive capacity, CNS health, and deeper sleep cycles.",
                longFormGuide = listOf(
                    "Breakfast: Take Ace Blend Multivitamin (1 capsule) & Naturaltein Omega-3 (1 softgel) with dietary fats.",
                    "Lunch: Take Carbamide Forte D3 + K2 (1 tablet) before 4:00 PM for maximum calcium absorption and melatonin protection.",
                    "Bedtime: Take Naturaltein Magnesium Glycinate (1 tablet) 45-60 min before sleep. Lowers core temperature and calms the central nervous system.",
                    "Always consume with actual food to maximize absorption of fat-soluble vitamins."
                ),
                frequency = "Daily with designated meals",
                targetMetrics = "Optimal micronutrient absorption, cognitive performance support, and improved deep sleep."
            )
        }
        title.contains("diet") || title.contains("meal") || title.contains("breakfast") || title.contains("lunch") || title.contains("dinner") || title.contains("protein") || title.contains("hydration") || title.contains("water") || title.contains("veg") -> {
            RoutineDetailedInfo(
                description = "Macro-nutrient integrity emphasizing high-protein, calorie compliance, clean hydration, and optimal digestion blocks.",
                longFormGuide = listOf(
                    "Protein Target: Reach 130–160 g daily across solid and liquid sources.",
                    "Morning: Drink 500 ml waking water immediately. Breakfast includes 2 whole eggs, 2 egg whites, and overnight soaked oats.",
                    "Lunch: Control rice portion, load up dal, add >=100g paneer, 1 bowl curd, and cucumber/carrot salads.",
                    "Evening: 1 scoop Whey Protein with water or milk and 1 fruit.",
                    "Dinner: Prioritize animal or high-quality vegetable protein (chicken, egg, chana, or mushroom paired with 50g soya chunks backup).",
                    "Saturday Veg-Only: No meat/chicken/fish. Maintain protein via paneer, dal, soya, curd, chana, eggs, and whey.",
                    "Digestion: Stay upright for 1 hour after dinner. Do a light 10-15 min post-meal walk."
                ),
                frequency = "Daily (Continuous Tracking)",
                targetMetrics = "Protein: 130-160 g | Water: 3.5-4 L | No lying down post-dinner"
            )
        }
        title.contains("gym") || title.contains("workout") || title.contains("walk") || title.contains("badminton") || category == "WORKOUT" -> {
            RoutineDetailedInfo(
                description = "Cardio-respiratory and weight progression training designed to maximize lean building and daily activity tracking.",
                longFormGuide = listOf(
                    "Gym Workouts: Push/Pull/Legs rotation, 4-5 times per week, taking 55-70 mins max.",
                    "Movement Goals: Brisk commute walk of 2.5 km from office daily.",
                    "Sunday Badminton: 8:00–10:00 AM (inclusive of 40-minute non-negotiable transport each way).",
                    "Saturday: Hometown bodyweight workout or 20 min HIIT / 40 min running.",
                    "Ensure a thorough 5-10 min dynamic warmup before lifting on compounds."
                ),
                frequency = "4-5x Weight training | Daily brisk walking | Sunday Badminton",
                targetMetrics = "Body weight: 73-74 kg | Daily movement target hit"
            )
        }
        title.contains("study") || title.contains("focus") || category == "STUDY" -> {
            RoutineDetailedInfo(
                description = "Academic rigor program aiming for peak focus blocks and consistent daily revision without cognitive exhaustion.",
                longFormGuide = listOf(
                    "Weekly Target: Achieve 55 hrs 20 mins total study time (Weekday average: 6h 10m, Weekend average: 12h 15m).",
                    "Morning Study Block: 9:10 AM - 11:30 AM (2h 20m) for hardest subjects and new concepts.",
                    "Evening Study Block: Post-work until 12:40 AM for practice problems and revision.",
                    "Focus Method: Use 50-minute fully isolated study blocks followed by 10-minute break recovery cycles.",
                    "Remove phone distractions. Keep workspace clean and organized."
                ),
                frequency = "Daily (High impact)",
                targetMetrics = "55h 20m weekly study target | Rested sleep transitions at 1:00 AM"
            )
        }
        else -> {
            RoutineDetailedInfo(
                description = "Core life productivity habit designated in the SyncWell lifestyle roadmap to support balanced wellness.",
                longFormGuide = listOf(
                    "Follow the top-down sequential routine in the daily checklist.",
                    "Check off items only upon execution. You can toggle/uncheck if done erroneously.",
                    "Maintain steady consistency; focus on daily repeatability over perfect schedules."
                ),
                frequency = "Daily / Weekly Designation",
                targetMetrics = "100% Habit adherence under vacation safety freeze rules."
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutinesScreen(viewModel: SyncWellViewModel) {
    val activeRoutineItems by viewModel.activeRoutineItems.collectAsState()
    val isVacationActive by viewModel.isVacationActive.collectAsState()

    val completedCount = activeRoutineItems.count { it.isDone }
    val totalCount = activeRoutineItems.size

    var selectedRoutineItem by remember { mutableStateOf<DailyRoutineItem?>(null) }
    var showCircadianWarning by remember { mutableStateOf(false) }
    var selectedSuppName by remember { mutableStateOf("") }

    val proteinConfirmationTask by viewModel.proteinConfirmationTask.collectAsState()

    BackHandler(enabled = selectedRoutineItem != null) {
        selectedRoutineItem = null
    }

    if (selectedRoutineItem != null) {
        // Detailed Routine Sub-view & Navigation
        RoutineDetailView(
            item = selectedRoutineItem!!,
            onBack = { selectedRoutineItem = null },
            onToggle = { 
                val current = selectedRoutineItem!!
                viewModel.toggleRoutineItem(current)
                // Keep selected item's state in sync
                val updated = current.copy(isDone = !current.isDone)
                selectedRoutineItem = updated
            },
            viewModel = viewModel,
            onLogSupplementWithWarning = { name ->
                selectedSuppName = name
                showCircadianWarning = true
            }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            // App Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "DAILY HABITS CHECKLIST",
                        color = Slate400,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Weekly Runbooks",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                // Checklist Counter Ring/Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceTier2)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "$completedCount / $totalCount DONE",
                        color = EmeraldGlow,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp
                    )
                }
            }

            // Vacation Mode Alert status
            if (isVacationActive) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = RoseDim.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, RoseCrimson.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Vacation active",
                            tint = RoseCrimson,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "VACATION MODE ON: Metrics safely frozen.",
                            color = RoseCrimson,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (activeRoutineItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "Empty",
                            tint = Slate400,
                            modifier = Modifier.size(64.dp)
                        )
                        Text(
                            text = "No routines spawned today yet.",
                            color = Slate400,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                }
            } else {
                // BUG 1 FIX: Sort uncompleted items at top, but use a stable list for LazyColumn
                // to avoid jarring scroll jumps.
                val sortedRoutineItems = remember(activeRoutineItems) {
                    activeRoutineItems.sortedBy { it.orderIndex }
                }
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(sortedRoutineItems, key = { it.id }) { item ->
                        RoutineItemRow(
                            item = item,
                            onCardClick = { selectedRoutineItem = item },
                            onCheckedChange = { viewModel.toggleRoutineItem(item) },
                            viewModel = viewModel,
                            onLogSupplementWithWarning = { name ->
                                selectedSuppName = name
                                showCircadianWarning = true
                            }
                        )
                    }

                    // Bottom: Migrated Dietary Supplements Chest section
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(20.dp),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "DIETARY SUPPLEMENTS CHEST",
                                    color = Slate400,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )

                                val supplementsList = listOf(
                                    "Ace Blend Multivitamin" to "Breakfast Supp",
                                    "Carbamide Forte D3+K2" to "Post-Lunch Supp",
                                    "Naturaltein Omega-3" to "Dietary Fat Supp",
                                    "Magnesium Glycinate" to "Bedtime Supp"
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    supplementsList.take(2).forEach { (name, label) ->
                                        Card(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    viewModel.addSupplement(name) {
                                                        selectedSuppName = name
                                                        showCircadianWarning = true
                                                    }
                                                }
                                                .testTag("supp_quick_log_$label"),
                                            colors = CardDefaults.cardColors(containerColor = SurfaceTier2),
                                            border = BorderStroke(1.dp, OledBorder)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(10.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(name.split(" ").last(), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                                                Text(label, color = Slate400, fontSize = 9.sp)
                                            }
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    supplementsList.drop(2).forEach { (name, label) ->
                                        Card(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    viewModel.addSupplement(name) {
                                                        selectedSuppName = name
                                                        showCircadianWarning = true
                                                    }
                                                }
                                                .testTag("supp_quick_log_$label"),
                                            colors = CardDefaults.cardColors(containerColor = SurfaceTier2),
                                            border = BorderStroke(1.dp, OledBorder)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(10.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(name.split(" ").last(), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                                                Text(label, color = Slate400, fontSize = 9.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Protein Intake Confirmation Dialog
    proteinConfirmationTask?.let { item ->
        AlertDialog(
            onDismissRequest = { viewModel.cancelProteinConfirmation() },
            title = {
                Text(
                    text = "Protein Intake Verification",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "Did you ensure adequate high-quality protein was consumed for this block (${item.title})?",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmProteinIntake(item, true) },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600)
                ) {
                    Text("Yes, Protein Taken", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.confirmProteinIntake(item, false) }
                ) {
                    Text("No / Skip Protein", color = Slate400)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Circular warning Circadian Dialog Alert past 4 PM
    if (showCircadianWarning) {
        AlertDialog(
            onDismissRequest = { showCircadianWarning = false },
            confirmButton = {
                Button(
                    onClick = { showCircadianWarning = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600)
                ) {
                    Text("Understood", color = MaterialTheme.colorScheme.onPrimary)
                }
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Circadian Warning Icon",
                    tint = GoldAccent,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    "Circadian Guard Trigger",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "Consuming $selectedSuppName past 4:00 PM interferes with natural melatonin synthesis. Consuming D3 with fats at breakfast tomorrow morning is highly recommended for safety and bone assimilation.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    lineHeight = 18.sp
                )
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
fun RoutineItemRow(
    item: DailyRoutineItem, 
    onCardClick: () -> Unit,
    onCheckedChange: () -> Unit,
    viewModel: SyncWellViewModel,
    onLogSupplementWithWarning: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
            .alpha(if (item.isDone) 0.6f else 1f)
            .testTag("routine_item_${item.orderIndex}"),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isDone) SurfaceTier1.copy(alpha = 0.7f) else SurfaceTier1
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, OledBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Checkbox Icon with 36dp Touch Target
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clickable { onCheckedChange() }
                            .testTag("toggle_checkbox_${item.orderIndex}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Completion Box",
                            tint = if (item.isDone) EmeraldGlow else TextMuted,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        // Title
                        Text(
                            text = item.title,
                            color = if (item.isDone) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (item.isDone) FontWeight.Medium else FontWeight.Bold,
                            fontSize = 14.sp,
                            textDecoration = if (item.isDone) TextDecoration.LineThrough else TextDecoration.None
                        )

                        // Timing & Category
                        Row(
                            modifier = Modifier.padding(top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (item.scheduledTime != null) {
                                Text(
                                    text = "⏱ " + item.scheduledTime,
                                    color = Slate400,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // Category Badge
                            val (bgColor, textColor) = getCategoryColors(item.category)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(bgColor)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = item.category,
                                    color = textColor,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Aligned Dietary Supplements directly below designated time blocks
            val titleLower = item.title.lowercase()
            val categoryUpper = item.category.uppercase()
            val scheduledTimeStr = item.scheduledTime
            val hour = scheduledTimeStr?.split(":")?.firstOrNull()?.toIntOrNull()

            val isDietOrSupp = categoryUpper == "DIET" || titleLower.contains("supplement") || titleLower.contains("magnesium") || titleLower.contains("d3") || titleLower.contains("omega") || titleLower.contains("multivitamin") || titleLower.contains("oats") || titleLower.contains("breakfast") || titleLower.contains("lunch") || titleLower.contains("brunch") || titleLower.contains("dinner")

            val hasBreakfastSupps = isDietOrSupp && ((hour != null && hour in 7..11) || titleLower.contains("breakfast") || titleLower.contains("morning"))
            val hasLunchSupps = isDietOrSupp && ((hour != null && hour in 12..16) || titleLower.contains("lunch") || titleLower.contains("brunch") || titleLower.contains("d3") || titleLower.contains("k2"))
            val hasBedtimeSupps = isDietOrSupp && ((hour != null && hour >= 20) || titleLower.contains("sleep") || titleLower.contains("night") || titleLower.contains("magnesium") || titleLower.contains("bedtime"))

            if (hasBreakfastSupps || hasLunchSupps || hasBedtimeSupps) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = OledBorder, thickness = 1.dp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "ALIGNED SYSTEM SUPPLEMENTS:",
                    color = Slate400,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                val matchedSupplements = when {
                    hasBreakfastSupps -> listOf("Ace Blend Multivitamin" to "Breakfast Supp", "Naturaltein Omega-3" to "Dietary Fat Supp")
                    hasLunchSupps -> listOf("Carbamide Forte D3+K2" to "Post-Lunch Supp")
                    else -> listOf("Magnesium Glycinate" to "Bedtime Supp")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    matchedSupplements.forEach { (name, label) ->
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    viewModel.addSupplement(name) {
                                        onLogSupplementWithWarning(name)
                                    }
                                },
                            colors = CardDefaults.cardColors(containerColor = SurfaceTier2),
                            border = BorderStroke(1.dp, OledBorder),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(name.split(" ").last(), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = TextPrimary)
                                Text(label, color = Slate400, fontSize = 8.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RoutineDetailView(
    item: DailyRoutineItem,
    onBack: () -> Unit,
    onToggle: () -> Unit,
    viewModel: SyncWellViewModel,
    onLogSupplementWithWarning: (String) -> Unit
) {
    val info = getRoutineDetailedGuide(item)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Back Navigation Bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("back_to_routines_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back to Routines Dashboard",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Routine Details",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val (bgColor, textColor) = getCategoryColors(item.category)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(bgColor)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = item.category.uppercase(),
                        color = textColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = item.title,
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (item.isDone) TextDecoration.LineThrough else TextDecoration.None
                )

                if (item.scheduledTime != null) {
                    Text(
                        text = "⏱ Scheduled: " + item.scheduledTime,
                        color = Slate400,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                HorizontalDivider(color = OledBorder, thickness = 1.dp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (item.isDone) "COMPLETED TODAY" else "PENDING COMPLETION",
                        color = if (item.isDone) Emerald600 else GoldAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )

                    Button(
                        onClick = onToggle,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (item.isDone) Slate700 else Emerald600
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.wrapContentWidth()
                    ) {
                        Text(
                            text = if (item.isDone) "Reset Active Progress" else "Check off Completed",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (item.isDone) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Guide / Description Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "AESTHETIC DESCRIPTION",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate400,
                    letterSpacing = 1.sp
                )
                Text(
                    text = info.description,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                HorizontalDivider(color = OledBorder, thickness = 1.dp)

                Text(
                    text = "LONG-FORM PROCESS GUIDE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate400,
                    letterSpacing = 1.sp
                )

                info.longFormGuide.forEachIndexed { i, pt ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "•",
                            color = Emerald600,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            modifier = Modifier.offset(y = (-1).dp)
                        )
                        Text(
                            text = pt,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Frequency & Metrics
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("FREQUENCY DETAILS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate400)
                        Text(info.frequency, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 2.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("OUTCOME TARGETS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate400)
                        Text(info.targetMetrics, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }
        }

        // Aligned Dietary Supplements in Detail View
        val titleLower = item.title.lowercase()
        val categoryUpper = item.category.uppercase()
        val scheduledTimeStr = item.scheduledTime
        val hour = scheduledTimeStr?.split(":")?.firstOrNull()?.toIntOrNull()

        val isDietOrSupp = categoryUpper == "DIET" || titleLower.contains("supplement") || titleLower.contains("magnesium") || titleLower.contains("d3") || titleLower.contains("omega") || titleLower.contains("multivitamin") || titleLower.contains("oats") || titleLower.contains("breakfast") || titleLower.contains("lunch") || titleLower.contains("brunch") || titleLower.contains("dinner")

        val hasBreakfastSupps = isDietOrSupp && ((hour != null && hour in 7..11) || titleLower.contains("breakfast") || titleLower.contains("morning"))
        val hasLunchSupps = isDietOrSupp && ((hour != null && hour in 12..16) || titleLower.contains("lunch") || titleLower.contains("brunch") || titleLower.contains("d3") || titleLower.contains("k2"))
        val hasBedtimeSupps = isDietOrSupp && ((hour != null && hour >= 20) || titleLower.contains("sleep") || titleLower.contains("night") || titleLower.contains("magnesium") || titleLower.contains("bedtime"))

        if (hasBreakfastSupps || hasLunchSupps || hasBedtimeSupps) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "ALIGNED SYSTEM SUPPLEMENTS",
                        color = Slate400,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    val matchedSupplements = when {
                        hasBreakfastSupps -> listOf("Ace Blend Multivitamin" to "Breakfast Supp", "Naturaltein Omega-3" to "Dietary Fat Supp")
                        hasLunchSupps -> listOf("Carbamide Forte D3+K2" to "Post-Lunch Supp")
                        else -> listOf("Magnesium Glycinate" to "Bedtime Supp")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        matchedSupplements.forEach { (name, label) ->
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        viewModel.addSupplement(name) {
                                            onLogSupplementWithWarning(name)
                                        }
                                    },
                                colors = CardDefaults.cardColors(containerColor = SurfaceTier2),
                                border = BorderStroke(1.dp, OledBorder),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(name.split(" ").last(), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
                                    Text(label, color = Slate400, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun getCategoryColors(category: String): Pair<Color, Color> {
    return when (category.uppercase()) {
        "DIET" -> AmberDim to AmberFlame
        "SKINCARE" -> EmeraldDim to EmeraldGlow
        "STUDY" -> VioletDim to VioletPulse
        "WORKOUT" -> Color(0xFF1E1B4B) to CyanLaser
        "HYGIENE" -> CyanDim to CyanLaser
        "HAIRCARE" -> RoseDim to RoseCrimson
        "PREP" -> SurfaceTier2 to TextSecondary
        "HEALTH" -> AmberDim to AmberFlame
        "SUPPLEMENT" -> AmberDim to AmberFlame
        else -> SurfaceTier2 to TextSecondary
    }
}
