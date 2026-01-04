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


/**
 * A [ContentProvider] implementation that allows testing Android APIs using roboelectric. This is
 * still not satisfactory, and some things should potentially be abstracted into [DocumentsRepository].
 * But there are still key parts that are very closely tied to the [ContentProvider]s functionality.
 */
class FakeDocumentsProvider(private val authority: String, private val documentStore: FakeDocumentStore) : ContentProvider() {

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
            addURI(authority, "tree/*/document/*", DOCUMENT)
            addURI(authority, "tree/*/document/*/children", CHILDREN)
        }

        return when (matcher.match(uri)) {
            CHILDREN -> {
                val parentId = DocumentsContract.getDocumentId(uri)
                    ?: return cursor

                documentStore.list()
                    .filter { it.parentId == parentId }
                    .forEach {
                        cursor.addRow(
                            arrayOf<Any?>(
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
                val doc = documentStore[docId] ?: return cursor

                cursor.addRow(
                    arrayOf<Any?>(
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
    override fun getType(uri: Uri): String = "vnd.android.document"
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