package app.zimly.backup.data.db.remote

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import app.zimly.backup.data.media.ContentType

@Entity
data class Remote(
    @PrimaryKey val uid: Int?,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "url") val url: String,
    @ColumnInfo(name = "key") val key: String,
    @ColumnInfo(name = "secret") val secret: String,
    @ColumnInfo(name = "bucket") val bucket: String,
    @ColumnInfo(name = "region") val region: String?,
    @ColumnInfo(name = "content_type", defaultValue = "MEDIA") val contentType: ContentType,
    @ColumnInfo(name = "content_uri") val contentUri: String,
    @ColumnInfo(name = "direction", defaultValue = "UPLOAD") val direction: SyncDirection,
)
