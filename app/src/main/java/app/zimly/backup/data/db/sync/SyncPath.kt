package app.zimly.backup.data.db.sync

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "sync_path",
    foreignKeys = [ForeignKey(
        entity = SyncProfile::class,
        parentColumns = ["uid"],
        childColumns = ["profileId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class SyncPath(
    @PrimaryKey val uid: Int?,
    val profileId: Int,
    val uri: String,
)