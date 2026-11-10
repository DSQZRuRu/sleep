package com.somnia.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SleepDao {
    @Insert
    suspend fun insertSession(session: SleepSessionEntity): Long

    @Update
    suspend fun updateSession(session: SleepSessionEntity)

    @Query("SELECT * FROM sleep_sessions WHERE id = :id")
    suspend fun session(id: Long): SleepSessionEntity?

    @Query("SELECT * FROM sleep_sessions WHERE id = :id")
    fun observeSession(id: Long): Flow<SleepSessionEntity?>

    @Query("SELECT * FROM sleep_sessions WHERE endTime IS NULL ORDER BY startTime DESC LIMIT 1")
    suspend fun openSession(): SleepSessionEntity?

    @Query("SELECT * FROM sleep_sessions WHERE processed = 1 AND isNap = 0 ORDER BY startTime DESC")
    fun observeProcessedSessions(): Flow<List<SleepSessionEntity>>

    @Query("SELECT * FROM sleep_sessions WHERE processed = 1 AND isNap = 0 ORDER BY startTime DESC LIMIT :limit")
    suspend fun recentProcessed(limit: Int): List<SleepSessionEntity>

    @Query("SELECT * FROM sleep_sessions WHERE processed = 1 AND isNap = 0 AND startTime >= :from ORDER BY startTime ASC")
    suspend fun processedSince(from: Long): List<SleepSessionEntity>

    @Query("SELECT COUNT(*) FROM sleep_sessions WHERE processed = 1 AND isNap = 0")
    suspend fun processedCount(): Int

    @Query("SELECT * FROM sleep_sessions WHERE processed = 1 AND isNap = 0 AND rewardsSeen = 0 ORDER BY startTime DESC LIMIT 1")
    fun observePendingReveal(): Flow<SleepSessionEntity?>

    @Query("SELECT * FROM sleep_sessions WHERE isNap = 0 AND startTime BETWEEN :from AND :to LIMIT 1")
    suspend fun sessionStartedBetween(from: Long, to: Long): SleepSessionEntity?

    @Query("UPDATE sleep_sessions SET dreamNote = :note WHERE id = :id")
    suspend fun setDreamNote(id: Long, note: String)

    @Query("SELECT * FROM sleep_sessions WHERE processed = 1 AND isNap = 1 ORDER BY startTime DESC LIMIT 20")
    fun observeNaps(): Flow<List<SleepSessionEntity>>

    @Query("UPDATE sleep_sessions SET factors = :factors WHERE id = :id")
    suspend fun setFactors(id: Long, factors: String)

    @Query("SELECT * FROM sleep_sessions WHERE processed = 1 AND isNap = 0 AND startTime >= :from AND startTime < :to ORDER BY startTime ASC")
    suspend fun processedBetween(from: Long, to: Long): List<SleepSessionEntity>

    @Query("DELETE FROM sleep_sessions WHERE id = :id")
    suspend fun deleteSession(id: Long)

    @Insert
    suspend fun insertEpoch(epoch: EpochEntity)

    @Update
    suspend fun updateEpochs(epochs: List<EpochEntity>)

    @Query("SELECT * FROM epochs WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun epochs(sessionId: Long): List<EpochEntity>

    @Query("SELECT * FROM epochs WHERE sessionId = :sessionId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun lastEpochs(sessionId: Long, limit: Int): List<EpochEntity>
}

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profile WHERE id = 0")
    fun observe(): Flow<ProfileEntity?>

    @Query("SELECT * FROM profile WHERE id = 0")
    suspend fun get(): ProfileEntity?

    @Upsert
    suspend fun upsert(profile: ProfileEntity)
}

@Dao
interface GameDao {
    @Query("SELECT * FROM game_state WHERE id = 0")
    fun observeState(): Flow<GameStateEntity?>

    @Query("SELECT * FROM game_state WHERE id = 0")
    suspend fun state(): GameStateEntity?

    @Upsert
    suspend fun upsertState(state: GameStateEntity)

    @Insert
    suspend fun insertCatches(catches: List<CreatureCatchEntity>)

    @Query("SELECT * FROM creature_catches ORDER BY caughtAt DESC")
    fun observeCatches(): Flow<List<CreatureCatchEntity>>

    @Query("SELECT * FROM creature_catches")
    suspend fun catches(): List<CreatureCatchEntity>

    @Query("SELECT * FROM creature_catches WHERE caughtAt >= :from AND caughtAt < :to")
    suspend fun catchesBetween(from: Long, to: Long): List<CreatureCatchEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBadges(badges: List<BadgeUnlockEntity>)

    @Query("SELECT * FROM badges")
    fun observeBadges(): Flow<List<BadgeUnlockEntity>>

    @Query("SELECT * FROM badges")
    suspend fun badges(): List<BadgeUnlockEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertChallengeCompletions(items: List<ChallengeCompletionEntity>)

    @Query("SELECT * FROM challenge_completions WHERE weekKey = :week")
    fun observeCompletions(week: Int): Flow<List<ChallengeCompletionEntity>>

    @Query("SELECT * FROM challenge_completions WHERE weekKey = :week")
    suspend fun completions(week: Int): List<ChallengeCompletionEntity>

    @Query("SELECT COUNT(*) FROM challenge_completions")
    suspend fun completionCount(): Int

    @Query("SELECT * FROM creature_progress")
    fun observeProgress(): Flow<List<CreatureProgressEntity>>

    @Query("SELECT * FROM creature_progress")
    suspend fun progress(): List<CreatureProgressEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProgress(items: List<CreatureProgressEntity>)

    @Query("UPDATE creature_progress SET slot = NULL WHERE slot = :slot")
    suspend fun clearSlot(slot: Int)

    @Query("UPDATE creature_progress SET slot = :slot WHERE creatureId = :id")
    suspend fun setSlot(id: String, slot: Int?)

    @Query("UPDATE creature_progress SET level = :level WHERE creatureId = :id")
    suspend fun setLevel(id: String, level: Int)

    @Query("SELECT * FROM decor_owned")
    fun observeDecorOwned(): Flow<List<DecorOwnedEntity>>

    @Query("SELECT * FROM decor_owned")
    suspend fun decorOwned(): List<DecorOwnedEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDecorOwned(item: DecorOwnedEntity)

    @Query("SELECT * FROM room_layout")
    fun observeLayout(): Flow<List<RoomLayoutEntity>>

    @Query("SELECT * FROM room_layout")
    suspend fun layout(): List<RoomLayoutEntity>

    @Upsert
    suspend fun upsertLayout(item: RoomLayoutEntity)

    // ---------- Outils développeur ----------
    @Query("UPDATE creature_progress SET level = 1")
    suspend fun resetCreatureLevels()

    @Query("DELETE FROM decor_owned")
    suspend fun clearDecorOwned()

    @Query("DELETE FROM room_layout")
    suspend fun clearLayout()

    /** Poussière réellement gagnée par les nuits enregistrées. */
    @Query("SELECT COALESCE(SUM(stardustGained), 0) FROM night_rewards")
    suspend fun stardustEarnedFromNights(): Int

    @Upsert
    suspend fun upsertReward(reward: NightRewardEntity)

    @Query("SELECT * FROM night_rewards WHERE sessionId = :sessionId")
    suspend fun reward(sessionId: Long): NightRewardEntity?
}
