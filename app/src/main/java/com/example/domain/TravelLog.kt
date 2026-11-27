package com.example.domain

/** The auto-written travel log: one calm line per thing that happened on the trail. A rest or vacation day writes nothing. */
object TravelLog {

    /** The log line for a Journey event, or null when it is not worth a line. [regionName] maps a region index to its name. */
    fun line(event: JourneyEvent, regionName: (Int) -> String): String? = when (event) {
        is JourneyEvent.Step -> "Day ${event.day} of ${regionName(event.region)}"
        is JourneyEvent.GateReached -> "Reached the gate of ${regionName(event.region)}"
        is JourneyEvent.Setback ->
            if (event.toStep == 0) "Back to the start of ${regionName(event.region)}" else "Back to day ${event.toStep} in ${regionName(event.region)}"
        is JourneyEvent.FreezeUsed -> "Rest day used, no change on the map"
        is JourneyEvent.Vacation -> null
    }

    fun crossed(regionName: String) = "Crossed the gate of $regionName"

    fun metCommitment(title: String) = "Met: $title"

    fun missedCommitment(title: String) = "Not met: $title"

    fun progress(goalTitle: String, note: String) = "$goalTitle: $note"

    /** The day a region's camp sits on, if it has one. A reflection is offered once the character has reached it. */
    fun campDay(region: RegionDef): Int? = region.points.firstOrNull { it.checkpoint }?.number
}
