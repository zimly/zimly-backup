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
class LocalDocumentsResolver(context: Context, private val root: Uri) :
    LocalContentResolver, WriteableContentResolver {

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

    /**
     * Takes a complete [objectPath], e.g. Pictures/2025/picture.png, and creates the directory path
     * if it's missing and an empty Document with the trailing objectName and returns the [OutputStream].
     */
    override fun createOutputStream(objectPath: String, mimeType: String): OutputStream {

        val (directories, objectName) = extractPath(objectPath)
        val rootDocUri = DocumentsContract.buildDocumentUriUsingTree(
            root,
            DocumentsContract.getTreeDocumentId(root)
        )

        val parentUri = if (directories.isNotEmpty()) {
            createOrFindDirectory(rootDocUri, directories)
        } else {
            rootDocUri
        }

        return getOutputStream(
            parentUri,
            objectName,
            mimeType
        )
    }

    private fun getOutputStream(
        parentDirectory: Uri,
        fileName: String,
        mimeType: String
    ): OutputStream {
        val fileUri = DocumentsContract.createDocument(
            contentResolver,
            parentDirectory,
            mimeType,
            fileName
        ) ?: throw IOException("Failed to create file in $parentDirectory")

        return contentResolver.openOutputStream(fileUri)
            ?: throw IOException("Failed to open stream for $fileUri")
    }

    /**
     * Resolves or creates the [pathSegments] recursively from the [rootDocument].
     */
    private fun createOrFindDirectory(rootDocument: Uri, pathSegments: List<String>): Uri {

        var currentUri = rootDocument
        for (directory in pathSegments) {
            var uri = findDirectory(currentUri, directory)
            if (uri == null) {
                uri = DocumentsContract.createDocument(
                    contentResolver,
                    currentUri,
                    DocumentsContract.Document.MIME_TYPE_DIR,
                    directory
                ) ?: throw Exception("Failed to create directory '$directory' under '$currentUri'.")
            }
            currentUri = uri
        }
        return currentUri
    }

    private fun findDirectory(parentDocumentUri: Uri, directoryName: String): Uri? {

        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            root,
            DocumentsContract.getDocumentId(parentDocumentUri)
        )

        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        )

        var folderUri: Uri? = null

        contentResolver.query(childrenUri, projection, null, null, null)?.use query@{ cursor ->

            val idIndex =
                cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameIndex =
                cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeTypeIndex =
                cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)

            while (cursor.moveToNext()) {
                val docId = cursor.getString(idIndex)
                val name = cursor.getString(nameIndex)
                val mimeType = cursor.getString(mimeTypeIndex)
                if (name == directoryName && mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                    folderUri =
                        DocumentsContract.buildDocumentUriUsingTree(parentDocumentUri, docId)
                    return@query // exits the lambda immediately
                }
            }
        }
        return folderUri
    }

    /**
     * Splits the passed [objectPath] into parent directories and the objectName.
     * TODO: Not sure, whether this should be sanitizing the path as the diff algorithm relies on the raw [objectPath]
     */
    fun extractPath(objectPath: String): Pair<List<String>, String> {
        val segments = objectPath
            .trim('/')
            .split('/')
            .filter { it.isNotBlank() && it != "." && it != ".." }

        return Pair(segments.dropLast(1), segments.last())
    }

}
