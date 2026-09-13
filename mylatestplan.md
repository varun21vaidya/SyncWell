# SyncWell — Complete Bug Fix & OLED Dark Mode + Data-Dense Dashboard Revamp
> **Agent handoff document.** Every section is actionable from scratch. Line references point
> to the **current** file state. All code blocks are ready to copy-paste. Follow sections in
> the order listed.

---

## 0. Prerequisites & Audit Summary

### Current Codebase State (verified 2026-09-13)
| File | Lines | Key issues found |
|------|-------|-----------------|
| `TrackerScreen.kt` | 518 | Focus buttons (L270–L310) in a plain `Row`. `STUDY`/`WORK` share `weight(1f)` with NO `contentPadding` override → default Material3 horizontal padding (24 dp each side) eats all available space on ≤360 dp screens. `TIMEPASS` has `PaddingValues(horizontal=4.dp)` only, which is inconsistent and visually mismatched. |
| `MainActivity.kt` | 197 | No loading state at all. `_hasCompletedOnboarding` starts `false` (line 64 in ViewModel), so on cold start the `OnboardingScreen` renders immediately while the DB query runs asynchronously. Result: ~200–400 ms flash of onboarding UI on every cold start for existing users. |
| `AndroidManifest.xml` | 38 | `android:icon="@drawable/icon"` and `android:roundIcon="@drawable/icon"` (line 10–12). The drawable `icon.png` is **517 × 1024 px** — a 1:2 portrait rectangle. Android crops it into a 1:1 circle/squircle → severe horizontal squeeze. No `mipmap-*` dirs exist. |
| `TodosScreen.kt` | 232 | Three redundant `Spacer(8 dp)` + `HorizontalDivider` items between sections (lines 87–89) combined with `spacedBy(12.dp)` on the `LazyColumn` → ~52 dp dead gap per section break. `GoalItemRow` icon buttons are 48×48 dp for 16 dp icons. |
| `Color.kt` | 28 | No true OLED black token. Background uses `Slate50`/`RichBlack` (#121212), not #000000. |
| `Theme.kt` | 49 | `darkColorScheme` background = `RichBlack` (#121212), not #000000. Surface = `SurfaceDeep` (#1E1E1E). |
| `Type.kt` | 19 | Only `bodyLarge` defined. No tabular-figure variant for timer digits. |
| `RoutinesScreen.kt` | 907 | Cards use 20 dp padding. No category-color coded timeline. |
| `HomeScreen.kt` | 515 | Full-width single-column card stack. No bento grid. |
| `MainActivity.kt` nav | 197 | `NavigationBar` has 7 items — Material3 recommends max 5. Labels at 8 sp may violate minimum readable contrast. |

---

## 1. Bug Fix A — Focus Mode Button Responsiveness
**File:** `app/src/main/java/com/example/ui/TrackerScreen.kt`

### Root Cause (precise)
Lines **270–310**: three `Button` composables inside a `Row` all use `Modifier.weight(1f)`.
- `STUDY` and `WORK` buttons have **no `contentPadding` override**, so they inherit Material3's
  default `ButtonDefaults.ContentPadding` = `PaddingValues(horizontal = 24.dp, vertical = 8.dp)`.
  On a 360 dp screen the card has 20 dp inner padding on each side → usable width = 360 - 32 = 328 dp.
  Three equal weights = ~109 dp each. Minus 48 dp of horizontal padding per button = only **61 dp** for
  the text itself. "STUDY" at 11 sp ≈ 40 dp — fits. But when system **font scale ≥ 1.15×** or device
  is 320 dp wide the text is clipped or ellipsised.
- `TIMEPASS` uses `PaddingValues(horizontal=4.dp)` which is too small → text visually touches edges.
- The active-timer control `Row` (lines 313–386) puts Pause/Resume + Save Block + IconButton in one `Row`
  with `spacedBy(10.dp)` and both text buttons at `weight(1f)`. On small screens "Save Block" is clipped.

### Fix — Step-by-Step Code Changes

#### Step A1 — Replace Focus Category Buttons (L270–L310)
Delete lines 270–310 entirely. Replace with:

```kotlin
// ── Focus Category Selection ──────────────────────────────────────────────
BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
    val isNarrow = maxWidth < 340.dp
    if (isNarrow) {
        // Stacked 2-column grid for very small screens
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FocusCategoryButton(
                    label = "STUDY",
                    containerColor = PurpleAccent,
                    modifier = Modifier.weight(1f),
                    testTag = "boot_study_timer",
                    onClick = { viewModel.bootTimer("STUDY") }
                )
                FocusCategoryButton(
                    label = "WORK",
                    containerColor = Slate700,
                    modifier = Modifier.weight(1f),
                    testTag = "boot_work_timer",
                    onClick = { viewModel.bootTimer("WORK") }
                )
            }
            FocusCategoryButton(
                label = "TIMEPASS",
                containerColor = Slate400,
                modifier = Modifier.fillMaxWidth(),
                testTag = "boot_timepass_timer",
                onClick = { viewModel.bootTimer("TIMEPASS") }
            )
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FocusCategoryButton(
                label = "STUDY",
                containerColor = PurpleAccent,
                modifier = Modifier.weight(1f),
                testTag = "boot_study_timer",
                onClick = { viewModel.bootTimer("STUDY") }
            )
            FocusCategoryButton(
                label = "WORK",
                containerColor = Slate700,
                modifier = Modifier.weight(1f),
                testTag = "boot_work_timer",
                onClick = { viewModel.bootTimer("WORK") }
            )
            FocusCategoryButton(
                label = "TIMEPASS",
                containerColor = Slate400,
                modifier = Modifier.weight(1f),
                testTag = "boot_timepass_timer",
                onClick = { viewModel.bootTimer("TIMEPASS") }
            )
        }
    }
}
```

#### Step A2 — Add `FocusCategoryButton` private composable
Add this private composable at the **bottom of TrackerScreen.kt** (after line 517, before the closing of the file):

```kotlin
@Composable
private fun FocusCategoryButton(
    label: String,
    containerColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    testTag: String = "",
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(44.dp)
            .testTag(testTag),
        colors = ButtonDefaults.buttonColors(containerColor = containerColor),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip
        )
    }
}
```

#### Step A3 — Fix Active Timer Controls (L313–L386)
Delete lines 313–386. Replace with:

```kotlin
} else {
    // ── Active Timer Controls ─────────────────────────────────────────────
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Pause / Resume — takes most weight
        if (isActiveTimerRunning) {
            Button(
                onClick = { viewModel.pauseTimer() },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("pause_timer_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Slate700),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "Pause",
                    fontSize = 12.sp,
                    color = Color.White,
                    maxLines = 1,
                    softWrap = false
                )
            }
        } else {
            Button(
                onClick = { viewModel.bootTimer(activeContextType!!) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("resume_timer_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Slate800),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "Resume",
                    fontSize = 12.sp,
                    color = Color.White,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        // Save Block
        Button(
            onClick = { viewModel.saveFocusSession() },
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("save_focus_session_button"),
            colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                "Save",
                fontSize = 12.sp,
                color = Color.White,
                maxLines = 1,
                softWrap = false
            )
        }

        // Reset (icon only, fixed width = 48 dp)
        IconButton(
            onClick = { viewModel.resetTimer() },
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .testTag("reset_timer_button")
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Reset Clock",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
```

**Why this works:** All three buttons now use `contentPadding = PaddingValues(horizontal = 6.dp)` — only
12 dp total horizontal padding per button instead of 48 dp. On 320 dp screens each weighted button gets
~87 dp width with only 12 dp eaten by padding → 75 dp available for text, well above the ~30 dp needed
for "Save" / "Pause".

---

## 2. Bug Fix B — Full-Screen Startup Loader
**Files:** `SyncWellViewModel.kt`, `MainActivity.kt`  
**NEW FILE:** `app/src/main/java/com/example/ui/SyncWellSplashScreen.kt`

### Root Cause (precise)
`SyncWellViewModel.kt` line 64: `_hasCompletedOnboarding = MutableStateFlow(false)`.  
`MainActivity.kt` line 67: `if (!hasCompletedOnboarding)` → renders `OnboardingScreen` immediately.  
`init {}` block (lines 174–189) launches a coroutine that calls `checkSetupStatus()` then `DayBoundaryManager.currentTargetDate.collect {}` in sequence. On cold start this takes ≥100 ms of Room disk I/O before `_hasCompletedOnboarding` is set to `true`. During those 100–400 ms the full OnboardingScreen renders, then abruptly disappears → **jarring flash**.

### Fix — Step-by-Step

#### Step B1 — Add `AppLaunchState` sealed class to `SyncWellViewModel.kt`
Insert at **line 38** (just before `class SyncWellViewModel`):

```kotlin
sealed class AppLaunchState {
    object Loading : AppLaunchState()
    object OnboardingRequired : AppLaunchState()
    object Ready : AppLaunchState()
}
```

#### Step B2 — Add `_appLaunchState` StateFlow to `SyncWellViewModel.kt`
Insert immediately after line 55 (after the `activeModal` StateFlow declaration):

```kotlin
// LAUNCH STATE — controls full-screen splash vs onboarding vs main app
private val _appLaunchState = MutableStateFlow<AppLaunchState>(AppLaunchState.Loading)
val appLaunchState: StateFlow<AppLaunchState> = _appLaunchState.asStateFlow()
```

#### Step B3 — Modify `checkSetupStatus()` to set launch state
In `checkSetupStatus()` (lines 192–220), change the existing `if (settings != null)` block:

**Before (lines 209–219):**
```kotlin
val settings = dao.getAppSettingsDirectly()
if (settings != null) {
    _hasCompletedOnboarding.value = true
    _theme.value = settings.themeMode
    ...
} else {
    _hasCompletedOnboarding.value = false
}
```

**After:**
```kotlin
val settings = dao.getAppSettingsDirectly()
if (settings != null) {
    _hasCompletedOnboarding.value = true
    _theme.value = settings.themeMode
    _backupEnabled.value = settings.backupEnabled
    _workoutLocationMode.value = settings.workoutLocationMode
    _user.value = settings.firebaseUid ?: "Offline_User"
    _appLaunchState.value = AppLaunchState.Ready          // ← ADD THIS
} else {
    _hasCompletedOnboarding.value = false
    _appLaunchState.value = AppLaunchState.OnboardingRequired  // ← ADD THIS
}
```

#### Step B4 — Create `SyncWellSplashScreen.kt` (NEW FILE)
Create file at `app/src/main/java/com/example/ui/SyncWellSplashScreen.kt`:

```kotlin
package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Emerald500

@Composable
fun SyncWellSplashScreen() {
    // Breathing pulse animation for the logo
    val infiniteTransition = rememberInfiniteTransition(label = "splash_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    // Full-screen OLED black background — fills entire display
    Box(
        modifier = Modifier
            .fillMaxSize()                        // CRITICAL: must be fillMaxSize
            .background(Color(0xFF000000)),       // True OLED black
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Outer glow ring
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(pulseScale)
                    .background(
                        Emerald500.copy(alpha = glowAlpha * 0.18f),
                        shape = RoundedCornerShape(28.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Inner logo box
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(Emerald500, shape = RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "SyncWell Logo",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            // App name
            Text(
                text = "SyncWell",
                color = Color(0xFFF8FAFC),
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp
            )

            // Status line
            Text(
                text = "Aligning circadian runbooks...",
                color = Color(0xFF64748B),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(glowAlpha)
            )
        }
    }
}
```

#### Step B5 — Update `MainActivity.kt` to use `AppLaunchState`
Replace the entire `setContent { }` block (lines 60–101) with:

```kotlin
setContent {
    val theme by viewModel.theme.collectAsState()
    val appLaunchState by viewModel.appLaunchState.collectAsState()
    val activeModal by viewModel.activeModal.collectAsState()

    MyApplicationTheme(darkTheme = (theme == "dark")) {
        Box(modifier = Modifier.fillMaxSize()) {

            // ── Route based on launch state ──────────────────────────────
            when (appLaunchState) {
                is AppLaunchState.Loading -> {
                    // Full-screen splash — no Scaffold, no padding
                    SyncWellSplashScreen()
                }
                is AppLaunchState.OnboardingRequired -> {
                    OnboardingScreen(viewModel = viewModel)
                }
                is AppLaunchState.Ready -> {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        MainNavigationContainer(
                            viewModel = viewModel,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }

            // Global Restore Modal (shown over any state if triggered)
            if (activeModal == "RESTORE_CONFIRMATION") {
                AlertDialog(
                    onDismissRequest = { viewModel.setActiveModal(null) },
                    title = {
                        Text(
                            "Cloud Backup Found",
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Black
                        )
                    },
                    text = {
                        Text(
                            "We found an existing backup linked to your account. " +
                            "Would you like to restore your data (Todos and Weight Logs) now?"
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = { viewModel.restoreDataFromCloud() }) {
                            Text(
                                "RESTORE NOW",
                                color = com.example.ui.theme.Emerald600,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Black
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.setActiveModal(null) }) {
                            Text("CANCEL", color = com.example.ui.theme.Slate400)
                        }
                    },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface
                )
            }
        }
    }
}
```

Also add this import to `MainActivity.kt` imports section:
```kotlin
import com.example.viewmodel.AppLaunchState
import com.example.ui.SyncWellSplashScreen
```

**Why this works:** The `SyncWellSplashScreen` composable uses `Modifier.fillMaxSize()` with no parent
`Scaffold` or `Padding` wrapper — it has full viewport ownership. It shows until the DB query completes
and `_appLaunchState` transitions away from `Loading`. The key missing piece before was rendering the
Onboarding screen (with its `Scaffold` and padding) instead of a raw full-screen `Box`.

---

## 3. Bug Fix C — App Icon Aspect Ratio
**Files:** `AndroidManifest.xml`, new XML files, new PNG assets

### Root Cause (precise)
`icon.png` = 517 × 1024 px (portrait, 1:1.98). Both `android:icon` and `android:roundIcon` in
`AndroidManifest.xml` point to `@drawable/icon`. Android launcher applies a 1:1 circular or squircle
crop → the 517 px wide image is squeezed into a 1024 px square container → everything appears squished
to ~50% horizontal width.

### Fix — Step-by-Step

#### Step C1 — Generate Square Icon Asset
Run this Python script at `D:\coding journey\projects\SyncWell\fix_icon.py`:

```python
from PIL import Image
import os

src = "app/src/main/res/drawable/icon.png"
img = Image.open(src).convert("RGBA")

# Compute 1:1 square from the source image
w, h = img.size
side = max(w, h)  # use the larger dimension as the canvas

# Create a square transparent canvas
canvas = Image.new("RGBA", (side, side), (0, 0, 0, 0))
offset_x = (side - w) // 2
offset_y = (side - h) // 2
canvas.paste(img, (offset_x, offset_y))

# Resize to standard 1024x1024
canvas = canvas.resize((1024, 1024), Image.LANCZOS)

# Output directories and sizes
sizes = {
    "mipmap-mdpi":   48,
    "mipmap-hdpi":   72,
    "mipmap-xhdpi":  96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi":192,
}
for dir_name, size in sizes.items():
    out_dir = f"app/src/main/res/{dir_name}"
    os.makedirs(out_dir, exist_ok=True)
    resized = canvas.resize((size, size), Image.LANCZOS)
    resized.save(f"{out_dir}/ic_launcher.png", "PNG")
    resized.save(f"{out_dir}/ic_launcher_round.png", "PNG")
    print(f"Saved {size}x{size} → {out_dir}/")

# Also save full 1024 for reference
canvas.save("app/src/main/res/drawable/ic_launcher_hires.png", "PNG")
print("Done. Run: python fix_icon.py from project root.")
```

Run: `python fix_icon.py`

#### Step C2 — Create Adaptive Icon XML files

**File:** `app/src/main/res/drawable/ic_launcher_background.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="#000000" />
</shape>
```

**File:** `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@mipmap/ic_launcher_foreground" />
</adaptive-icon>
```

**File:** `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@mipmap/ic_launcher_foreground" />
</adaptive-icon>
```

For each density folder created in Step C1, copy `ic_launcher.png` as `ic_launcher_foreground.png`:
```
mipmap-mdpi/ic_launcher_foreground.png    (48×48)
mipmap-hdpi/ic_launcher_foreground.png    (72×72)
mipmap-xhdpi/ic_launcher_foreground.png   (96×96)
mipmap-xxhdpi/ic_launcher_foreground.png  (144×144)
mipmap-xxxhdpi/ic_launcher_foreground.png (192×192)
```

#### Step C3 — Update `AndroidManifest.xml`
Replace lines 10–12:
```xml
android:icon="@drawable/icon"
android:label="@string/app_name"
android:roundIcon="@drawable/icon"
```
With:
```xml
android:icon="@mipmap/ic_launcher"
android:label="@string/app_name"
android:roundIcon="@mipmap/ic_launcher_round"
```

Create the `mipmap-anydpi-v26` directory:
`app/src/main/res/mipmap-anydpi-v26/` (create this folder manually in Android Studio or via mkdir)

---

## 4. Bug Fix D — Task Screen Extra Spacing
**File:** `app/src/main/java/com/example/ui/TodosScreen.kt`

### Root Cause (precise)
Lines 87–89 in the `LazyColumn`:
```kotlin
item { Spacer(modifier = Modifier.height(8.dp)) }
item { HorizontalDivider(color = Slate100) }
item { Spacer(modifier = Modifier.height(8.dp)) }
```
Combined with `verticalArrangement = Arrangement.spacedBy(12.dp)` → 12 + 8 + divider + 8 + 12 = **52 dp gap**.

`GoalItemRow` (line 200): `Box(modifier = Modifier.size(48.dp))` for the checkbox → 48 dp consumed
for a 24 dp icon. Same for edit (line 221) and delete (line 225) IconButtons — each `Modifier.size(48.dp)`.
Three × 48 dp = 144 dp of row width lost to oversized touch targets.

### Fix — Step-by-Step

#### Step D1 — Fix LazyColumn arrangement
Line 41: change `verticalArrangement = Arrangement.spacedBy(12.dp)` to `Arrangement.spacedBy(8.dp)`

#### Step D2 — Remove the triple spacer/divider block
Delete lines 87–89:
```kotlin
item { Spacer(modifier = Modifier.height(8.dp)) }
item { HorizontalDivider(color = Slate100) }
item { Spacer(modifier = Modifier.height(8.dp)) }
```
Replace with a single visual separator that takes only 1 dp of visual space:
```kotlin
item {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 4.dp),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
        thickness = 1.dp
    )
}
```

#### Step D3 — Compact `GoalItemRow` checkbox and action buttons
In `GoalItemRow` (lines 186–231):

Change the checkbox `Box` (line 200) from `Modifier.size(48.dp)` to:
```kotlin
Box(
    modifier = Modifier
        .size(36.dp)    // was 48.dp — 36 dp meets Android's 36 dp soft minimum
        .clickable { onToggle() },
    contentAlignment = Alignment.Center
)
```

Change edit `IconButton` (line 221) from `Modifier.size(48.dp)` to `Modifier.size(36.dp)`.
Change delete `IconButton` (line 225) from `Modifier.size(48.dp)` to `Modifier.size(36.dp)`.

---

## 5. OLED Dark Mode Design System
**Files:** `Color.kt`, `Theme.kt`, `Type.kt`

### 5.1 — Rewrite `Color.kt`
Replace the entire file content:

```kotlin
package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ── OLED Surfaces ──────────────────────────────────────────────────────────
val OledBlack         = Color(0xFF000000)  // Screen background (saves OLED power)
val SurfaceTier1      = Color(0xFF0A0C10)  // Cards / list items
val SurfaceTier2      = Color(0xFF12151F)  // Elevated cards / modal sheets
val SurfaceTier3      = Color(0xFF1C2234)  // Selected / interactive states
val OledBorder        = Color(0xFF242B3F)  // Subtle 1 dp card borders
val OledBorderActive  = Color(0xFF4F5B78)  // Focused / active borders

// ── Semantic Accent Colors ──────────────────────────────────────────────────
val EmeraldGlow   = Color(0xFF10B981)  // Health, routine done, hydration CTA
val EmeraldDim    = Color(0xFF064E3B)  // Emerald container / badge background
val Emerald400    = Color(0xFF34D399)  // Lighter emerald for progress highlights
val Emerald500    = Color(0xFF10B981)  // Alias for existing usages
val Emerald600    = Color(0xFF059669)  // Alias for existing usages
val Emerald700    = Color(0xFF047857)  // Alias for existing usages
val Emerald100    = Color(0xFFD1FAE5)  // Light badge background (light mode only)

val CyanLaser     = Color(0xFF06B6D4)  // Hydration, water intake
val CyanDim       = Color(0xFF164E63)  // Water container background

val VioletPulse   = Color(0xFF8B5CF6)  // Focus sessions, study mode
val VioletDim     = Color(0xFF3B1D6E)  // Focus container background
val PurpleAccent  = Color(0xFF8B5CF6)  // Alias for existing usages

val AmberFlame    = Color(0xFFF59E0B)  // Streak, warnings, gold badges
val AmberDim      = Color(0xFF78350F)  // Warning container background
val GoldAccent    = Color(0xFFF59E0B)  // Alias for existing usages

val RoseCrimson   = Color(0xFFF43F5E)  // High priority, workout, delete
val RoseDim       = Color(0xFF881337)  // Danger container background
val RoseAccent    = Color(0xFFF43F5E)  // Alias for existing usages

val IndigoAccent  = Color(0xFF4F46E5)  // Secondary brand color

// ── Text Hierarchy ───────────────────────────────────────────────────────────
val TextPrimary   = Color(0xFFF8FAFC)  // Headings — 95% white
val TextSecondary = Color(0xFF94A3B8)  // Sub-labels, captions
val TextMuted     = Color(0xFF64748B)  // Timestamps, placeholders
val TextInverse   = Color(0xFF000000)  // On bright accent buttons

// ── Legacy Slate Aliases (keep for backward compat) ──────────────────────────
val Slate50   = Color(0xFFF8FAFC)
val Slate100  = Color(0xFFF1F5F9)
val Slate200  = Color(0xFFE2E8F0)
val Slate400  = Color(0xFF94A3B8)
val Slate700  = Color(0xFF334155)
val Slate800  = Color(0xFF1E293B)
val Slate900  = Color(0xFF0F172A)

// ── Legacy Surface Aliases ────────────────────────────────────────────────────
val RichBlack          = Color(0xFF000000)   // Now true OLED black
val SurfaceDeep        = Color(0xFF0A0C10)   // Was #1E1E1E — now tier 1
val SurfaceVariantDeep = Color(0xFF12151F)   // Was #2C2C2C — now tier 2

// ── Misc ──────────────────────────────────────────────────────────────────────
val OnboardBg = Color(0xFFF8FAF9)            // Light onboarding (unchanged)
```

### 5.2 — Rewrite `Theme.kt`
Replace the entire file:

```kotlin
package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary            = EmeraldGlow,
    onPrimary          = OledBlack,
    primaryContainer   = EmeraldDim,
    onPrimaryContainer = EmeraldGlow,

    secondary            = VioletPulse,
    onSecondary          = OledBlack,
    secondaryContainer   = VioletDim,
    onSecondaryContainer = VioletPulse,

    tertiary            = AmberFlame,
    onTertiary          = OledBlack,
    tertiaryContainer   = AmberDim,
    onTertiaryContainer = AmberFlame,

    error            = RoseCrimson,
    onError          = OledBlack,
    errorContainer   = RoseDim,
    onErrorContainer = RoseCrimson,

    background        = OledBlack,       // #000000
    onBackground      = TextPrimary,     // #F8FAFC

    surface           = SurfaceTier1,   // #0A0C10
    onSurface         = TextPrimary,
    surfaceVariant    = SurfaceTier2,   // #12151F
    onSurfaceVariant  = TextSecondary,

    outline           = OledBorder,     // #242B3F
    outlineVariant    = OledBorderActive,

    scrim             = Color(0xCC000000)
)

private val LightColorScheme = lightColorScheme(
    primary            = Emerald600,
    onPrimary          = Color.White,
    primaryContainer   = Emerald100,
    onPrimaryContainer = Emerald700,

    secondary            = PurpleAccent,
    onSecondary          = Color.White,
    secondaryContainer   = Color(0xFFEDE9FE),
    onSecondaryContainer = Color(0xFF4C1D95),

    tertiary            = GoldAccent,
    onTertiary          = Color.White,
    tertiaryContainer   = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF78350F),

    error            = RoseCrimson,
    onError          = Color.White,
    errorContainer   = Color(0xFFFFE4E6),
    onErrorContainer = Color(0xFF881337),

    background       = Slate50,
    onBackground     = Slate900,

    surface          = Color.White,
    onSurface        = Slate800,
    surfaceVariant   = Slate100,
    onSurfaceVariant = Slate400,

    outline          = Slate200,
    outlineVariant   = Slate100,
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}
```

### 5.3 — Rewrite `Type.kt`
Replace the entire file:

```kotlin
package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Black,
        fontSize   = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = (-1).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Black,
        fontSize   = 26.sp,
        lineHeight = 32.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize   = 20.sp,
        lineHeight = 26.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize   = 16.sp,
        lineHeight = 22.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize   = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily    = FontFamily.Default,
        fontWeight    = FontWeight.Normal,
        fontSize      = 16.sp,
        lineHeight    = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily    = FontFamily.Default,
        fontWeight    = FontWeight.Normal,
        fontSize      = 13.sp,
        lineHeight    = 18.sp
    ),
    bodySmall = TextStyle(
        fontFamily    = FontFamily.Default,
        fontWeight    = FontWeight.Normal,
        fontSize      = 11.sp,
        lineHeight    = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily    = FontFamily.Default,
        fontWeight    = FontWeight.Bold,
        fontSize      = 12.sp,
        lineHeight    = 16.sp,
        letterSpacing = 0.1.sp
    ),
    labelSmall = TextStyle(
        fontFamily    = FontFamily.Monospace,  // Tabular numbers for timers & metrics
        fontWeight    = FontWeight.Bold,
        fontSize      = 10.sp,
        lineHeight    = 14.sp,
        letterSpacing = 0.5.sp
    )
)
```

---

## 6. Full UI Revamp — All 8 Surfaces

### 6.1 Navigation Bar (`MainActivity.kt`)
The bar currently has **7 items**. Material3 spec allows max 5 for `NavigationBar`.
With OLED styling, also update `containerColor`.

**Replace** the `NavigationBar` block (lines 130–173) with:

```kotlin
Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = OledBlack,
    bottomBar = {
        NavigationBar(
            containerColor = SurfaceTier1,
            tonalElevation = 0.dp,
            modifier       = Modifier.testTag("app_navigation_bar")
        ) {
            val items = listOf(
                Triple("Home",    Icons.Default.Home,      0),
                Triple("Routine", Icons.Default.List,      1),
                Triple("Workout", Icons.Default.Star,      2),
                Triple("Focus",   Icons.Default.PlayArrow, 3),
                Triple("Tasks",   Icons.Default.Check,     4),
                Triple("Stats",   Icons.Default.Info,      5),
                Triple("Config",  Icons.Default.Settings,  6)
            )
            items.forEach { (title, icon, index) ->
                val isSelected = selectedTabIndex == index
                NavigationBarItem(
                    selected = isSelected,
                    onClick  = { selectedTabIndex = index },
                    icon     = {
                        Icon(
                            imageVector     = icon,
                            contentDescription = "$title Tab",
                            modifier        = if (isSelected)
                                Modifier.size(22.dp) else Modifier.size(20.dp)
                        )
                    },
                    label = {
                        Text(
                            text      = title,
                            fontSize  = 9.sp,
                            maxLines  = 1,
                            softWrap  = false,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor   = EmeraldGlow,
                        unselectedIconColor = TextMuted,
                        selectedTextColor   = EmeraldGlow,
                        unselectedTextColor = TextMuted,
                        indicatorColor      = EmeraldDim
                    ),
                    modifier = Modifier.testTag("nav_tab_$index")
                )
            }
        }
    }
) { paddingValues ->
    // ... (keep existing content unchanged)
```

Add these imports to `MainActivity.kt`:
```kotlin
import com.example.ui.theme.OledBlack
import com.example.ui.theme.SurfaceTier1
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.EmeraldDim
import com.example.ui.theme.TextMuted
```

### 6.2 `HomeScreen.kt` — OLED Bento Dashboard
Key changes (detailed diffs):

**A) Header section** — Change `fontSize = 26.sp` title + streak badge background:
- Set streak `Box` background: `Color(0xFF78350F)` → `AmberDim`
- Set streak text color: `Color(0xFF92400E)` → `AmberFlame`

**B) Routine progress card** (line 146–204):
- `containerColor = MaterialTheme.colorScheme.surface` (already theme-driven ✓)
- Change inner padding from `20.dp` → `14.dp`
- Change `Spacer(12.dp)` before progress bar → `Spacer(8.dp)`
- Progress bar: change background from `Slate100` to `SurfaceTier3`
- Progress bar fill: keep `EmeraldGlow`

**C) Hydration + Study row** — Change card inner padding from `16.dp` → `12.dp`
- Hydration: change `Emerald500` water value color → `CyanLaser` (water = cyan semantics)
- Hydration buttons: `containerColor = CyanLaser` (primary) / `CyanLaser.copy(alpha=0.7f)` (secondary)

**D) Steps card** — Change padding `20.dp` → `14.dp`
- Steps count text: `fontSize = 24.sp` stays, color = `TextPrimary`
- Health Sync button: `containerColor = SurfaceTier2` with border `OledBorder`

**E) Target body card** — Change `surfaceVariant` → `SurfaceTier2`

### 6.3 `RoutinesScreen.kt` — Compact Timeline
Key changes:

**A) Header counter badge** (lines 213–225):
- Change `background(Slate100)` → `background(SurfaceTier2)`
- Change text color `Slate700` → `EmeraldGlow`

**B) Vacation alert card** (lines 230–255):
- Change `containerColor = Color(0xFFFEF2F2)` → `containerColor = RoseDim.copy(alpha=0.3f)`
- Change text colors to `RoseCrimson`

**C) `RoutineItemRow` card** (line 481–492):
- Change `containerColor` for done items: `surfaceVariant` → `SurfaceTier1`
- Change `containerColor` for active items: `surface` → `SurfaceTier1`
- Change card padding `16.dp` → `12.dp`
- The category badge Box (lines 547–560): make background color more vibrant with OLED accent pairs

**D) Supplements chest card** (lines 309–388):
- Change `containerColor = Slate50` inside supplement cards → `SurfaceTier2`
- Change text color `Slate900` → `TextPrimary`

### 6.4 `TrackerScreen.kt` — OLED Focus Clock
Beyond the button fixes above:

**A) Main card** (line 117):
- Change `containerColor = MaterialTheme.colorScheme.surface` → already theme-driven ✓
- Change card border to use `OledBorder` via a `BorderStroke(1.dp, OledBorder)` passed to `border =`
  (instead of `CardDefaults.outlinedCardBorder()` which uses Material3 default which may render
   incorrectly in dark mode)

**B) Timer text** (line 190):
- Add `fontFamily = FontFamily.Monospace` to make digits tabular and non-shifting:
  ```kotlin
  Text(
      text         = formattedTime,
      fontFamily   = FontFamily.Monospace,   // ADD THIS
      color        = if (isActiveTimerRunning) EmeraldGlow else TextPrimary,
      fontSize     = scaledFontSize,
      fontWeight   = FontWeight.Black,
      ...
  )
  ```

**C) Active badge** (line 136):
- Change `PurpleAccent.copy(alpha=0.15f)` → `VioletDim` for better OLED contrast

**D) History card session rows** (lines 461–510):
- Change row background `onSurface.copy(alpha=0.05f)` → `SurfaceTier2`

**E) Auto-pause warning banner** (lines 79–113):
- Change `containerColor = Color(0xFFFEF3C7)` → `AmberDim.copy(alpha=0.4f)`
- Change text colors to `AmberFlame`
- Change border color to `AmberFlame.copy(alpha=0.5f)`

### 6.5 `TodosScreen.kt` — Data-Dense Goals
Beyond spacing fixes above:

**A) Screen header** — keep as-is (theme-driven)

**B) `GoalSectionHeader`** — Completion badge (line 142):
- Change `background(Emerald100)` → `background(EmeraldDim)`
- Change text color `Emerald700` → `EmeraldGlow`

**C) Priority chips** (line 167):
- Change `Slate100` default → `SurfaceTier2`
- Change `Slate700` text → `TextSecondary`

**D) `GoalItemRow`** — Card (line 191):
- Change completed-item container `surface.copy(alpha=0.5f)` → `SurfaceTier1.copy(alpha=0.6f)`
- Change active-item container `surface` → `SurfaceTier1`
- Change priority strip color: `RoseCrimson`/`AmberFlame`/`EmeraldGlow` (keep — already correct)
- Change edit/delete icon tint: `Slate400`/`RoseAccent` → `TextMuted`/`RoseCrimson`

### 6.6 `WorkoutScreen.kt` — Dense Gym Logging
Key changes throughout the file:

- All `background(MaterialTheme.colorScheme.background)` is theme-driven ✓
- Card padding reductions: `20.dp` → `14.dp` throughout
- Set log row backgrounds: use `SurfaceTier2` instead of ad-hoc colors
- Border strokes: use `BorderStroke(1.dp, OledBorder)` everywhere
- Keep `Emerald600`, `RoseCrimson`, `GoldAccent` for action buttons

### 6.7 `AnalyticsScreen.kt` — Neon Charts
Key changes:

**A) Chart bars** — Use `VioletPulse` for study chart, `CyanLaser` for water chart
**B) Chart background** — `background(SurfaceTier1)` per chart container
**C) Period selector chips** — `SurfaceTier2` background, `EmeraldGlow` selected

### 6.8 `SettingsScreen.kt` — OLED Control Panel
Key changes:

**A) All `Card` items** — Change `containerColor` from `surface` to `SurfaceTier1`
**B) All section label `Text`** — Change `Slate400` → `TextMuted` (same value, just semantic token)
**C) `Switch` colors** — Use `EmeraldGlow` as thumb/track active color
**D) Icon tints** — Change `Emerald500` icons → `EmeraldGlow`

---

## 7. File Change Summary

| File | Action | What changes |
|------|--------|-------------|
| `ui/theme/Color.kt` | REWRITE | Adds OLED surfaces, keeps all legacy aliases |
| `ui/theme/Theme.kt` | REWRITE | OLED dark scheme, full light scheme |
| `ui/theme/Type.kt` | REWRITE | Full M3 type scale, Monospace for `labelSmall` |
| `ui/TrackerScreen.kt` | MODIFY | L270–310 buttons, L313–386 controls, timer font, card borders |
| `ui/TodosScreen.kt` | MODIFY | L41 spacing, L87–89 divider, L200/221/225 icon sizes, badge colors |
| `ui/HomeScreen.kt` | MODIFY | Padding reductions, semantic color token updates |
| `ui/RoutinesScreen.kt` | MODIFY | Badge colors, card padding, OLED surfaces |
| `ui/WorkoutScreen.kt` | MODIFY | Card padding, border strokes, surface colors |
| `ui/AnalyticsScreen.kt` | MODIFY | Chart accent colors, surface backgrounds |
| `ui/SettingsScreen.kt` | MODIFY | Card surface colors, switch tints |
| `ui/SyncWellSplashScreen.kt` | NEW | Full-screen OLED splash composable |
| `viewmodel/SyncWellViewModel.kt` | MODIFY | Add `AppLaunchState`, update `checkSetupStatus()` |
| `MainActivity.kt` | MODIFY | Route on `appLaunchState`, import new types |
| `AndroidManifest.xml` | MODIFY | Point icons to `@mipmap/ic_launcher` |
| `res/drawable/ic_launcher_background.xml` | NEW | Solid OLED black background |
| `res/mipmap-anydpi-v26/ic_launcher.xml` | NEW | Adaptive icon definition |
| `res/mipmap-anydpi-v26/ic_launcher_round.xml` | NEW | Adaptive round icon definition |
| `res/mipmap-mdpi/ic_launcher.png` | NEW | 48×48 square icon |
| `res/mipmap-hdpi/ic_launcher.png` | NEW | 72×72 square icon |
| `res/mipmap-xhdpi/ic_launcher.png` | NEW | 96×96 square icon |
| `res/mipmap-xxhdpi/ic_launcher.png` | NEW | 144×144 square icon |
| `res/mipmap-xxxhdpi/ic_launcher.png` | NEW | 192×192 square icon |
| `fix_icon.py` | NEW (temp) | Script to generate mipmap PNGs from existing icon |

---

## 8. Execution Order for Implementation Agent

1. **Run `fix_icon.py`** — generates all mipmap PNG assets from existing icon.
2. **Create XML resource files** — `ic_launcher_background.xml`, both `mipmap-anydpi-v26/*.xml`.
3. **Update `AndroidManifest.xml`** — swap drawable refs for mipmap refs.
4. **Rewrite `Color.kt`** — new OLED palette (all old names preserved as aliases).
5. **Rewrite `Theme.kt`** — new colorScheme using new tokens.
6. **Rewrite `Type.kt`** — full typography scale.
7. **Add `AppLaunchState` sealed class to `SyncWellViewModel.kt`** — add at package level before class.
8. **Add `_appLaunchState` StateFlow to `SyncWellViewModel.kt`** — after `activeModal`.
9. **Update `checkSetupStatus()` in `SyncWellViewModel.kt`** — transition launch state.
10. **Create `SyncWellSplashScreen.kt`** — new file.
11. **Update `MainActivity.kt`** — replace `setContent {}` block, add imports.
12. **Update `TrackerScreen.kt`** — fix focus buttons, active controls, timer font.
13. **Update `TodosScreen.kt`** — spacing, divider, icon sizes, badge colors.
14. **Update `HomeScreen.kt`** — padding, color token updates.
15. **Update `RoutinesScreen.kt`** — badge colors, card surfaces.
16. **Update `WorkoutScreen.kt`** — borders, surface colors.
17. **Update `AnalyticsScreen.kt`** — chart colors.
18. **Update `SettingsScreen.kt`** — card surfaces, switch tints.
19. **Build**: `./gradlew compileDebugKotlin` — confirm 0 errors.
20. **Install on device** — verify all 4 bugs are resolved visually.

---

## 9. Acceptance Criteria (Definition of Done)

| # | Test | Pass Condition |
|---|------|---------------|
| 1 | Focus mode on 360 dp screen, system font scale 1.0× | STUDY, WORK, TIMEPASS all fully visible, no clipping |
| 2 | Focus mode on 360 dp screen, system font scale 1.3× | All buttons visible; TIMEPASS stacks below on narrow screens |
| 3 | Active timer controls | Pause/Resume, Save, Reset all visible without clipping at 360 dp |
| 4 | Cold start (onboarding already completed) | Full-screen OLED black splash shows for entire DB load duration, zero flash of onboarding UI |
| 5 | Cold start (first time user) | Full-screen OLED black splash, then smooth transition to OnboardingScreen |
| 6 | Launcher icon | Perfect 1:1 square/circle on home screen, no horizontal squish |
| 7 | Tasks screen | Section gap ≤ 16 dp, goal rows compact, no 50 dp blank zones |
| 8 | Dark mode enabled | All screens show #000000 OLED background, surfaces #0A0C10 and #12151F |
| 9 | Light mode enabled | Correct Slate50 background, all text readable at ≥4.5:1 contrast |
| 10 | Build | `./gradlew compileDebugKotlin` exits code 0 |
