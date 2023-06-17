package io.zeitmaschine.zimzync

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity
data class Log(
    @PrimaryKey val uid: Int?,
    @ColumnInfo(name = "date") val date: Date,
    @ColumnInfo(name = "remote_id") val remoteId: Int,
    @ColumnInfo(name = "worker_id") val workerId: Int,
    @ColumnInfo(name = "status") val status: Status,
    @ColumnInfo(name = "files") val files: Long,
    @ColumnInfo(name = "size") val size: Long,
)

enum class Status {
    IN_PROGRESS, SUCCESS, ERROR, CANCEL
}