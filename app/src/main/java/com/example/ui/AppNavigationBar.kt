package com.example.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

private fun iconFor(tab: Tab): ImageVector = when (tab) {
    Tab.HOME -> Icons.Default.Home
    Tab.ROUTINE -> Icons.Default.List
    Tab.WORKOUT -> Icons.Default.Star
    Tab.FOCUS -> Icons.Default.PlayArrow
}

/** Four tabs. The selected one gets a mint icon and label on a Surface3 pill; the rest are muted. */
@Composable
fun AppNavigationBar(selected: Tab, onSelect: (Tab) -> Unit) {
    NavigationBar(containerColor = Surface1, tonalElevation = 0.dp, modifier = Modifier.testTag("app_navigation_bar")) {
        Tab.values().forEach { tab ->
            val isSelected = selected == tab
            NavigationBarItem(
                selected = isSelected,
                onClick = { onSelect(tab) },
                icon = { Icon(iconFor(tab), contentDescription = "${tab.title} tab", modifier = Modifier.size(if (isSelected) 24.dp else 22.dp)) },
                label = { Text(tab.title, fontSize = 11.sp, maxLines = 1, softWrap = false, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Progress, selectedTextColor = Progress,
                    unselectedIconColor = TextMuted, unselectedTextColor = TextMuted,
                    indicatorColor = Surface3
                ),
                modifier = Modifier.testTag("nav_tab_${tab.ordinal}")
            )
        }
    }
}
