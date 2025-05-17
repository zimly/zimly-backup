package app.zimly.backup.sync

import kotlinx.coroutines.flow.Flow

interface SyncService {
    fun calculateDiff(): Diff
    fun synchronize(): Flow<SyncProgress>
}

abstract class Diff {
    abstract var totalObjects: Int
    abstract var totalBytes: Long
}
