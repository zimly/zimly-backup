package app.zimly.backup.data.media

import android.net.Uri

/**
 * Represents a local document or media object.
 */
data class ContentObject(
    var path: String,
    var size: Long,
    var contentType: String,
    val uri: Uri
)
