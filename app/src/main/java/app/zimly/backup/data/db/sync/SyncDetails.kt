package app.zimly.backup.data.db.sync

import androidx.room.Embedded
import androidx.room.Relation

data class SyncDetails(
    @Embedded val profile: SyncProfile,

    @Relation(
        parentColumn = "uid",
        entityColumn = "profile_id"
    )
    val paths: List<SyncPath>
)
