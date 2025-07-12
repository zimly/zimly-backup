package app.zimly.backup.data.media

import android.content.Context
import android.provider.DocumentsContract
import androidx.test.core.app.ApplicationProvider
import app.zimly.backup.data.media.FakeDocumentsProvider.FakeDocument
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * These tests rely on roboelectric to stub the [android.content.ContentProvider]. Additionally, tests
 * that need to create Documents need [FakeDocumentsRepository] to circumvent issues with security
 * restrictions in newer Android APIs and other problems.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LocalDocumentsResolverTest {

    private var documentStore: MutableMap<String, FakeDocument> = mutableMapOf(
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
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val authority = "com.android.externalstorage.documents"

    @Before
    fun setup() {
        val provider = FakeDocumentsProvider(authority, documentStore)
        ShadowContentResolver.registerProviderInternal(authority, provider)
    }

    @Test
    fun listObjectsRecursive() {
        val rootUri = DocumentsContract.buildTreeDocumentUri(authority, "primary:Documents")

        // Does not need FakeDocumentsRepository.
        val resolver = LocalDocumentsResolver(context, rootUri)

        val result = resolver.listObjects()
        assertTrue(result.size == 2)
    }

    @Test
    fun existingDocument() {
        val rootUri = DocumentsContract.buildTreeDocumentUri(authority, "primary:Documents")

        // Does not need FakeDocumentsRepository.
        val resolver = LocalDocumentsResolver(context, rootUri)

        val result = resolver.createOrFindDocument("test1.txt", "text/plain")

        assertTrue(result.authority == authority)
        assertTrue(result.path?.contains("test1.txt") == true)
    }

    @Test
    fun nonExistingDocument() {
        val rootUri = DocumentsContract.buildTreeDocumentUri(authority, "primary:Documents")

        // Needs FakeDocumentsRepository.
        val resolver = LocalDocumentsResolver(context, rootUri, FakeDocumentsRepository(authority, documentStore))

        val result = resolver.createOrFindDocument("Folder1/Folder2/test1.txt", "text/plain")

        assertTrue(result.authority == authority)
        assertTrue(result.path?.contains("Folder1/Folder2/test1.txt") == true)
    }
}