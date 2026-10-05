package com.quickotp.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [OtpAccount::class],
    version = 4,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun otpAccountDao(): OtpAccountDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "quick_otp.db",
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build().also { instance = it }
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE otp_accounts ADD COLUMN kind TEXT NOT NULL DEFAULT 'OTP'",
                )
                db.execSQL(
                    "ALTER TABLE otp_accounts ADD COLUMN barcodeFormat TEXT NOT NULL DEFAULT ''",
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE otp_accounts ADD COLUMN valueVisible INTEGER NOT NULL DEFAULT 1",
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE otp_accounts ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0",
                )
                // Keep the previous newest-first order: rank each row by how many rows are newer.
                db.execSQL(
                    """
                    UPDATE otp_accounts SET sortOrder = (
                        SELECT COUNT(*) FROM otp_accounts AS newer
                        WHERE newer.createdAt > otp_accounts.createdAt
                            OR (newer.createdAt = otp_accounts.createdAt AND newer.id > otp_accounts.id)
                    )
                    """.trimIndent(),
                )
            }
        }
    }
}
