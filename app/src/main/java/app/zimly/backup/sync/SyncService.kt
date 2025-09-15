package app.zimly.backup.sync

import android.content.Context
import androidx.core.net.toUri
import app.zimly.backup.data.db.sync.SyncProfile
import app.zimly.backup.data.db.sync.SyncDirection
import app.zimly.backup.data.db.sync.SyncDetails
import app.zimly.backup.data.media.ContentType
import app.zimly.backup.data.media.LocalDocumentsResolver
import app.zimly.backup.data.media.LocalMediaResolverImpl
import app.zimly.backup.data.s3.MinioRepository
import kotlinx.coroutines.flow.Flow

interface SyncService {
    fun calculateDiff(): Diff
    fun synchronize(): Flow<SyncProgress>

    companion object {
        /**
         * Provides the [SyncService] based on [SyncProfile].
         */
        fun get(context: Context, syncDetails: SyncDetails): SyncService {

            val s3Repository =
                MinioRepository(syncDetails.profile.url, syncDetails.profile.key, syncDetails.profile.secret, syncDetails.profile.bucket, syncDetails.profile.region, syncDetails.profile.virtualHostedStyle)

            return when (syncDetails.profile.contentType) {
                ContentType.MEDIA -> {
                    val contentResolver = LocalMediaResolverImpl(context, syncDetails.paths[0].uri)
                    UploadSyncService(s3Repository, contentResolver)
                }

                ContentType.FOLDER -> {
                    val contentResolver = LocalDocumentsResolver(context, syncDetails.paths[0].uri.toUri())
                    when (syncDetails.profile.direction) {
                        SyncDirection.UPLOAD -> UploadSyncService(s3Repository, contentResolver)
                        SyncDirection.DOWNLOAD -> DownloadSyncService(
                            s3Repository,
                            contentResolver,
                        )
                    }
                }
            }
        }
    }
}

abstract class Diff {
    abstract var totalObjects: Int
    abstract var totalBytes: Long
}
