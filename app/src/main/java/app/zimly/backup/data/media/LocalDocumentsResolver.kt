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
class LocalDocumentsResolver(private val contentResolver: ContentResolver, private val parent: Uri) :
    LocalContentResolver {

    companion object {
        private val TAG: String? = LocalDocumentsResolver::class.simpleName

        /**
         * Strips off the storage type path from the documentId, e.g. "primary:some/path" -> "some/path".
         */
        private fun removeStorageIdentifier(documentId: String): String {
            return documentId.substringAfter(":")
        }

        /**
         * Shady function to create a nice path for documents.
         *
         * See e.g. https://stackoverflow.com/questions/13209494/how-to-get-the-full-file-path-from-uri
         */
        fun objectPath(uri: Uri): String {
            if (uri == Uri.EMPTY || uri.path == null) return ""
            try {
                val docId =
                    if (DocumentsContract.isTreeUri(uri)) DocumentsContract.getTreeDocumentId(uri) else DocumentsContract.getDocumentId(
                        uri
                    )
                return removeStorageIdentifier(docId)
            } catch (e: IllegalArgumentException) {
                // This should never happen, should throw here.
                Log.e(TAG, "Failed to create object Path from uri.", e)
            }
            return uri.path!!
        }
    }

    override fun listObjects(): List<ContentObject> {
        val files = mutableListOf<ContentObject>()
        val treeDocumentId = DocumentsContract.getTreeDocumentId(parent)

        listObjectsRecursive(treeDocumentId, files)
        return files
    }

    private fun listObjectsRecursive(folderDocumentId: String, files: MutableList<ContentObject>) {
        val childrenUri =
            DocumentsContract.buildChildDocumentsUriUsingTree(parent, folderDocumentId)

        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE
        )

        contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
            val idIndex =
                cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameIndex =
                cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeTypeIndex =
                cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
            val sizeIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)

            while (cursor.moveToNext()) {
                val documentId = cursor.getString(idIndex)
                val displayName = cursor.getString(nameIndex)
                val mimeType = cursor.getString(mimeTypeIndex)
                val size = cursor.getLong(sizeIndex)

                // Resolve directories recursively
                if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                    listObjectsRecursive(documentId, files)
                } else {
                    val fileUri = DocumentsContract.buildDocumentUriUsingTree(parent, documentId)
                    val path = removeStorageIdentifier(documentId)
                    files.add(ContentObject(path, size, mimeType, fileUri))
                    // Log or handle the file
                    Log.i(TAG, "File: $displayName, MIME Type: $mimeType, Uri: $fileUri")
                }
            }
        }
    }

    override fun getStream(uri: Uri): InputStream {
        return contentResolver.openInputStream(uri)
            ?: throw Exception("Could not open stream for $uri.")
    }
}
