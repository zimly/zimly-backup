package app.zimly.backup.data.media

import android.content.ContentResolver
import android.net.Uri
import androidx.core.net.toUri
import java.io.InputStream
import java.io.OutputStream

/**
 * Common interface shared by all implementations or wrappers of [ContentResolver] that queries local
 * [ContentObject]s, like media or documents.
 */
interface LocalContentResolver {
    fun getInputStream(uri: Uri): InputStream
    fun getOutputStream(parentUri: Uri, objectName: String, mimeType: String): OutputStream
    fun listObjects(): List<ContentObject>

    companion object {

        /**
         * Provides the correct [LocalContentResolver] based on [SourceType].
         */
        fun get(contentResolver: ContentResolver, type: SourceType, scope: String) =
            when (type) {
                SourceType.MEDIA -> LocalMediaResolverImpl(contentResolver, scope)
                SourceType.FOLDER -> LocalDocumentsResolver(contentResolver, scope.toUri())
            }
    }
}

