package com.example.service

import com.example.data.Goal
import com.example.data.GoalProgress
import com.example.data.JourneyNote
import com.example.data.JourneyState
import com.example.data.PassportStamp
import com.example.data.SyncWellDao
import org.json.JSONArray
import org.json.JSONObject

/**
 * Backup and restore of the Journey tables, kept out of CloudBackupService so that file does not grow.
 * Every key is optional on restore: an older backup has none of them and leaves the defaults alone.
 * The state row is restored with its lastProcessedDate, otherwise old days would be replayed and cause a wrong setback.
 */
internal object JourneyBackup {

    suspend fun write(root: JSONObject, dao: SyncWellDao) {
        dao.getJourneyState()?.let { s ->
            root.put("journeyState", JSONObject().apply {
                put("enabled", s.enabled); put("characterId", s.characterId); put("regionIndex", s.regionIndex)
                put("stepInRegion", s.stepInRegion); put("furthestRegion", s.furthestRegion)
                putOpt("freezeUsedWeekStart", s.freezeUsedWeekStart); putOpt("lastProcessedDate", s.lastProcessedDate)
                putOpt("ambitionText", s.ambitionText); putOpt("identityText", s.identityText)
                putOpt("ambitionStart", s.ambitionStart); putOpt("targetDate", s.targetDate)
                putOpt("bigRewardText", s.bigRewardText); putOpt("bigRewardUnlockedAt", s.bigRewardUnlockedAt)
            })
        }
        root.put("goals", array(dao.getAllGoals()) { g ->
            put("id", g.id); put("title", g.title); put("kind", g.kind); put("mandatory", g.mandatory)
            put("measure", g.measure); put("target", g.target); putOpt("targetLabel", g.targetLabel)
            put("focusLinked", g.focusLinked); put("windowStart", g.windowStart); put("windowEnd", g.windowEnd)
            put("status", g.status); putOpt("regionIndex", g.regionIndex); putOpt("rewardText", g.rewardText)
            putOpt("rewardClaimedAt", g.rewardClaimedAt); putOpt("planText", g.planText)
            putOpt("planTime", g.planTime); putOpt("ifThenText", g.ifThenText)
        })
        root.put("goalProgress", array(dao.getAllGoalProgress()) { p ->
            put("id", p.id); put("goalId", p.goalId); put("date", p.date); put("amount", p.amount); putOpt("note", p.note)
        })
        root.put("passport", array(dao.getAllPassportStamps()) { s ->
            put("id", s.id); put("kind", s.kind); put("regionIndex", s.regionIndex); put("earnedAt", s.earnedAt); put("label", s.label)
        })
        root.put("journeyNotes", array(dao.getAllJourneyNotes()) { n ->
            put("id", n.id); put("date", n.date); put("kind", n.kind); put("text", n.text)
        })
    }

    suspend fun read(root: JSONObject, dao: SyncWellDao) {
        root.optJSONObject("journeyState")?.let { o ->
            dao.upsertJourneyState(JourneyState(
                enabled = o.optBoolean("enabled", false), characterId = o.optString("characterId", "max"),
                regionIndex = o.optInt("regionIndex", 0), stepInRegion = o.optInt("stepInRegion", 0),
                furthestRegion = o.optInt("furthestRegion", 0), freezeUsedWeekStart = o.str("freezeUsedWeekStart"),
                lastProcessedDate = o.str("lastProcessedDate"), ambitionText = o.str("ambitionText"),
                identityText = o.str("identityText"), ambitionStart = o.str("ambitionStart"), targetDate = o.str("targetDate"),
                bigRewardText = o.str("bigRewardText"), bigRewardUnlockedAt = o.str("bigRewardUnlockedAt")
            ))
        }
        // goals before their progress, which points at them
        rows(root, "goals") { o ->
            Goal(
                id = o.getString("id"), title = o.getString("title"), kind = o.optString("kind", "COMMITMENT"),
                mandatory = o.optBoolean("mandatory", true), measure = o.optString("measure", "COUNT"),
                target = o.optDouble("target", 0.0), targetLabel = o.str("targetLabel"), focusLinked = o.optBoolean("focusLinked", false),
                windowStart = o.getString("windowStart"), windowEnd = o.getString("windowEnd"), status = o.optString("status", "PENDING"),
                regionIndex = if (o.has("regionIndex") && !o.isNull("regionIndex")) o.getInt("regionIndex") else null,
                rewardText = o.str("rewardText"), rewardClaimedAt = o.str("rewardClaimedAt"), planText = o.str("planText"),
                planTime = o.str("planTime"), ifThenText = o.str("ifThenText")
            )
        }.takeIf { it.isNotEmpty() }?.let { dao.insertGoals(it) }
        rows(root, "goalProgress") { o ->
            GoalProgress(o.getString("id"), o.getString("goalId"), o.getString("date"), o.optDouble("amount", 0.0), o.str("note"))
        }.takeIf { it.isNotEmpty() }?.let { dao.insertGoalProgress(it) }
        rows(root, "passport") { o ->
            PassportStamp(o.getString("id"), o.optString("kind", "REGION"), o.optInt("regionIndex", 0), o.getString("earnedAt"), o.optString("label", ""))
        }.takeIf { it.isNotEmpty() }?.let { dao.insertPassportStamps(it) }
        rows(root, "journeyNotes") { o ->
            JourneyNote(o.getString("id"), o.getString("date"), o.optString("kind", "LOG"), o.optString("text", ""))
        }.takeIf { it.isNotEmpty() }?.let { dao.insertJourneyNotes(it) }
    }

    private fun <T> array(items: List<T>, fill: JSONObject.(T) -> Unit) =
        JSONArray().also { arr -> items.forEach { arr.put(JSONObject().apply { fill(it) }) } }

    private fun <T> rows(root: JSONObject, key: String, build: (JSONObject) -> T): List<T> =
        root.optJSONArray(key)?.let { arr -> (0 until arr.length()).map { build(arr.getJSONObject(it)) } } ?: emptyList()

    private fun JSONObject.str(key: String): String? = if (has(key) && !isNull(key)) getString(key) else null
}
