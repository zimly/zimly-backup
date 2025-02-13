package app.zimly.backup.data.media

import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.provider.MediaStore
import androidx.test.platform.app.InstrumentationRegistry
import junit.framework.TestCase.assertSame
import junit.framework.TestCase.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test

class LocalMediaRepositoryTest {
    private var testUris: MutableSet<Uri> = mutableSetOf()

    // These names are not random:
    // ContentResolver only accepts certain collection names for media files.
    private val imagesCollection = "Pictures"
    private val videosCollection = "Movies"

    private lateinit var contentResolver: ContentResolver

    @Before
    fun setup() {
        contentResolver = InstrumentationRegistry.getInstrumentation().targetContext.contentResolver
    }

    @After
    fun clear() {
        // Delete the test media file after the test
        testUris.forEach {
            val deletedRows = contentResolver.delete(it, null, null)
            assertTrue("Failed to delete test media file", deletedRows > 0)
        }
    }

    private fun bootstrapMedia() {
        val image = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "test_image.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, imagesCollection)
        }
        val video = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "test_video.mp4")
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, videosCollection)
        }

        contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, image).let {
            requireNotNull(it)
            testUris.add(it)
        }

        contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, video).let {
            requireNotNull(it)
            testUris.add(it)
        }
    }

    @Test
    fun repository() {
        bootstrapMedia()

        val repository = LocalMediaRepository(contentResolver)

        val collections = repository.getBuckets()
        assertTrue(collections.containsKey(imagesCollection))
        assertTrue(collections.containsKey(videosCollection))

        val photos = repository.getPhotos(setOf(imagesCollection))
        val videos = repository.getVideos(setOf(videosCollection))

        assertSame(collections[imagesCollection], 1)
        assertSame(collections[videosCollection], 1)
        assertSame(photos.size, 1)
        assertSame(videos.size, 1)
    }

}