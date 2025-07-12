package app.zimly.backup.data.media

import android.net.Uri

/**
 * Wraps tricky APIs related to [android.content.ContentProvider] and [android.provider.DocumentsContract]
 * that make testing a PITA.
 */
interface DocumentsRepository {

    fun createDocument(parentDocumentUri: Uri, mimeType: String, displayName: String): Uri?
}