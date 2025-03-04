package app.zimly.backup.data.media

import android.net.Uri

data class ContentObject(
    var name: String,
    var size: Long,
    var contentType: String,
    val path: Uri
)
