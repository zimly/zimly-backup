package app.zimly.backup.data.media

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import java.io.InputStream

/**
 * https://developer.android.com/training/data-storage/shared/documents-files#grant-access-directory
 * https://developer.android.com/training/data-storage/shared/documents-files#persist-permissions
 */
class LocalDocumentsResolver(private val contentResolver: ContentResolver, folder: String): LocalContentResolver {

    private val parent = Uri.parse(folder)

    companion object {
        private val TAG: String? = LocalDocumentsResolver::class.simpleName
    }

    override fun listObjects(): List<ContentObject> {

        val documentId = DocumentsContract.getTreeDocumentId(parent)

        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(parent, documentId)

        val files = mutableListOf<ContentObject>()

        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE
        )

        contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeTypeIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
            val sizeIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)

            while (cursor.moveToNext()) {
                val documentId = cursor.getString(idIndex)
                val displayName = cursor.getString(nameIndex)
                val mimeType = cursor.getString(mimeTypeIndex)
                val size = cursor.getLong(sizeIndex)

                val fileUri = DocumentsContract.buildDocumentUriUsingTree(parent, documentId)
                files.add(ContentObject(displayName, size, mimeType, fileUri))

                // Log or handle the file
                Log.i(TAG, "File: $displayName, MIME Type: $mimeType, Uri: $fileUri")
            }
        }

        return files
    }

    override fun getStream(uri: Uri): InputStream {
        return contentResolver.openInputStream(uri) ?: throw Exception("Could not open stream for $uri.")
    }
}
