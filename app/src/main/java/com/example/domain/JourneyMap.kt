package com.example.domain

import org.json.JSONObject

/** A step on the painted trail. [x] and [y] are fractions of the region image. */
data class MapPoint(val number: Int, val x: Float, val y: Float, val checkpoint: Boolean)

/** One region from `res/raw/adventure_route.json`. A null [image] means the art is not painted yet and the screen draws a placeholder. */
data class RegionDef(
    val index: Int,
    val name: String,
    val image: String?,
    val points: List<MapPoint>,
    val weather: String?,
    val intro: String,
    val releaseDate: String
) {
    val steps get() = points.size
}

enum class RegionView { CURRENT, DONE, LOCKED }

object JourneyMap {

    fun parse(json: String): List<RegionDef> {
        val arr = JSONObject(json).getJSONArray("regions")
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val pts = o.getJSONArray("points")
            RegionDef(
                index = i, name = o.getString("name"), image = if (o.isNull("image")) null else o.getString("image"),
                points = (0 until pts.length()).map { j ->
                    val p = pts.getJSONObject(j)
                    MapPoint(p.getInt("number"), p.getDouble("x").toFloat(), p.getDouble("y").toFloat(), p.optBoolean("checkpoint", false))
                }.sortedBy { it.number },
                weather = if (o.has("weatherOverlay")) o.getString("weatherOverlay") else null,
                intro = o.optString("intro", ""), releaseDate = o.optString("releaseDate", "1970-01-01")
            )
        }
    }

    /** Where the sprite stands: on the point of that step. Step 0 (region start) is just below the first point, kept on the image. */
    fun positionOf(region: RegionDef, step: Int): Pair<Float, Float> {
        val first = region.points.first()
        if (step <= 0) return first.x to minOf(1f, first.y + 0.04f)
        val p = region.points[(step - 1).coerceAtMost(region.points.lastIndex)]
        return p.x to p.y
    }

    /** A region is shown clear once the character has reached it and it has been released; otherwise only its name is shown, blurred. */
    fun view(region: RegionDef, current: Int, furthest: Int, today: String): RegionView = when {
        region.releaseDate > today || region.index > furthest -> RegionView.LOCKED
        region.index == current -> RegionView.CURRENT
        else -> RegionView.DONE
    }

    /** "Day 9 of 14", and "Camp on day 7" or "Gate on day 14" when that is where the character is heading next. */
    fun dayLabel(region: RegionDef, step: Int): String {
        val total = region.steps
        return "Day ${step.coerceIn(0, total)} of $total"
    }

    fun nextMilestone(region: RegionDef, step: Int): String? {
        val camp = region.points.firstOrNull { it.checkpoint }?.number
        return when {
            camp != null && step < camp -> "Camp on day $camp"
            step < region.steps -> "Gate on day ${region.steps}"
            else -> null
        }
    }
}

/** The six base characters, picked at the start and changeable later. Cosmetic only. [color] is an ARGB long for the placeholder sprite. */
data class JourneyCharacter(val id: String, val name: String, val color: Long)

object JourneyCharacters {
    val all = listOf(
        JourneyCharacter("max", "Max", 0xFF4FB8FF), JourneyCharacter("ivy", "Ivy", 0xFF3DDC97),
        JourneyCharacter("lee", "Lee", 0xFFFFC857), JourneyCharacter("emma", "Emma", 0xFFFF7A85),
        JourneyCharacter("charles", "Charles", 0xFFA78BFA), JourneyCharacter("georgia", "Georgia", 0xFFFF9F6B)
    )

    fun byId(id: String?) = all.firstOrNull { it.id == id } ?: all.first()
}
