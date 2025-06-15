package app.zimly.backup.data.media

import android.net.Uri

/**
 * Represents a local document or media object.
 *
 * [path] and [relPath] have different context in case of Media and Document objects.
 *
 * See [LocalDocumentsResolver]
 */
data class ContentObject(
    var path: String,
    var relPath: String,
    var size: Long,
    var contentType: String,
    val uri: Uri,
    /**
     * Timestamp in milliseconds since 1970. Populated from
     * [android.provider.DocumentsContract.Document.COLUMN_LAST_MODIFIED] and
     * [android.provider.MediaStore.MediaColumns.DATE_MODIFIED]
     */
    val lastModified: Long
)
