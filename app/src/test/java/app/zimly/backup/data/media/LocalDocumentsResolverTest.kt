package app.zimly.backup.data.media

import android.content.Context
import android.provider.DocumentsContract
import androidx.test.core.app.ApplicationProvider
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowContentResolver
import kotlin.test.Test
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class LocalDocumentsResolverTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val authority = "com.android.externalstorage.documents"

    @Before
    fun setup() {
        val provider = FakeDocumentsProvider()
        ShadowContentResolver.registerProviderInternal(authority, provider)
    }


    @Test
    fun testSomethingWithContentResolver() {
        val rootUri = DocumentsContract.buildTreeDocumentUri(authority, "primary:Documents")

        val resolver = LocalDocumentsResolver(context, rootUri)

        val result = resolver.listObjects()
        assertTrue(result.isNotEmpty())

    }
}