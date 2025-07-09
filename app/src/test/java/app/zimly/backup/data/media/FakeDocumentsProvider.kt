package app.zimly.backup.data.media

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.DocumentsContract

class FakeDocumentsProvider : ContentProvider() {

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        // Columns your code might expect
        val cursor = MatrixCursor(
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                DocumentsContract.Document.COLUMN_SIZE
            )
        )

        // Add fake file entry
        cursor.addRow(arrayOf(
            "primary:Documents/test.txt",
            "test.txt",
            "text/plain",
            System.currentTimeMillis(),
            12345L
        ))

        return cursor
    }

    // These can be no-op for read-only tests
    override fun getType(uri: Uri): String? = "vnd.android.document"
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}