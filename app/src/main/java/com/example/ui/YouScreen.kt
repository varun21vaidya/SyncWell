package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*
import com.example.viewmodel.SyncWellViewModel

/** Stats, Goals and Config as one tab: a segmented control on top, the existing screens unchanged underneath. */
@Composable
fun YouScreen(viewModel: SyncWellViewModel, segment: YouSegment, onSegmentChange: (YouSegment) -> Unit) {
    Column(Modifier.fillMaxSize().background(Background)) {
        YouSegmentBar(segment, onSegmentChange)
        Box(Modifier.weight(1f)) {
            when (segment) {
                YouSegment.STATS -> AnalyticsScreen(viewModel = viewModel)
                YouSegment.GOALS -> TodosScreen(viewModel = viewModel)
                YouSegment.CONFIG -> SettingsScreen(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouSegmentBar(segment: YouSegment, onSegmentChange: (YouSegment) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        YouSegment.values().forEachIndexed { i, s ->
            SegmentedButton(
                selected = segment == s, onClick = { onSegmentChange(s) },
                shape = SegmentedButtonDefaults.itemShape(i, YouSegment.values().size),
                modifier = Modifier.height(48.dp).testTag("you_segment_${s.name}"),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = ProgressDim, activeContentColor = Progress, activeBorderColor = Progress,
                    inactiveContainerColor = Surface2, inactiveContentColor = TextSecondary, inactiveBorderColor = Border
                )
            ) { Text(s.title, fontWeight = FontWeight.Bold) }
        }
    }
}
