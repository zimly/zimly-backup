package app.zimly.backup.data.db.remote

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import app.zimly.backup.data.media.SourceType

@Entity
data class Remote(
    @PrimaryKey val uid: Int?,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "url") val url: String,
    @ColumnInfo(name = "key") val key: String,
    @ColumnInfo(name = "secret") val secret: String,
    @ColumnInfo(name = "bucket") val bucket: String,
    @ColumnInfo(name = "region") val region: String?,
    @ColumnInfo(name = "source_type", defaultValue = "MEDIA") val sourceType: SourceType,
    @ColumnInfo(name = "source_uri") val sourceUri: String,
)
