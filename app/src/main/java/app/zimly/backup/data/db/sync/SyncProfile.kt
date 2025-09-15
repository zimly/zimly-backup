package app.zimly.backup.data.db.sync

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import app.zimly.backup.data.media.ContentType

@Entity(tableName = "sync_profile")
data class SyncProfile(
    @PrimaryKey val uid: Int?, // TODO Long? Save the casts
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "url") val url: String,
    @ColumnInfo(name = "key") val key: String,
    @ColumnInfo(name = "secret") val secret: String,
    @ColumnInfo(name = "bucket") val bucket: String,
    @ColumnInfo(name = "region") val region: String?,
    @ColumnInfo(name = "virtual_hosted_style", defaultValue = "0") val virtualHostedStyle: Boolean,
    @ColumnInfo(name = "content_type", defaultValue = "MEDIA") val contentType: ContentType,
    @ColumnInfo(name = "direction", defaultValue = "UPLOAD") val direction: SyncDirection,
)
