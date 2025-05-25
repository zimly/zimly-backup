package app.zimly.backup.sync

data class SyncProgress(
    val transferredBytes: Long,
    val transferredFiles: Int,
    val percentage: Float,
    val bytesPerSecond: Long?,
    val totalFiles: Int,
    val totalBytes: Long
) {
    companion object {
        val EMPTY = SyncProgress(0, 0, 0F, null, 0, 0)
    }
}
