package app.zimly.backup.data.media

import android.net.Uri
import android.provider.DocumentsContract
import app.zimly.backup.data.media.FakeDocumentsProvider.FakeDocument

class FakeDocumentsRepository(
    private val authority: String,
    private val documentStore: FakeDocumentStore
) : DocumentsRepository {

    /**
     * Stub implementation to make [DocumentsContract.createDocument] work.
     */
    override fun createDocument(
        parentDocumentUri: Uri,
        mimeType: String,
        displayName: String
    ): Uri? {
        val parentDocId = DocumentsContract.getDocumentId(parentDocumentUri)

        val newDocId = "$parentDocId/$displayName"

        val newDocUri =
            DocumentsContract.buildDocumentUri(authority, newDocId)

        documentStore.add(
            FakeDocument(
                id = newDocId,
                parentId = parentDocId,
                name = displayName,
                mimeType = mimeType,
            )
        )

        return newDocUri
    }
}