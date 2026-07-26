package com.example.securenote.data

import android.content.Context
import com.example.securenote.security.VaultKeyManager

interface AppContainer {
    val context: Context
    val vaultKeyManager: VaultKeyManager
    val session: VaultSession
}

class DefaultAppContainer(appContext: Context) : AppContainer {
    override val context: Context = appContext.applicationContext
    override val vaultKeyManager: VaultKeyManager = VaultKeyManager(context)
    override val session: VaultSession = VaultSession(context)
}

/**
 * Holds the open, decrypted database for the lifetime of an unlocked session.
 * Call [lock] to zeroize the key and close the DB.
 */
class VaultSession(private val appContext: Context) {
    @Volatile private var database: AppDatabase? = null
    @Volatile private var key: ByteArray? = null

    val isUnlocked: Boolean get() = database != null

    fun unlock(rawKey: ByteArray) {
        lock()
        key = rawKey.copyOf()
        database = AppDatabase.open(appContext, rawKey)
    }

    fun lock() {
        database?.close()
        database = null
        key?.fill(0)
        key = null
    }

    fun repository(): NoteRepository {
        val db = database ?: error("Vault is locked")
        return NoteRepository(db.noteDao())
    }
}
