package app.zimly.backup.data.media

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * https://developer.android.com/training/data-storage/shared/documents-files#grant-access-directory
 * https://developer.android.com/training/data-storage/shared/documents-files#persist-permissions
 */
class LocalDocumentsResolver(private val context: Context, private val root: Uri) :
    LocalContentResolver {

    val contentResolver: ContentResolver = context.contentResolver
    val rootDocumentId: String = DocumentsContract.getTreeDocumentId(root)

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

        listObjectsRecursive(rootDocumentId, files)
        return files
    }

    private fun listObjectsRecursive(folderDocumentId: String, files: MutableList<ContentObject>) {
        val childrenUri =
            DocumentsContract.buildChildDocumentsUriUsingTree(root, folderDocumentId)

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
                    val fileUri = DocumentsContract.buildDocumentUriUsingTree(root, documentId)
                    val path = removeStorageIdentifier(documentId)
                    val relPath = documentId.substringAfter("$rootDocumentId/")

                    files.add(ContentObject(path, relPath, size, mimeType, fileUri))
                    // Log or handle the file
                    Log.i(TAG, "File: $displayName, MIME Type: $mimeType, Uri: $fileUri")
                }
            }
        }
    }

    override fun getInputStream(uri: Uri): InputStream {
        return contentResolver.openInputStream(uri)
            ?: throw Exception("Could not open stream for $uri.")
    }

    override fun getOutputStream(parentFolderUri: Uri, fileName: String, mimeType: String): OutputStream {
        val fileUri = DocumentsContract.createDocument(
            contentResolver,
            parentFolderUri,
            mimeType,
            fileName
        ) ?: throw IOException("Failed to create file in $parentFolderUri")

        return contentResolver.openOutputStream(fileUri)
            ?: throw IOException("Failed to open stream for $fileUri")
    }

    override fun createDirectoryStructure(parentTreeUri: Uri, path: String): Uri {
        val baseDocumentUri = DocumentsContract.buildDocumentUriUsingTree(
            parentTreeUri,
            DocumentsContract.getTreeDocumentId(parentTreeUri)
        )

        val segments = path.trim('/').split('/')
        var folders = segments.dropLast(1) // drop filename
        var currentUri = baseDocumentUri
        for (folder in folders) {
            currentUri = findOrCreateFolder(contentResolver, currentUri, folder)
        }
        return currentUri
    }

    fun findOrCreateFolder(
        contentResolver: ContentResolver,
        parentDocumentUri: Uri,
        folderName: String
    ): Uri {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            parentDocumentUri,
            DocumentsContract.getDocumentId(parentDocumentUri)
        )

        contentResolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            ),
            null,
            null,
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val docId = cursor.getString(0)
                val name = cursor.getString(1)
                val mimeType = cursor.getString(2)
                if (name == folderName && mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                    return DocumentsContract.buildDocumentUriUsingTree(parentDocumentUri, docId)
                }
            }
        }

        return DocumentsContract.createDocument(
            contentResolver,
            parentDocumentUri,
            DocumentsContract.Document.MIME_TYPE_DIR,
            folderName
        )!!
    }

}
