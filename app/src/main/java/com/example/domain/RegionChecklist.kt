package com.example.domain

/** One thing wrong with a region's data. [blocking] issues must be fixed before the region ships; art is the one that may lag. */
data class RegionIssue(val region: String, val issue: String, val blocking: Boolean = true)

/**
 * The weekly region pipeline as a check: painted image, traced points, checkpoint flag, intro line, Warden, release date.
 * Run over the bundled route in a test, so a region added to `adventure_route.json` cannot ship half finished.
 */
object RegionChecklist {
    private val DATE = Regex("^\\d{4}-\\d{2}-\\d{2}$")

    fun check(regions: List<RegionDef>): List<RegionIssue> = regions.flatMapIndexed { i, r -> checkOne(r, regions.getOrNull(i - 1)) }

    private fun checkOne(r: RegionDef, previous: RegionDef?): List<RegionIssue> {
        fun bad(text: String) = RegionIssue(r.name, text)
        return listOfNotNull(
            bad("has no name").takeIf { r.name.isBlank() },
            bad("has ${r.steps} points, the route needs ${JourneyRoute.stepsIn(r.index)}").takeIf { r.steps != JourneyRoute.stepsIn(r.index) },
            bad("points are not numbered 1 to ${r.steps} in order").takeIf { r.points.map { it.number } != (1..r.steps).toList() },
            bad("has a point outside the image").takeIf { r.points.any { it.x !in 0f..1f || it.y !in 0f..1f } },
            bad("has two points on the same spot").takeIf { r.points.zipWithNext().any { (a, b) -> a.x == b.x && a.y == b.y } },
            bad("checkpoint is on ${r.points.filter { it.checkpoint }.map { it.number }}, the route needs ${JourneyRoute.checkpointOf(r.index)}")
                .takeIf { r.points.filter { it.checkpoint }.map { it.number } != listOfNotNull(JourneyRoute.checkpointOf(r.index)) },
            bad("has no intro line").takeIf { r.intro.isBlank() },
            bad("has an incomplete Warden").takeIf { listOf(r.warden.name, r.warden.personality, r.warden.challenge, r.warden.open, r.warden.closed).any { it.isBlank() } },
            bad("has no valid release date").takeIf { !DATE.matches(r.releaseDate) },
            bad("is released before the region ahead of it").takeIf { previous != null && r.releaseDate < previous.releaseDate },
            RegionIssue(r.name, "has no painted image yet", blocking = false).takeIf { r.image == null }
        )
    }
}
