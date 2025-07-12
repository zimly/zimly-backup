package app.zimly.backup.data.media

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract

/**
 * Wraps calls to the untestable [DocumentsContract].
 */
class DocumentsContractRepository(private val contentResolver: ContentResolver) : DocumentsRepository {

    override fun createDocument(parentDocumentUri: Uri, mimeType: String, displayName: String): Uri? {
        return DocumentsContract.createDocument(
            contentResolver,
            parentDocumentUri,
            mimeType,
            displayName
        )
    }
}