package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.SyncWellViewModel

/** Stats, Goals or the Journey as a full screen with a back arrow, opened from the Home header. */
@Composable
fun OverlayScreen(overlay: Overlay, viewModel: SyncWellViewModel, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Background).testTag("overlay_${overlay.name}")) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("overlay_back")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Text(overlay.title, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Black)
        }
        Box(Modifier.weight(1f)) {
            when (overlay) {
                Overlay.STATS -> AnalyticsScreen(viewModel = viewModel)
                Overlay.GOALS -> TodosScreen(viewModel = viewModel)
                Overlay.JOURNEY -> JourneyScreen(viewModel = viewModel)
            }
        }
    }
}
