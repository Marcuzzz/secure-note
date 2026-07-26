package com.example.securenote.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

@Database(entities = [Note::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    companion object {
        private const val DB_NAME = "secure_notes.db"

        fun open(context: Context, passphrase: ByteArray): AppDatabase {
            SQLiteDatabase.loadLibs(context.applicationContext)
            val factory = SupportFactory(passphrase.copyOf(), null, /* clearPassphrase */ true)
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DB_NAME
            )
                .openHelperFactory(factory)
                .build()
        }
    }
}
