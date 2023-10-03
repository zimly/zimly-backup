package io.zeitmaschine.zimzync

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [Remote::class],
    version = 2,
    autoMigrations = [
        AutoMigration(from = 1, to = 2)
    ]
)
abstract class ZimDatabase : RoomDatabase() {
    abstract fun remoteDao(): RemoteDao
}