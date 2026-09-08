package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
  entities = [
    ConversationEntity::class,
    ChatMessageEntity::class,
    GeneratedImageEntity::class,
    StudySessionEntity::class,
    MemoryEntity::class,
    GeneratedSongEntity::class,
  ],
  version = 6,
  exportSchema = false
)
abstract class KasaDatabase : RoomDatabase() {

  abstract fun conversationDao(): ConversationDao
  abstract fun generatedImageDao(): GeneratedImageDao
  abstract fun studySessionDao(): StudySessionDao
  abstract fun memoryDao(): MemoryDao
  abstract fun generatedSongDao(): GeneratedSongDao

  companion object {
    @Volatile
    private var INSTANCE: KasaDatabase? = null

    val MIGRATION_1_2 = object : Migration(1, 2) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
          CREATE TABLE IF NOT EXISTS `generated_images` (
            `id` TEXT NOT NULL,
            `user_id` TEXT NOT NULL,
            `prompt` TEXT NOT NULL,
            `revised_prompt` TEXT,
            `aspect_ratio` TEXT NOT NULL,
            `style_preset` TEXT NOT NULL,
            `local_file_path` TEXT NOT NULL,
            `mime_type` TEXT NOT NULL,
            `width` INTEGER NOT NULL,
            `height` INTEGER NOT NULL,
            `created_at` INTEGER NOT NULL,
            `is_favorite` INTEGER NOT NULL,
            PRIMARY KEY(`id`)
          )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_generated_images_user_id` ON `generated_images` (`user_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_generated_images_created_at` ON `generated_images` (`created_at`)")
      }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
          CREATE TABLE IF NOT EXISTS `study_sessions` (
            `id` TEXT NOT NULL,
            `user_id` TEXT NOT NULL,
            `subject` TEXT NOT NULL,
            `topic` TEXT NOT NULL,
            `education_level` TEXT NOT NULL,
            `difficulty` TEXT NOT NULL,
            `total_questions` INTEGER NOT NULL,
            `correct_questions` INTEGER NOT NULL,
            `score_percentage` INTEGER NOT NULL,
            `created_at` INTEGER NOT NULL,
            `submissions_json` TEXT NOT NULL,
            PRIMARY KEY(`id`)
          )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_study_sessions_user_id` ON `study_sessions` (`user_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_study_sessions_created_at` ON `study_sessions` (`created_at`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_study_sessions_subject` ON `study_sessions` (`subject`)")
      }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
          CREATE TABLE IF NOT EXISTS `memories` (
            `id` TEXT NOT NULL,
            `user_id` TEXT NOT NULL,
            `memory_text` TEXT NOT NULL,
            `created_at` INTEGER NOT NULL,
            `updated_at` INTEGER NOT NULL,
            `is_active` INTEGER NOT NULL,
            PRIMARY KEY(`id`)
          )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_memories_user_id` ON `memories` (`user_id`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_memories_created_at` ON `memories` (`created_at`)")
      }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `chat_messages` ADD COLUMN `isWebSearch` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE `chat_messages` ADD COLUMN `searchSourcesJson` TEXT DEFAULT NULL")
      }
    }

    val MIGRATION_5_6 = object : Migration(5, 6) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
          CREATE TABLE IF NOT EXISTS `generated_songs` (
            `id` TEXT NOT NULL,
            `userId` TEXT NOT NULL,
            `title` TEXT NOT NULL,
            `prompt` TEXT NOT NULL,
            `audioUrl` TEXT NOT NULL,
            `duration` REAL NOT NULL,
            `imageUrl` TEXT,
            `tags` TEXT,
            `genre` TEXT,
            `language` TEXT,
            `isInstrumental` INTEGER NOT NULL,
            `createdAt` INTEGER NOT NULL,
            PRIMARY KEY(`id`)
          )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_generated_songs_userId` ON `generated_songs` (`userId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_generated_songs_createdAt` ON `generated_songs` (`createdAt`)")
      }
    }

    fun getDatabase(context: Context): KasaDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          KasaDatabase::class.java,
          "kasa_ai_database"
        )
          .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
          .fallbackToDestructiveMigration()
          .build()
        INSTANCE = instance
        instance
      }
    }
  }
}

