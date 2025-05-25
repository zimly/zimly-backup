package app.zimly.backup.data.media

import android.content.ContentResolver
import android.content.Context
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
    fun createDirectoryStructure(uri: Uri, path: String): Uri

    companion object {

        /**
         * Provides the correct [LocalContentResolver] based on [ContentType].
         */
        fun get(context: Context, type: ContentType, scope: String) =
            when (type) {
                ContentType.MEDIA -> LocalMediaResolverImpl(context, scope)
                ContentType.FOLDER -> LocalDocumentsResolver(context, scope.toUri())
            }
    }
}

