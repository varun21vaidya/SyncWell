package com.example.data

import java.util.UUID

object DatabaseSeeder {

    suspend fun seedDatabase(dao: SyncWellDao, forceUpdate: Boolean = false) {
        val existingTemplates = dao.getRoutineTemplates()
        if (existingTemplates.isNotEmpty() && !forceUpdate) return

        if (forceUpdate) {
            dao.clearRoutineTemplates()
            dao.clearRoutineTemplateItems()
            dao.clearExerciseLibrary()
        }

        val weekdayId = UUID.randomUUID().toString()
        val saturdayId = UUID.randomUUID().toString()
        val sundayId = UUID.randomUUID().toString()

        // 1. INSTANTIATE MASTER SYSTEM ROUTINE LAYOUTS
        dao.insertRoutineTemplate(RoutineTemplate(id = weekdayId, name = "Standard Weekday Runbook", dayType = "WEEKDAY", isActive = true))
        dao.insertRoutineTemplate(RoutineTemplate(id = saturdayId, name = "Saturday Academic Sprint & Veg-Only", dayType = "SATURDAY", isActive = true))
        dao.insertRoutineTemplate(RoutineTemplate(id = sundayId, name = "Sunday Badminton & Reset", dayType = "SUNDAY", isActive = true))

        // 2. COMPREHENSIVE ROUTINE SEEDING (22+ Items)
        val templateItems = listOf(
            // --- WEEKDAY RUNBOOK (Monday-Friday) ---
            RoutineTemplateItem(UUID.randomUUID().toString(), weekdayId, "Wake cleanly (8:00 AM) & 500ml water", "HYGIENE", 1, "08:00"),
            RoutineTemplateItem(UUID.randomUUID().toString(), weekdayId, "Morning Fresh (Simple Wash, Pat Dry)", "HYGIENE", 2, "08:30"),
            RoutineTemplateItem(UUID.randomUUID().toString(), weekdayId, "Morning Skincare (Niacinamide, Moisturizer, SPF)", "SKINCARE", 3, "08:35"),
            RoutineTemplateItem(UUID.randomUUID().toString(), weekdayId, "Breakfast: 2 Whole + 2 Egg Whites, Oats, Fruit", "DIET", 4, "08:45"),
            RoutineTemplateItem(UUID.randomUUID().toString(), weekdayId, "Morning Stack: Multivitamin & Omega-3 with fat", "SUPPLEMENT", 5, "09:00"),
            RoutineTemplateItem(UUID.randomUUID().toString(), weekdayId, "Morning Focus Study Block 1 (2h 20m high-intensity)", "STUDY", 6, "09:10"),
            RoutineTemplateItem(UUID.randomUUID().toString(), weekdayId, "Work-Prep: Shaker bottle + Whey pack + Water sip", "PREP", 7, "11:30"),
            RoutineTemplateItem(UUID.randomUUID().toString(), weekdayId, "Commute to Office (Avoid fried snacks / sugar)", "HYGIENE", 8, "11:50"),
            RoutineTemplateItem(UUID.randomUUID().toString(), weekdayId, "Gym Mode: Active Warm-up (Shoulders/Hips/Knees)", "WORKOUT", 9, "12:15"),
            RoutineTemplateItem(UUID.randomUUID().toString(), weekdayId, "Gym Training: Compound Lifts (8-12 reps, 80 min)", "WORKOUT", 10, "12:30"),
            RoutineTemplateItem(UUID.randomUUID().toString(), weekdayId, "Cool post-gym shower (No hot water on hair!)", "HYGIENE", 11, "13:50"),
            RoutineTemplateItem(UUID.randomUUID().toString(), weekdayId, "Post-Gym Skincare (Simple Wash + Moisturizer)", "SKINCARE", 12, "14:10"),
            RoutineTemplateItem(UUID.randomUUID().toString(), weekdayId, "Precision Lunch: Paneer 100g, Dal, Curd, Salad", "DIET", 13, "14:20"),
            RoutineTemplateItem(UUID.randomUUID().toString(), weekdayId, "Lunch Supplement: Carbamide D3+K2 (Before 4PM)", "SUPPLEMENT", 14, "14:30"),
            RoutineTemplateItem(UUID.randomUUID().toString(), weekdayId, "Afternoon Hydration (500ml) & Office Focus", "HYGIENE", 15, "15:00"),
            RoutineTemplateItem(UUID.randomUUID().toString(), weekdayId, "Evening Protein (1 scoop whey + fruit)", "SUPPLEMENT", 16, "17:20"),
            RoutineTemplateItem(UUID.randomUUID().toString(), weekdayId, "Evening Study Sprint / Work Overflow", "STUDY", 17, "17:30"),
            RoutineTemplateItem(UUID.randomUUID().toString(), weekdayId, "Return Commute: 2.5 km brisk walk back home", "WORKOUT", 18, "18:00"),
            RoutineTemplateItem(UUID.randomUUID().toString(), weekdayId, "Protein Dinner (Chicken/Eggs/Soya + Curd)", "DIET", 19, "20:30"),
            RoutineTemplateItem(UUID.randomUUID().toString(), weekdayId, "Digestion Guard: 15m walk (Stay upright!)", "HEALTH", 20, "21:30"),
            RoutineTemplateItem(UUID.randomUUID().toString(), weekdayId, "Night Study Block (Review & Topics)", "STUDY", 21, "21:45"),
            RoutineTemplateItem(UUID.randomUUID().toString(), weekdayId, "Rosemary/Lavender Scalp massage & Hair Care", "HAIRCARE", 22, "22:00"),
            RoutineTemplateItem(UUID.randomUUID().toString(), weekdayId, "Night Skincare (Salicylic wash + Moisturizer)", "SKINCARE", 23, "22:30"),
            RoutineTemplateItem(UUID.randomUUID().toString(), weekdayId, "Magnesium Glycinate for CNS Calm", "SUPPLEMENT", 24, "23:00"),
            RoutineTemplateItem(UUID.randomUUID().toString(), weekdayId, "Night-Before Oats Prep & Tomorrow's Shaker", "PREP", 25, "23:15"),
            RoutineTemplateItem(UUID.randomUUID().toString(), weekdayId, "Sleep Prep: Device Blackout & Dim Lights", "HEALTH", 26, "23:30"),

            // --- SATURDAY RUNBOOK (Veg-Only, Heavy Study) ---
            RoutineTemplateItem(UUID.randomUUID().toString(), saturdayId, "Wake up (8:30 AM) — No Alarm Snooze", "HYGIENE", 1, "08:30"),
            RoutineTemplateItem(UUID.randomUUID().toString(), saturdayId, "🥗 VEG-ONLY DAY — No Meat / Chicken / Fish", "DIET", 2, "08:30"),
            RoutineTemplateItem(UUID.randomUUID().toString(), saturdayId, "Drink 500ml waking water", "HYGIENE", 3, "08:32"),
            RoutineTemplateItem(UUID.randomUUID().toString(), saturdayId, "Morning Skincare (Wash, Niacinamide, SPF)", "SKINCARE", 4, "08:40"),
            RoutineTemplateItem(UUID.randomUUID().toString(), saturdayId, "Breakfast — Eggs, Oats, Seasonal Fruit", "DIET", 5, "09:00"),
            RoutineTemplateItem(UUID.randomUUID().toString(), saturdayId, "Morning Stack: Multivitamin + Omega-3", "SUPPLEMENT", 6, "09:10"),
            RoutineTemplateItem(UUID.randomUUID().toString(), saturdayId, "Morning Study Sprint Block 1 (3h 50m)", "STUDY", 7, "09:30"),
            RoutineTemplateItem(UUID.randomUUID().toString(), saturdayId, "Lunch — Rice, Dal, Paneer 100g, Curd, Salad", "DIET", 8, "13:20"),
            RoutineTemplateItem(UUID.randomUUID().toString(), saturdayId, "Lunch Supplement: Vitamin D3+K2 (Before 4PM)", "SUPPLEMENT", 9, "13:30"),
            RoutineTemplateItem(UUID.randomUUID().toString(), saturdayId, "Study Sprint Block 2", "STUDY", 10, "14:00"),
            RoutineTemplateItem(UUID.randomUUID().toString(), saturdayId, "Hydration Check — 500ml Water", "HYGIENE", 11, "15:30"),
            RoutineTemplateItem(UUID.randomUUID().toString(), saturdayId, "Whey Protein (Mix with Milk for Veg Day)", "SUPPLEMENT", 12, "17:20"),
            RoutineTemplateItem(UUID.randomUUID().toString(), saturdayId, "Study Sprint Block 3", "STUDY", 13, "17:30"),
            RoutineTemplateItem(UUID.randomUUID().toString(), saturdayId, "Dinner — Paneer / Dal / Soya (Zero Meat)", "DIET", 14, "19:30"),
            RoutineTemplateItem(UUID.randomUUID().toString(), saturdayId, "Digestion Guard: Stay Upright 1 Hour", "HEALTH", 15, "20:00"),
            RoutineTemplateItem(UUID.randomUUID().toString(), saturdayId, "Study Sprint Block 4 (Evening Focus)", "STUDY", 16, "20:30"),
            RoutineTemplateItem(UUID.randomUUID().toString(), saturdayId, "Night Skincare (Salicylic wash + Moisturizer)", "SKINCARE", 17, "22:00"),
            RoutineTemplateItem(UUID.randomUUID().toString(), saturdayId, "Night Supplement: Magnesium Glycinate", "SUPPLEMENT", 18, "22:15"),
            RoutineTemplateItem(UUID.randomUUID().toString(), saturdayId, "Device Blackout & Sleep Prep", "HEALTH", 19, "23:00"),

            // --- SUNDAY RUNBOOK (Badminton, Calibration) ---
            RoutineTemplateItem(UUID.randomUUID().toString(), sundayId, "Wake up (7:00 AM Sharp)", "HYGIENE", 1, "07:00"),
            RoutineTemplateItem(UUID.randomUUID().toString(), sundayId, "Drink 500ml waking water", "HYGIENE", 2, "07:05"),
            RoutineTemplateItem(UUID.randomUUID().toString(), sundayId, "Quick face rinse — No full skincare yet", "SKINCARE", 3, "07:10"),
            RoutineTemplateItem(UUID.randomUUID().toString(), sundayId, "Travel to Badminton Court (40-min commute)", "PREP", 4, "07:15"),
            RoutineTemplateItem(UUID.randomUUID().toString(), sundayId, "🏸 Badminton Session (8:00 AM – 10:00 AM)", "WORKOUT", 5, "08:00"),
            RoutineTemplateItem(UUID.randomUUID().toString(), sundayId, "Return Commute (40-min walk/transit)", "PREP", 6, "10:00"),
            RoutineTemplateItem(UUID.randomUUID().toString(), sundayId, "Post-Sport Cool Shower (No Hot on Scalp)", "HYGIENE", 7, "10:45"),
            RoutineTemplateItem(UUID.randomUUID().toString(), sundayId, "Full Morning Skincare (Niacinamide + SPF)", "SKINCARE", 8, "11:00"),
            RoutineTemplateItem(UUID.randomUUID().toString(), sundayId, "High-Protein Post-Sport Brunch (Eggs, Oats, Paneer)", "DIET", 9, "11:15"),
            RoutineTemplateItem(UUID.randomUUID().toString(), sundayId, "Morning Supplements: Multivitamin + Omega-3", "SUPPLEMENT", 10, "11:25"),
            RoutineTemplateItem(UUID.randomUUID().toString(), sundayId, "Study Block 1 (4h Target Focus)", "STUDY", 11, "12:00"),
            RoutineTemplateItem(UUID.randomUUID().toString(), sundayId, "Lunch: Rice, Dal, Paneer, Curd, Salad", "DIET", 12, "16:00"),
            RoutineTemplateItem(UUID.randomUUID().toString(), sundayId, "Lunch Supplement: D3+K2 (Before 4PM)", "SUPPLEMENT", 13, "16:05"),
            RoutineTemplateItem(UUID.randomUUID().toString(), sundayId, "Study Block 2 (7h 40m Target Focus)", "STUDY", 14, "17:00"),
            RoutineTemplateItem(UUID.randomUUID().toString(), sundayId, "Whey Protein (5:20 PM)", "SUPPLEMENT", 15, "17:20"),
            RoutineTemplateItem(UUID.randomUUID().toString(), sundayId, "Hydration Check — 500ml Water", "HYGIENE", 16, "18:00"),
            RoutineTemplateItem(UUID.randomUUID().toString(), sundayId, "📅 Weekly Plan Calibration Modal (9:00 PM)", "PREP", 17, "21:00"),
            RoutineTemplateItem(UUID.randomUUID().toString(), sundayId, "Dinner — Chicken / Egg Curry / Chole / Soya", "DIET", 18, "19:30"),
            RoutineTemplateItem(UUID.randomUUID().toString(), sundayId, "Digestion Guard — Upright 1 Hour + 10-min Walk", "HEALTH", 19, "20:10"),
            RoutineTemplateItem(UUID.randomUUID().toString(), sundayId, "Night Skincare (Salicylic wash + Moisturizer)", "SKINCARE", 20, "22:00"),
            RoutineTemplateItem(UUID.randomUUID().toString(), sundayId, "Night Supplement: Magnesium Glycinate", "SUPPLEMENT", 21, "22:15"),
            RoutineTemplateItem(UUID.randomUUID().toString(), sundayId, "Device Blackout & Sleep Prep (Target: 11:45 PM)", "HEALTH", 22, "23:00")
        )

        dao.insertRoutineTemplateItems(templateItems)

        // 3. SEED CORE EXERCISE LIBRARY (Perfect Workouts)
        val exercises = listOf(
            // --- COMMERCIAL GYM: PUSH ---
            ExerciseLibraryItem("ex_pushups", "Pushups", "CHEST", 1, 15, 100, 60, "home_pushups", null),
            ExerciseLibraryItem("ex_flat_bench", "Barbell Flat Bench Press", "CHEST", 4, 8, 15, 150, null, null),
            ExerciseLibraryItem("ex_overhead_press", "Dumbbell Overhead Shoulder Press", "SHOULDERS", 3, 15, 15, 120, null, null),
            ExerciseLibraryItem("ex_incline_press", "Incline Dumbbell Press", "CHEST", 3, 12, 15, 120, null, null),
            ExerciseLibraryItem("ex_lateral_raises", "Lateral Raises", "SHOULDERS", 3, 12, 15, 90, null, null),
            ExerciseLibraryItem("ex_lateral_cable", "Lateral Raises Cable", "SHOULDERS", 3, 10, 15, 90, null, null),
            ExerciseLibraryItem("ex_pec_fly", "Pec Fly Machine", "CHEST", 3, 10, 15, 90, null, null),
            ExerciseLibraryItem("ex_tricep_pushdown", "Tricep Pushdown — Bar/Rope", "TRICEPS", 3, 10, 12, 90, null, null),
            ExerciseLibraryItem("ex_cable_crossover", "Cable Crossover", "CHEST", 3, 15, 20, 90, null, null),
            ExerciseLibraryItem("ex_tricep_overhead", "Tricep Overhead Extension", "TRICEPS", 3, 10, 10, 90, null, null),
            ExerciseLibraryItem("ex_pullover", "Dumbbell Pullover", "CHEST", 2, 10, 10, 90, null, null),

            // --- COMMERCIAL GYM: PULL ---
            ExerciseLibraryItem("ex_pullups", "Pull-ups", "BACK", 3, 15, 15, 120, "home_pullups", null),
            ExerciseLibraryItem("ex_deadlift", "Deadlift", "BACK", 3, 15, 15, 180, null, null),
            ExerciseLibraryItem("ex_lat_pulldown", "Lat Pulldown Front", "BACK", 3, 12, 15, 120, null, null),
            ExerciseLibraryItem("ex_db_row", "Dumbbell Row", "BACK", 3, 15, 15, 90, null, null),
            ExerciseLibraryItem("ex_cable_row", "Seated Cable Row", "BACK", 3, 15, 15, 90, null, null),
            ExerciseLibraryItem("ex_face_pull", "Face Pull", "REAR_DELTS", 3, 15, 15, 90, null, null),
            ExerciseLibraryItem("ex_rear_delt_raise", "Rear Delt Dumbbell Raise", "REAR_DELTS", 2, 10, 10, 90, null, null),
            ExerciseLibraryItem("ex_bicep_curl", "DB Bicep Curl (supinated)", "BICEPS", 3, 10, 12, 90, null, null),
            ExerciseLibraryItem("ex_hammer_curl", "Hammer Curl (neutral)", "BICEPS", 3, 10, 12, 90, null, null),
            ExerciseLibraryItem("ex_preacher_curl", "Preacher Curl DB", "BICEPS", 3, 12, 12, 90, null, null),
            ExerciseLibraryItem("ex_wrist_curl", "Forearm Wrist Curl — Rod", "FOREARMS", 2, 15, 15, 60, null, null),

            // --- COMMERCIAL GYM: LEGS + ABS ---
            ExerciseLibraryItem("ex_free_squats", "Free Squats (Warmup)", "LEGS", 2, 20, 20, 60, "home_bw_squats", null),
            ExerciseLibraryItem("ex_weighted_squat", "Weighted Barbell Squat", "LEGS", 3, 15, 15, 150, null, null),
            ExerciseLibraryItem("ex_leg_press", "Leg Press", "LEGS", 3, 15, 15, 120, null, null),
            ExerciseLibraryItem("ex_rdl", "Romanian Deadlift (RDL)", "LEGS", 3, 10, 10, 120, null, null),
            ExerciseLibraryItem("ex_leg_extension", "Leg Extension", "LEGS", 3, 12, 15, 90, null, null),
            ExerciseLibraryItem("ex_leg_curl", "Leg Curl", "LEGS", 3, 15, 15, 90, null, null),
            ExerciseLibraryItem("ex_lunges", "Leg Lunges", "LEGS", 3, 12, 12, 90, null, null),
            ExerciseLibraryItem("ex_calf_raises", "Calf Raises", "CALVES", 3, 20, 20, 60, "home_step_calf_raises", null),
            ExerciseLibraryItem("ex_leg_raises", "Hanging Leg Raises", "ABS", 3, 12, 15, 60, "home_lying_leg_raises", null),
            ExerciseLibraryItem("ex_cable_crunches", "Cable Crunches", "ABS", 3, 15, 15, 60, null, null),
            ExerciseLibraryItem("ex_decline_crunches", "Decline Crunches", "ABS", 3, 20, 20, 60, null, null),
            ExerciseLibraryItem("ex_russian_twists", "Russian Twists", "ABS", 3, 30, 30, 60, null, null),
            ExerciseLibraryItem("ex_plank", "Plank", "ABS", 3, 60, 75, 60, "home_plank", null),

            // --- HOME CALISTHENICS: PUSH ---
            ExerciseLibraryItem("home_pushups", "Elevated Pushups", "CHEST", 4, 10, 20, 60, null, null, "HOME"),
            ExerciseLibraryItem("home_pike_pushups", "Pike Pushups", "SHOULDERS", 3, 8, 12, 60, null, null, "HOME"),
            ExerciseLibraryItem("home_diamond_pushups", "Diamond / Close-Grip Pushups", "TRICEPS", 3, 10, 15, 60, null, null, "HOME"),
            ExerciseLibraryItem("home_chair_dips", "Chair Dips", "TRICEPS", 3, 10, 15, 60, null, null, "HOME"),
            ExerciseLibraryItem("home_bottle_lateral_raises", "Lateral Raise (water bottles)", "SHOULDERS", 3, 15, 20, 60, null, null, "HOME"),

            // --- HOME CALISTHENICS: PULL ---
            ExerciseLibraryItem("home_pullups", "Pull-ups/Chin-ups", "BACK", 4, 5, 15, 90, null, null, "HOME"),
            ExerciseLibraryItem("home_inverted_rows", "Inverted Rows under table", "BACK", 3, 10, 15, 60, null, null, "HOME"),
            ExerciseLibraryItem("home_backpack_rows", "Backpack Rows", "BACK", 3, 12, 15, 60, null, null, "HOME"),
            ExerciseLibraryItem("home_backpack_curls", "Backpack Curls", "BICEPS", 3, 12, 15, 60, null, null, "HOME"),
            ExerciseLibraryItem("home_y_t_raises", "Y- or T-Raises on floor", "REAR_DELTS", 3, 15, 20, 60, null, null, "HOME"),

            // --- HOME CALISTHENICS: LEGS + ABS ---
            ExerciseLibraryItem("home_bulgarian_squats", "Bulgarian Split Squats", "LEGS", 3, 12, 15, 60, null, null, "HOME"),
            ExerciseLibraryItem("home_bw_squats", "Bodyweight Squats (tempo)", "LEGS", 3, 20, 20, 60, null, null, "HOME"),
            ExerciseLibraryItem("home_single_leg_thrust", "Single-Leg Hip Thrust", "LEGS", 3, 15, 15, 60, null, null, "HOME"),
            ExerciseLibraryItem("home_step_calf_raises", "Calf Raises on Step", "CALVES", 4, 20, 25, 60, null, null, "HOME"),
            ExerciseLibraryItem("home_lying_leg_raises", "Lying Leg Raises", "ABS", 3, 15, 15, 60, null, null, "HOME"),
            ExerciseLibraryItem("home_plank", "Plank", "ABS", 3, 45, 60, 60, null, null, "HOME")
        )

        dao.insertExerciseLibrary(exercises)

        // 4. SEED WORKOUT PATTERN ASSIGNMENTS
        val workouts = listOf(
            WorkoutTemplate(id = UUID.randomUUID().toString(), name = "Heavy Push Lifts", dayType = "MONDAY"),
            WorkoutTemplate(id = UUID.randomUUID().toString(), name = "Heavy Pull Lifts", dayType = "TUESDAY"),
            WorkoutTemplate(id = UUID.randomUUID().toString(), name = "High-Load Legs + Core", dayType = "WEDNESDAY"),
            WorkoutTemplate(id = UUID.randomUUID().toString(), name = "Lighter Push Velocity Selections", dayType = "THURSDAY"),
            WorkoutTemplate(id = UUID.randomUUID().toString(), name = "Lighter Pull Velocity Selections", dayType = "FRIDAY")
        )
        dao.insertWorkoutTemplates(workouts)
    }
}
