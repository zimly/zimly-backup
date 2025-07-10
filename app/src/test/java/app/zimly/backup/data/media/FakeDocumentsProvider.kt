package app.zimly.backup.data.media

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.DocumentsContract

private const val DOCUMENT = 0
private const val CHILDREN = 1


class FakeDocumentsProvider : ContentProvider() {
    private val documents = mutableMapOf<String, FakeDocument>(
        "primary:Documents/test.txt" to FakeDocument(
            "primary:Documents/test.txt",
            "test.txt",
            "text/plain",
            System.currentTimeMillis(),
            12345L,
            "primary:Documents"
        ),
        "primary:Documents/test1.txt" to FakeDocument(
            "primary:Documents/test1.txt",
            "test1.txt",
            "text/plain",
            System.currentTimeMillis(),
            12345L,
            "primary:Documents"
        ),
        "primary:Documents/Folder1" to FakeDocument(
            "primary:Documents/Folder1",
            "Folder1",
            DocumentsContract.Document.MIME_TYPE_DIR,
            System.currentTimeMillis(),
            12345L,
            "primary:Documents"
        ),
        "primary:Documents/Folder2" to FakeDocument(
            "primary:Documents/Folder2",
            "Folder2",
            DocumentsContract.Document.MIME_TYPE_DIR,
            System.currentTimeMillis(),
            12345L,
            "primary:Documents"
        ),
    )

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        val cursor = MatrixCursor(
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                DocumentsContract.Document.COLUMN_SIZE
            )
        )

        val matcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI("com.android.externalstorage.documents", "tree/*/document/*", DOCUMENT)
            addURI("com.android.externalstorage.documents", "tree/*/document/*/children", CHILDREN)
        }

        return when (matcher.match(uri)) {
            CHILDREN -> {
                val parentId = DocumentsContract.getDocumentId(uri)
                    ?: return cursor

                documents.values
                    .filter { it.parentId == parentId }
                    .forEach {
                        cursor.addRow(
                            arrayOf(
                                it.id,
                                it.name,
                                it.mimeType,
                                it.lastModified,
                                it.size
                            )
                        )
                    }

                return cursor
            }

            DOCUMENT -> {
                val docId = DocumentsContract.getDocumentId(uri)
                    ?: return cursor
                val doc = documents[docId] ?: return cursor

                cursor.addRow(
                    arrayOf(
                        doc.id,
                        doc.name,
                        doc.mimeType,
                        doc.lastModified,
                        doc.size
                    )
                )
                return cursor;
            }

            else -> cursor
        }
    }

    // These can be no-op for read-only tests
    override fun getType(uri: Uri): String? = "vnd.android.document"
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int =
        DOCUMENT

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int =
        DOCUMENT

    data class FakeDocument(
        val id: String, // e.g. "primary:Documents/test.txt"
        val name: String,
        val mimeType: String,
        val lastModified: Long = System.currentTimeMillis(),
        val size: Long = 0L,
        val parentId: String? = null
    )
}