package io.zeitmaschine.zimzync

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [Remote::class],
    version = 1,
    autoMigrations = []
)
abstract class ZimDatabase : RoomDatabase() {
    abstract fun remoteDao(): RemoteDao
}