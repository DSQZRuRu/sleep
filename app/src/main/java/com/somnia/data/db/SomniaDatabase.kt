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
        DecorOwnedEntity::class,
        RoomLayoutEntity::class,
    ],
    version = SomniaDatabase.VERSION,
    exportSchema = true,
)
abstract class SomniaDatabase : RoomDatabase() {
    abstract fun sleepDao(): SleepDao
    abstract fun profileDao(): ProfileDao
    abstract fun gameDao(): GameDao

    companion object {
        const val VERSION = 4
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

        /** v3 : facteurs du quotidien, rituel du soir, chambre décorable, expéditions, pleine lune. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sleep_sessions ADD COLUMN factors TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE sleep_sessions ADD COLUMN ritualDone INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE game_state ADD COLUMN expeditionRegion INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE game_state ADD COLUMN expeditionProgress INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE night_rewards ADD COLUMN expeditionSteps INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE night_rewards ADD COLUMN regionsCompleted TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE night_rewards ADD COLUMN fullMoon INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE TABLE IF NOT EXISTS `decor_owned` (`itemId` TEXT NOT NULL, `purchasedAt` INTEGER NOT NULL, PRIMARY KEY(`itemId`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `room_layout` (`slot` TEXT NOT NULL, `itemId` TEXT NOT NULL, PRIMARY KEY(`slot`))")
            }
        }

        /** v4 : journal de rêves, siestes. */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sleep_sessions ADD COLUMN dreamNote TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE sleep_sessions ADD COLUMN isNap INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun create(context: Context): SomniaDatabase =
            Room.databaseBuilder(context, SomniaDatabase::class.java, NAME)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()
    }
}
