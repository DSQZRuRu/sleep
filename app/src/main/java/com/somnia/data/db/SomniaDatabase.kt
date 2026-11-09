package com.somnia.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
        CreatureProgressEntity::class,
    ],
    version = SomniaDatabase.VERSION,
    exportSchema = true,
)
abstract class SomniaDatabase : RoomDatabase() {
    abstract fun sleepDao(): SleepDao
    abstract fun profileDao(): ProfileDao
    abstract fun gameDao(): GameDao

    companion object {
        const val VERSION = 2
        const val NAME = "somnia.db"

        /** v2 : Éclat de rêve, poussière d'étoiles, niveaux et lit des créatures, événements. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE game_state ADD COLUMN stardust INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE game_state ADD COLUMN bestDream INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE night_rewards ADD COLUMN dreamBase INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE night_rewards ADD COLUMN dreamScore INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE night_rewards ADD COLUMN dreamLines TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE night_rewards ADD COLUMN stardustGained INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE night_rewards ADD COLUMN duplicateStardust INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE night_rewards ADD COLUMN eventId TEXT")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `creature_progress` (`creatureId` TEXT NOT NULL, " +
                        "`level` INTEGER NOT NULL, `slot` INTEGER, PRIMARY KEY(`creatureId`))"
                )
                db.execSQL("INSERT OR IGNORE INTO creature_progress (creatureId, level, slot) SELECT DISTINCT creatureId, 1, NULL FROM creature_catches")
                // Cadeau de bienvenue pour découvrir la montée en niveau.
                db.execSQL("UPDATE game_state SET stardust = stardust + 100")
            }
        }

        fun create(context: Context): SomniaDatabase =
            Room.databaseBuilder(context, SomniaDatabase::class.java, NAME)
                .addMigrations(MIGRATION_1_2)
                .build()
    }
}
