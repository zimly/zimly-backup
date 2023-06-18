package io.zeitmaschine.zimzync

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [Remote::class, Log::class],
    version = 2,
    autoMigrations = [
        AutoMigration(from = 1, to = 2)
    ]
)
@TypeConverters(Converters::class)
abstract class ZimDatabase : RoomDatabase() {
    abstract fun remoteDao(): RemoteDao
}