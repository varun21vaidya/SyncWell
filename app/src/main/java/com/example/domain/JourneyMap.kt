package com.example.domain

import com.example.data.JourneyState
import org.json.JSONObject

/** A step on the painted trail. [x] and [y] are fractions of the region image. */
data class MapPoint(val number: Int, val x: Float, val y: Float, val checkpoint: Boolean)

/** The guardian of a region's gate. The lines are calm information, never guilt. */
data class Warden(val name: String, val personality: String, val challenge: String, val open: String, val closed: String)

/** One region from `res/raw/adventure_route.json`. A null [image] means the art is not painted yet and the screen draws a placeholder. */
data class RegionDef(
    val index: Int,
    val name: String,
    val image: String?,
    val points: List<MapPoint>,
    val weather: String?,
    val intro: String,
    val releaseDate: String,
    val warden: Warden
) {
    val steps get() = points.size
}

enum class RegionView { CURRENT, DONE, LOCKED }

/** The painted weather of a region, used for its stamp and the light overlay. A region with no weather tag is clear. */
enum class Weather { SUN, RAIN, SNOW }

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
                intro = o.optString("intro", ""), releaseDate = o.optString("releaseDate", "1970-01-01"),
                warden = o.getJSONObject("warden").let { w ->
                    Warden(w.getString("name"), w.getString("personality"), w.getString("challenge"), w.getString("open"), w.getString("closed"))
                }
            )
        }
    }

    /** The one-line status shared by the Home strip and the widget: where you are, a pause note, or an invitation. Never says step. */
    fun stripText(state: JourneyState?, regions: List<RegionDef>): String = when {
        state == null -> "Try the Journey: walk a painted map, one day at a time"
        !state.enabled -> "Journey paused. Tap to resume"
        else -> regions.getOrElse(state.regionIndex) { regions.last() }.let { r ->
            listOfNotNull(r.name, dayLabel(r, state.stepInRegion), nextMilestone(r, state.stepInRegion)).joinToString(" · ")
        }
    }

    /** A window of the region image around (x, y), as fractions of the image: [cw] wide and [ch] tall, kept fully inside the picture. */
    fun cropAround(x: Float, y: Float, cw: Float, ch: Float): FloatArray {
        val w = cw.coerceIn(0.05f, 1f)
        val h = ch.coerceIn(0.05f, 1f)
        val left = (x - w / 2).coerceIn(0f, 1f - w)
        val top = (y - h / 2).coerceIn(0f, 1f - h)
        return floatArrayOf(left, top, left + w, top + h)
    }

    fun weatherOf(region: RegionDef?): Weather = when (region?.weather) {
        "light_rain_particles" -> Weather.RAIN
        "light_snow_particles" -> Weather.SNOW
        else -> Weather.SUN
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
data class JourneyCharacter(val id: String, val name: String, val color: Long, val blurb: String = "")

object JourneyCharacters {
    val all = listOf(
        JourneyCharacter("max", "Max", 0xFF4FB8FF, "Steady and curious. Never rushes, never stops."),
        JourneyCharacter("ivy", "Ivy", 0xFF3DDC97, "Quick on her feet and always up for a fresh trail."),
        JourneyCharacter("lee", "Lee", 0xFFFFC857, "Calm under pressure and packs light."),
        JourneyCharacter("emma", "Emma", 0xFFFF7A85, "Bright, and stubborn about small wins."),
        JourneyCharacter("charles", "Charles", 0xFFA78BFA, "A patient planner who trusts the process."),
        JourneyCharacter("georgia", "Georgia", 0xFFFF9F6B, "A bold early starter who sets the pace.")
    )

    fun byId(id: String?) = all.firstOrNull { it.id == id } ?: all.first()
}
