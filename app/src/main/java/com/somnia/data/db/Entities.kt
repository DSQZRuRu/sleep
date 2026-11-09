package com.somnia.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

object SessionSource {
    const val SENSORS = "SENSORS"
    const val PHONE_USAGE = "PHONE_USAGE"
}

@Entity(tableName = "sleep_sessions")
data class SleepSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTime: Long,
    val endTime: Long? = null,
    val source: String = SessionSource.SENSORS,
    val micEnabled: Boolean = false,
    val alarmTargetTime: Long? = null,
    val alarmRangAt: Long? = null,
    val sleepOnset: Long? = null,
    val finalWake: Long? = null,
    val totalSleepMin: Int = 0,
    val lightMin: Int = 0,
    val deepMin: Int = 0,
    val awakeMin: Int = 0,
    val latencyMin: Int = 0,
    val awakenings: Int = 0,
    val efficiency: Float = 0f,
    val score: Int = 0,
    val snoreMinutes: Int = 0,
    val targetBedMin: Int = 0,
    val targetWakeMin: Int = 0,
    val morningRating: Int? = null,
    val processed: Boolean = false,
    val rewardsSeen: Boolean = false,
)

@Entity(
    tableName = "epochs",
    foreignKeys = [ForeignKey(
        entity = SleepSessionEntity::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("sessionId")],
)
data class EpochEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val timestamp: Long,
    val motionCount: Float,
    val noiseDb: Float = 0f,
    val loudEvents: Int = 0,
    val snoreLike: Boolean = false,
    val phoneUsed: Boolean = false,
    val stage: Int = -1,
)

@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: Int = 0,
    val onboardingDone: Boolean = false,
    val targetWakeMin: Int = 7 * 60,
    val earliestBedMin: Int? = null,
    val latestBedMin: Int? = null,
    val weekendDifferent: Boolean = false,
    val latencyMin: Int = 15,
    val cycleMin: Int = 90,
    val preferredCycles: Int = 5,
    val chronotypeScore: Int = 0,
    val chronotype: String = "NEITHER",
    val nightsLearned: Int = 0,
    val micEnabled: Boolean = false,
    val smartAlarmEnabled: Boolean = true,
    val smartWindowMin: Int = 30,
)

@Entity(tableName = "game_state")
data class GameStateEntity(
    @PrimaryKey val id: Int = 0,
    val totalXp: Int = 0,
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val lastStreakNightDay: Long? = null,
    val freezeUsedWeek: Int? = null,
    val companionCare: Int = 0,
)

@Entity(tableName = "creature_catches", indices = [Index("creatureId")])
data class CreatureCatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val creatureId: String,
    val sessionId: Long,
    val caughtAt: Long,
)

@Entity(tableName = "badges")
data class BadgeUnlockEntity(
    @PrimaryKey val badgeId: String,
    val unlockedAt: Long,
)

@Entity(tableName = "challenge_completions", primaryKeys = ["weekKey", "challengeId"])
data class ChallengeCompletionEntity(
    val weekKey: Int,
    val challengeId: String,
    val completedAt: Long,
)

/** Récompenses figées d'une nuit, relues par l'écran du matin. Listes encodées en texte simple. */
@Entity(tableName = "night_rewards")
data class NightRewardEntity(
    @PrimaryKey val sessionId: Long,
    /** "label|xp" séparés par ";" */
    val xpLines: String,
    val creatureIds: String,
    val newCreatureIds: String,
    val badgeIds: String,
    val challengeIds: String,
    val streakAfter: Int,
    val freezeUsed: Boolean,
    val streakBroken: Boolean,
    val levelBefore: Int,
    val levelAfter: Int,
    val stageBefore: String,
    val stageAfter: String,
    val totalXpAfter: Int,
)
