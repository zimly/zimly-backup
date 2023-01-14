package io.zeitmaschine.zimzync

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Remote(
    @PrimaryKey val uid: Int?,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "url") val url: String,
    @ColumnInfo(name = "key") val key: String,
    @ColumnInfo(name = "secret") val secret: String,
)
