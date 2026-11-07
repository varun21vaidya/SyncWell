package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

private data class HelpStep(val title: String, val body: String, val button: String)

private val HELP_STEPS = listOf(
    HelpStep("Your day, built from routines", "Today's list shows every routine scheduled for today. Tick items off as you go.", "Routine tab"),
    HelpStep("Open Manage routines", "Tap MANAGE ROUTINES at the top of the Routine tab. Everything below is done from there.", "MANAGE ROUTINES"),
    HelpStep("Change just today", "Under TODAY'S LIST use Edit, ↑ ↓ and ✕, or add one extra item. This only changes today.", "+ Add item to today"),
    HelpStep("Create your own routine", "Tap + New routine. Give it a name, then choose when it repeats: Once on a date, Daily, or Custom days of the week.", "+ New routine"),
    HelpStep("Add items to it", "Open a routine and tap + Add item. Set a title, an optional time and a category. DIET and WORKOUT items count toward your streaks, and SUPPLEMENT items get a quick-log button.", "+ Add item"),
    HelpStep("Apply it, or start over", "When you leave a routine you can apply the changes to today. \"Reset starter routines\" restores the defaults, and Settings > Re-run setup asks the questions again.", "Apply to today?")
)

/** Swipeable how-to for building and editing routines, opened from the "i" button on the Routine tab. */
@Composable
fun RoutineHelpDialog(onDismiss: () -> Unit) {
    val pager = rememberPagerState { HELP_STEPS.size }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("How routines work", fontWeight = FontWeight.Black) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                HorizontalPager(pager, Modifier.fillMaxWidth().height(240.dp)) { page ->
                    val step = HELP_STEPS[page]
                    Column(Modifier.fillMaxSize().padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.size(32.dp).clip(CircleShape).background(EmeraldDim), contentAlignment = Alignment.Center) {
                            Text("${page + 1}", color = EmeraldGlow, fontWeight = FontWeight.Black)
                        }
                        Text(step.title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(step.body, fontSize = 13.sp, color = TextSecondary, lineHeight = 19.sp)
                        Box(Modifier.clip(RoundedCornerShape(10.dp)).background(SurfaceTier2).padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Text(step.button, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EmeraldGlow)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(HELP_STEPS.size) { i ->
                        Box(Modifier.size(7.dp).clip(CircleShape).background(if (i == pager.currentPage) EmeraldGlow else Slate400.copy(alpha = 0.4f)))
                    }
                }
                Text("Swipe for the next step", fontSize = 10.sp, color = Slate400, modifier = Modifier.padding(top = 6.dp))
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("GOT IT", color = Emerald600, fontWeight = FontWeight.Black) } },
        shape = RoundedCornerShape(20.dp),
        containerColor = SurfaceTier1
    )
}
