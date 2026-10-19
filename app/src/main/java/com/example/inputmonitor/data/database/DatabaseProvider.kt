package com.example.inputmonitor.data.database

import android.content.Context
import androidx.room.Room
import com.example.inputmonitor.data.security.DatabaseKeyManager
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

/**
 * Factory for building production encrypted and test in-memory Room database instances (DEC-005).
 */
object DatabaseProvider {

    /**
     * Builds the production encrypted Room database instance.
     * Uses SQLCipher with Android Keystore managed AES-256 key.
     */
    fun createEncryptedDatabase(
        context: Context,
        keyManager: DatabaseKeyManager = DatabaseKeyManager(context)
    ): AppDatabase {
        try {
            System.loadLibrary("sqlcipher")
        } catch (_: Throwable) {
            // Ignored if already loaded or in test environment
        }
        val passphrase = keyManager.getOrCreateDatabasePassphrase()
        val factory = SupportOpenHelperFactory(passphrase)

        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .openHelperFactory(factory)
            .fallbackToDestructiveMigration()
            .build()
    }

    /**
     * Builds an in-memory database instance for testing.
     */
    fun createInMemoryDatabase(context: Context): AppDatabase {
        return Room.inMemoryDatabaseBuilder(
            context.applicationContext,
            AppDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
    }
}
