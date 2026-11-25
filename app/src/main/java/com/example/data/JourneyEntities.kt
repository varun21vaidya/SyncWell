package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** The single Journey row (id is always 1). Position fields feed [com.example.domain.JourneyPosition]. */
@Entity(tableName = "journey_state")
data class JourneyState(
    @PrimaryKey val id: Int = 1,
    val enabled: Boolean = false,
    val characterId: String = "max",
    val regionIndex: Int = 0,
    val stepInRegion: Int = 0,
    val furthestRegion: Int = 0,
    val freezeUsedWeekStart: String? = null,
    val lastProcessedDate: String? = null, // restored with a backup, otherwise old days would be replayed
    val ambitionText: String? = null,
    val identityText: String? = null,
    val ambitionStart: String? = null,
    val targetDate: String? = null,
    val bigRewardText: String? = null,
    val bigRewardUnlockedAt: String? = null
)

/** A weekly goal or a region commitment. */
@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey val id: String,
    val title: String,
    val kind: String, // WEEKLY or COMMITMENT
    val mandatory: Boolean,
    val measure: String, // COUNT, DURATION, MILESTONE, PERCENT
    val target: Double,
    val targetLabel: String? = null,
    val focusLinked: Boolean = false, // DURATION only
    val windowStart: String,
    val windowEnd: String,
    val status: String = "PENDING", // PENDING, MET, MISSED
    val regionIndex: Int? = null, // commitments
    val rewardText: String? = null,
    val rewardClaimedAt: String? = null,
    val planText: String? = null, // when and where
    val planTime: String? = null,
    val ifThenText: String? = null
)

@Entity(
    tableName = "goal_progress",
    foreignKeys = [ForeignKey(entity = Goal::class, parentColumns = ["id"], childColumns = ["goalId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index(value = ["goalId"])]
)
data class GoalProgress(
    @PrimaryKey val id: String,
    val goalId: String,
    val date: String, // YYYY-MM-DD
    val amount: Double,
    val note: String? = null
)

/** A stamp earned by crossing a region or meeting a commitment. */
@Entity(tableName = "passport")
data class PassportStamp(
    @PrimaryKey val id: String,
    val kind: String, // REGION or COMMITMENT
    val regionIndex: Int,
    val earnedAt: String,
    val label: String
)

@Entity(tableName = "journey_notes")
data class JourneyNote(
    @PrimaryKey val id: String,
    val date: String, // YYYY-MM-DD
    val kind: String, // REFLECTION or LOG
    val text: String
)
