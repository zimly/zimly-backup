package app.zimly.backup.data.media

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalMediaRepoTest {

    @Test
    fun testQueryReturnsNullBucketName() {
        val cursor = mockk<Cursor>()
        val contentResolver = mockk<ContentResolver>()

        mockkStatic(MediaStore.Images.Media::class)
        mockkStatic(MediaStore.Video.Media::class)
        every { MediaStore.Images.Media.getContentUri(any()) } returns mockk<Uri>()
        every { MediaStore.Video.Media.getContentUri(any()) } returns mockk<Uri>()

        // Mock contentResolver.query() to return our fake cursor
        every { contentResolver.query(any(), any(), any(), any(), any()) } returns cursor

        // Mock cursor behavior
        every { cursor.moveToNext() } returns true andThen false // Simulate 1 row
        every { cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME) } returns 0
        every { cursor.getString(0) } returns null // <-- Provoking the null pointer
        every { cursor.close() } returns Unit

        val mediaRepository = LocalMediaRepository(contentResolver)

        // Call the function
        val buckets = mediaRepository.getBuckets()

        // Verify that we correctly handle the null value
        assertTrue(buckets.isEmpty()) // Expecting an empty list since the name is null
    }
}
