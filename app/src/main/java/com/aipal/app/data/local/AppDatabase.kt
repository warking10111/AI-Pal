package com.aipal.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.aipal.app.data.local.dao.*
import com.aipal.app.data.local.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

@Database(
    entities = [
        AIPersona::class,
        ChatConversation::class,
        ChatMessage::class,
        AIMemory::class,
        UserEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun personaDao(): AIPersonaDao
    abstract fun conversationDao(): ChatConversationDao
    abstract fun messageDao(): ChatMessageDao
    abstract fun memoryDao(): AIMemoryDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Table: memories
                db.execSQL("ALTER TABLE memories ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE memories ADD COLUMN isFavourite INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE memories ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE memories ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")

                // Table: conversations
                db.execSQL("ALTER TABLE conversations ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE conversations ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE conversations ADD COLUMN isFavourite INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE conversations ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE conversations ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE conversations ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE conversations ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'synced'")

                // Table: messages
                db.execSQL("ALTER TABLE messages ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE messages ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE messages ADD COLUMN isFavourite INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE messages ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE messages ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE messages ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE messages ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'synced'")

                // Table: personas
                db.execSQL("ALTER TABLE personas ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE personas ADD COLUMN isArchived INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE personas ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE personas ADD COLUMN isFavourite INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE personas ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE personas ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE personas ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE personas ADD COLUMN syncStatus TEXT NOT NULL DEFAULT 'synced'")
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "aipal_database"
                )
                .addMigrations(MIGRATION_3_4)
                .fallbackToDestructiveMigration()
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        prepopulateDatabase(database)
                    }
                }
            }

            private suspend fun prepopulateDatabase(db: AppDatabase) {
                val personaDao = db.personaDao()
                val memoryDao = db.memoryDao()

                // Prepopulate specialized prebuilt personas/agents
                val defaultPersonas = listOf(
                    AIPersona(
                        id = "agent_programmer",
                        name = "Coder Pro",
                        avatar = "💻",
                        prompt = "You are Coder Pro, an expert programming assistant. Provide clean, well-commented, and highly-optimized code blocks, and suggest security and runtime performance improvements.",
                        voiceName = "Kore",
                        temperature = 0.4f,
                        isCustom = false,
                        description = "Expert in Kotlin, Jetpack Compose, Python, debugging, and system design."
                    ),
                    AIPersona(
                        id = "agent_teacher",
                        name = "Socrates",
                        avatar = "🎓",
                        prompt = "You are Socrates, a highly patient academic teacher. Break down complex topics step-by-step using analogies, questions, and summaries.",
                        voiceName = "Leda",
                        temperature = 0.7f,
                        isCustom = false,
                        description = "Learn math, science, history, literature, or languages clearly."
                    ),
                    AIPersona(
                        id = "agent_fitness",
                        name = "Coach Flex",
                        avatar = "💪",
                        prompt = "You are Coach Flex, an encouraging fitness instructor and athletic trainer. Provide tailored exercise regimes, custom dietary advice, and training schedules.",
                        voiceName = "Puck",
                        temperature = 0.8f,
                        isCustom = false,
                        description = "Custom workout routines, nutritional habits, and core fitness advice."
                    ),
                    AIPersona(
                        id = "agent_travel",
                        name = "Wanderlust",
                        avatar = "✈️",
                        prompt = "You are Wanderlust, an expert globetrotter and travel guide. Recommend complete city itineraries, hidden local gems, cost estimates, and cultural guidelines.",
                        voiceName = "Kore",
                        temperature = 0.8f,
                        isCustom = false,
                        description = "Personalized holiday planners, local spots, and budget guides."
                    ),
                    AIPersona(
                        id = "agent_law",
                        name = "Legal Guide",
                        avatar = "⚖️",
                        prompt = "You are Legal Guide, a highly analytical law informational helper. Conceptualize legal principles, terms, and contract structures clearly. Emphasize that you provide educational information, not actual legal advice.",
                        voiceName = "Fenrir",
                        temperature = 0.5f,
                        isCustom = false,
                        description = "Educational breakdowns of statutes, business contracts, and rules."
                    ),
                    AIPersona(
                        id = "agent_medical",
                        name = "Health Assistant",
                        avatar = "🩺",
                        prompt = "You are Health Assistant, a supportive, professional, and comforting medical facts AI. Answer wellness, fitness, and symptomatic questions conceptually based on clinical general studies. Always remind the user that your output is for information and does not replace actual physical doctor checkups.",
                        voiceName = "Leda",
                        temperature = 0.6f,
                        isCustom = false,
                        description = "General symptomatic reviews, nutrition facts, and wellness guides."
                    )
                )

                for (persona in defaultPersonas) {
                    personaDao.insertPersona(persona)
                }

                // Initial Memories
                memoryDao.insertMemory(
                    AIMemory(
                        id = "init_mem_1",
                        content = "User name is Hein.",
                        category = "Preferences",
                        isEnabled = true,
                        importanceScore = 5,
                        tags = "identity,personal",
                        createdDate = System.currentTimeMillis(),
                        modifiedDate = System.currentTimeMillis(),
                        syncStatus = "synced",
                        isPinned = true,
                        isArchived = false
                    )
                )
                memoryDao.insertMemory(
                    AIMemory(
                        id = "init_mem_2",
                        content = "Prefers dark slate visual themes.",
                        category = "Preferences",
                        isEnabled = true,
                        importanceScore = 3,
                        tags = "ui,theme",
                        createdDate = System.currentTimeMillis(),
                        modifiedDate = System.currentTimeMillis(),
                        syncStatus = "synced",
                        isPinned = false,
                        isArchived = false
                    )
                )
                memoryDao.insertMemory(
                    AIMemory(
                        id = "init_mem_3",
                        content = "Primary language of focus is Kotlin & Jetpack Compose.",
                        category = "Writing Style",
                        isEnabled = true,
                        importanceScore = 4,
                        tags = "code,android",
                        createdDate = System.currentTimeMillis(),
                        modifiedDate = System.currentTimeMillis(),
                        syncStatus = "synced",
                        isPinned = false,
                        isArchived = false
                    )
                )
            }
        }
    }
}
