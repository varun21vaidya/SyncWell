package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.ui.theme.Background

/**
 * The tab bar plus one screen at a time. A Home shortcut opens Stats or Goals as a full screen that
 * replaces the tab content (it is not drawn over it, so taps and the screen reader cannot reach what is behind it).
 * The screens are passed in, so the navigation can be tested without the real ones.
 */
@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    deepLink: Destination? = null,
    onDeepLinkHandled: () -> Unit = {},
    home: @Composable (onNavigate: (Destination) -> Unit) -> Unit,
    routine: @Composable () -> Unit,
    workout: @Composable () -> Unit,
    focus: @Composable () -> Unit,
    settings: @Composable () -> Unit,
    overlayContent: @Composable (overlay: Overlay, onBack: () -> Unit) -> Unit
) {
    var selected by rememberSaveable { mutableStateOf(Tab.HOME) }
    var overlay by rememberSaveable { mutableStateOf<Overlay?>(null) }

    // A notification tap or link to Goals, Stats or Settings opens that full screen on top of its tab
    LaunchedEffect(deepLink) {
        deepLink?.let { selected = it.tab; overlay = it.overlay; onDeepLinkHandled() }
    }

    // Registered before any screen's own BackHandler, so a screen that handles Back itself (workout history, an open
    // routine item) is registered later and wins; this one only runs when nothing deeper wants the press.
    BackHandler(enabled = overlay != null || selected != Tab.HOME) {
        if (overlay != null) overlay = null else selected = Tab.HOME
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Background,
        bottomBar = { AppNavigationBar(selected) { selected = it; overlay = null } }
    ) { paddingValues ->
        Box(Modifier.fillMaxSize().padding(paddingValues)) {
            val open = overlay
            if (open != null) overlayContent(open) { overlay = null }
            else when (selected) {
                Tab.HOME -> home { d -> selected = d.tab; overlay = d.overlay }
                Tab.ROUTINE -> routine()
                Tab.WORKOUT -> workout()
                Tab.FOCUS -> focus()
                Tab.SETTINGS -> settings()
            }
        }
    }
}
