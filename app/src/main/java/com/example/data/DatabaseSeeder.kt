package com.example.data

import java.util.UUID

data class VitaminChoice(val name: String, val time: String? = null)

data class SetupAnswers(
    val wakeTime: String = "07:00",
    val sleepTime: String = "23:00",
    val workoutDays: List<Int> = listOf(1, 3, 5), // 0 = Sunday
    val vitamins: List<VitaminChoice> = emptyList()
)

object DatabaseSeeder {

    private fun shift(time: String, minutes: Int): String {
        val (h, m) = time.split(":").map { it.toInt() }
        val total = ((h * 60 + m + minutes) % 1440 + 1440) % 1440
        return "%02d:%02d".format(total / 60, total % 60)
    }

    /** Pure: builds the starter templates and their items from the onboarding answers. */
    fun buildStarterTemplates(a: SetupAnswers): Pair<List<RoutineTemplate>, List<RoutineTemplateItem>> {
        val w = a.wakeTime
        val s = a.sleepTime
        val templates = mutableListOf<RoutineTemplate>()
        val items = mutableListOf<RoutineTemplateItem>()

        fun add(name: String, repeat: String, days: List<Int>, rows: List<Triple<String, String, String>>) {
            val id = UUID.randomUUID().toString()
            templates += RoutineTemplate(id = id, name = name, repeatType = repeat, daysOfWeek = days.joinToString(",", "[", "]"), isStarter = true)
            rows.forEachIndexed { i, (title, category, time) ->
                items += RoutineTemplateItem(UUID.randomUUID().toString(), id, title, category, i + 1, time)
            }
        }

        add("Daily Essentials", "DAILY", listOf(1, 2, 3, 4, 5, 6, 0), listOf(
            Triple("Wake up & drink water", "HYGIENE", w),
            Triple("Morning hygiene", "HYGIENE", shift(w, 30)),
            Triple("Breakfast", "DIET", shift(w, 60)),
            Triple("Focus block", "STUDY", shift(w, 120)),
            Triple("Lunch", "DIET", shift(w, 300)),
            Triple("Afternoon focus block", "STUDY", shift(w, 360)),
            Triple("Hydration check", "HYGIENE", shift(w, 420)),
            Triple("Dinner", "DIET", shift(s, -180)),
            Triple("Evening walk", "HEALTH", shift(s, -150)),
            Triple("Wind down & screens off", "PREP", shift(s, -45)),
            Triple("Sleep", "HEALTH", s)
        ))
        if (a.workoutDays.isNotEmpty()) {
            add("Workout Days", "WEEKLY", a.workoutDays, listOf(
                Triple("Warm-up", "WORKOUT", shift(w, 540)),
                Triple("Workout", "WORKOUT", shift(w, 555)),
                Triple("Cool-down & shower", "HYGIENE", shift(w, 630))
            ))
        }
        if (a.vitamins.isNotEmpty()) {
            add("Supplements", "DAILY", listOf(1, 2, 3, 4, 5, 6, 0), a.vitamins.map { v ->
                val time = v.time ?: when {
                    v.name.contains("D3", true) || v.name.contains("K2", true) -> shift(w, 310)
                    v.name.contains("Omega", true) -> shift(w, 315)
                    v.name.contains("Magnesium", true) -> shift(s, -45)
                    else -> shift(w, 75)
                }
                Triple(v.name, "SUPPLEMENT", time)
            })
        }
        return templates to items
    }

    /** Replaces starter routines (user-created ones are untouched). */
    suspend fun seedStarterRoutines(dao: SyncWellDao, answers: SetupAnswers = SetupAnswers()) {
        dao.deleteStarterTemplates()
        val (templates, items) = buildStarterTemplates(answers)
        templates.forEach { dao.insertRoutineTemplate(it) }
        dao.insertRoutineTemplateItems(items)
    }

    /** Inserts built-in exercises that are missing; never deletes custom ones. */
    suspend fun seedExercises(dao: SyncWellDao) {
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

        val have = dao.getExerciseLibrary().map { it.id }.toSet()
        dao.insertExerciseLibrary(exercises.filter { it.id !in have })
    }
}
