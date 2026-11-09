package com.somnia.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        SleepSessionEntity::class,
        EpochEntity::class,
        ProfileEntity::class,
        GameStateEntity::class,
        CreatureCatchEntity::class,
        BadgeUnlockEntity::class,
        ChallengeCompletionEntity::class,
        NightRewardEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class SomniaDatabase : RoomDatabase() {
    abstract fun sleepDao(): SleepDao
    abstract fun profileDao(): ProfileDao
    abstract fun gameDao(): GameDao

    companion object {
        fun create(context: Context): SomniaDatabase =
            Room.databaseBuilder(context, SomniaDatabase::class.java, "somnia.db").build()
    }
}
