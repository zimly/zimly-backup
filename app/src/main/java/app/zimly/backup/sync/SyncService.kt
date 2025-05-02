package app.zimly.backup.sync

import kotlinx.coroutines.flow.Flow

interface SyncService {
    fun synchronize(): Flow<SyncProgress>
}
