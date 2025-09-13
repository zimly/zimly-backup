package app.zimly.backup.data.db.sync

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
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
    @ColumnInfo(name = "uri") val uri: String,
)