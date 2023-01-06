package io.zeitmaschine.zimzync

import androidx.room.RoomDatabase

abstract class ZimDatabase : RoomDatabase() {
    abstract fun remoteDao(): RemoteDao
}