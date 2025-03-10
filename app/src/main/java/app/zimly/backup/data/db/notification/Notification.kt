package app.zimly.backup.data.db.notification

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// Unique column constraint with Index workaround
@Entity(indices = [Index(value = ["type"], unique = true)])
data class Notification(
    @PrimaryKey val uid: Int?,
    @ColumnInfo(name = "type") val type: NotificationType,
    @ColumnInfo(name = "ignore") val ignore: Boolean = false,
)
