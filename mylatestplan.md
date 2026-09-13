# Comprehensive Analytics & Personal Tracking Overhaul Plan

## 1. Executive Overview & Problem Statement
Currently, SyncWell collects extensive personal tracking data across multiple dimensions:
- **Circadian Routines & Habits** (Daily checklists, completion percentages, meal protein tracking)
- **Focus Sessions** (`STUDY`, `WORK`, `TIMEPASS`, `SPORT`, `SLEEP` tracked with start/end timestamps and durations)
- **Physical Activity & Step Counts** (Daily step records from manual logs or sensors)
- **Weight Room Workouts** (Sessions, exercises, set logs with exact reps and kilogram weights)
- **Body Weight Composition** (Logged weights, pinned goal 70.0 kg)
- **Hydration Tracking** (Water intake events in mL)
- **Supplement Compliance** (Morning, lunch, and bedtime supplement checklists)

However, the current **Analytics Screen** only displays:
1. Two small streak cards (Gym and Diet).
2. Weight submission input box.
3. Vertical bars for Study minutes, Gym binary %, and Diet protein points.
4. Basic text list for Weight history.
5. Hydration meters.

### Core Deficiencies Identified:
1. **Unused / Hidden Rich Data**:
   - **Step Counts & Cardio Trends**: Steps are collected and logged in `StepsLog`, but nowhere visualized on Analytics!
   - **Focus Categorization Breakdown**: Focus tracking records `WORK`, `STUDY`, `TIMEPASS`, and `SLEEP`, but only `STUDY` was being graphed. A student/professional needs to see their **Deep Work vs. Study vs. Timepass ratio** (Focus Quality Index).
   - **Workout Volume & Strength Progression**: Sets, weights, and reps are logged in detail, but there is zero volume tonnage ($kg \times reps$) or workout duration analytics over time.
   - **Circadian Routine Adherence**: Routine completion percentage (`routineCompletionPct`) is stored daily in `DailyAnalytics`, but not graphed as an overall lifestyle adherence trend.
2. **UI/UX Aesthetics & Data-Density**:
   - Lack of cohesive executive summary KPI cards (e.g. 7-Day Average Steps, Total Weekly Volume, Focus Efficiency %).
   - Basic vertical bars without gradient shaders or interactive indicators.
   - Repetitive, generic container cards without visual hierarchy.

---

## 2. Proposed Architectural & Feature Enhancements

### Dimension 1: Executive KPI Overview Matrix (Header Tier)
At the very top of the Analytics screen, introduce a high-impact, 4-cell data-dense metric grid:
- **Consistency Score**: Weighted daily lifestyle index combining routine completion + focus target + hydration.
- **Focus Volume**: Total focus hours logged this period (with breakdown tag: e.g. "82% Productive").
- **Physical Output**: Total step count & active workout sessions completed.
- **Weight Delta**: Current weight vs. 70.0 kg target with trend badge ($\pm \Delta$).

### Dimension 2: Multi-Category Focus Time Allocation (Stacked/Multi-Bar)
- Graph **Study**, **Work**, and **Timepass** side-by-side or stacked in a sleek, color-coded chart:
  - `STUDY`: Neon Purple (`#A855F7`)
  - `WORK`: Cyan Laser (`#06B6D4`)
  - `TIMEPASS`: Slate / Amber (`#F59E0B` or `#64748B`)
- Compute and display the **Productivity Ratio**: $\frac{\text{Study} + \text{Work}}{\text{Total Tracked}} \times 100\%$.

### Dimension 3: Step Count & Physical Activity Index
- Weekly / Monthly step count bar chart with horizontal benchmark lines at 8,000 and 10,000 steps.
- Color indicators for active days (Green glow if $\ge 8,000$ steps).

### Dimension 4: Weight Room Tonnage & Workout Performance
- Compute total session tonnage: $\sum (\text{weightKg} \times \text{reps})$ across completed sets.
- Graph weekly lifting volume to visualize Progressive Overload over the month.
- Display Workout Session frequency (e.g., 4/5 planned lifting days hit).

### Dimension 5: Routine Adherence & Streaks Evolution
- Visualize the **Circadian Habit Score** across the last 7 or 30 days.
- Retain and elevate the **Gym Streak** and **Diet Protein Streak** badges with dynamic fire glow and progress bars.

### Dimension 6: Modern OLED UI/UX Polish
- **Color tokens**: Pure `OledBlack` (`#000000`), `SurfaceTier1` (`#0A0A0A`), `SurfaceTier2` (`#141414`), `OledBorder` (`#1F1F1F`), and neon glows (`EmeraldGlow`, `CyanLaser`, `PurpleAccent`, `AmberFlame`).
- **Interactive period switch**: Seamless `DAILY` | `WEEKLY` | `MONTHLY` toggle.
- **Micro-animations**: Smooth loading transitions, value tags, and progress bar animations.

---

## 3. Implementation Plan

### Phase 1: Computation Service & DAO Queries (`AnalyticsComputationService.kt` & `SyncWellDao.kt`)
1. In `AnalyticsComputationService.kt`:
   - Calculate workout duration from `workout_sessions` (difference between `startedAt` and `endedAt`) and store in `totalWorkoutMinutes`.
   - Ensure `workMin`, `timepassMin`, `totalStudyMinutes`, `totalSteps`, and `totalWaterMl` are aggregated reliably.
2. In `SyncWellDao.kt`:
   - Add query to fetch completed workout sets for calculating volume tonnage if needed.
   - Add query to get recent step logs across a date range.

### Phase 2: ViewModel Aggregation Enhancements (`SyncWellViewModel.kt`)
1. Create unified data structure `AnalyticsPeriodSummary`:
   - Average daily steps
   - Total productive focus vs leisure focus
   - Total workout sessions & lifting tonnage
   - Hydration compliance rate (% of days hitting $\ge 3500$ ml)
   - Habit adherence average
2. Expose reactive StateFlows for period analytics so UI updates in real-time.

### Phase 3: Analytics Screen UI Redesign (`AnalyticsScreen.kt`)
1. **Executive Metrics Grid**: 4 curated KPI cards with icons, values, and delta indicators.
2. **Focus Breakdown Chart**: Compare Study, Work, and Timepass side-by-side with productivity ratio gauge.
3. **Physical Activity & Steps Chart**: Daily and weekly step trends with 8k/10k target markers.
4. **Workout Progression & Tonnage Card**: Session count, weekly volume, and consistency.
5. **Circadian Habit Adherence Chart**: Daily checklist percentage progress bar timeline.
6. **Hydration & Weight Tracker Cards**: Enhanced OLED styling with target indicators.

---

## 4. Verification Plan
- **Build verification**: `./gradlew compileDebugKotlin` to ensure type safety and error-free compilation.
- **Data verification**: Verify fallback/zero-state rendering for empty databases and populated trends for active data.
- **Git workflow**: Commit and push changes to `origin/main`.
