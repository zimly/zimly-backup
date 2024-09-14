package io.zeitmaschine.zimzync

import android.net.Uri
import android.util.Log
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.zeitmaschine.zimzync.data.media.MediaObject
import io.zeitmaschine.zimzync.data.media.MediaRepository
import io.zeitmaschine.zimzync.data.s3.MinioRepository
import io.zeitmaschine.zimzync.sync.Diff
import io.zeitmaschine.zimzync.sync.SyncServiceImpl
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.hasItems
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.testcontainers.containers.MinIOContainer

class SyncServiceImplTest {
    private val minioUser = "test"
    private val minioPwd = "testtest"

    private lateinit var mediaRepository: MediaRepository
    private lateinit var minioRepository: MinioRepository

    private val containerName = "minio/minio:latest"

    @get:Rule
    val minioContainer: MinIOContainer = MinIOContainer(containerName)
        .withUserName(minioUser)
        .withPassword(minioPwd)

    @Before
    fun setUp() {

        mockkStatic(Log::class)
        every { Log.v(any(), any()) } returns 0
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0

        val uri = mockk<Uri>()
        this.mediaRepository = mockk()

        every { mediaRepository.getMedia(setOf("Camera")) } returns listOf(MediaObject("name", 1234, "jpeg", uri))

        val bucket = "test-bucket"
        minioRepository = MinioRepository(minioContainer.s3URL, minioUser, minioPwd, bucket)

        runBlocking { minioRepository.createBucket(bucket) }
    }

    @Test
    fun diff() {
        val ss = SyncServiceImpl(minioRepository, mediaRepository)

        val diff = ss.diff(setOf("Camera"))
        assertThat(diff.remotes.size, `is`(0))
        assertThat(diff.locals.size, `is`(1))
        assertThat(diff.diff.size, `is`(1))
    }

    @Test
    fun syncIT() {
        val image1 = "test_image.png"
        val image2 = "test_image2.png"
        runTest {

            // GIVEN
            val path1 = "/testdata/$image1"
            val path2 = "/testdata/$image2"
            val stream1 =
                javaClass.getResourceAsStream(path1) ?: throw Error("Could not open test resource.")
            val stream2 =
                javaClass.getResourceAsStream(path2) ?: throw Error("Could not open test resource.")
            val size1 = stream1.available().toLong()
            val size2 = stream2.available().toLong()
            val totalSize = size1 + size2

            every { mediaRepository.getStream(any()) } returns stream1 andThen stream2

            val ss = SyncServiceImpl(minioRepository, mediaRepository)

            val localMediaUri = mockk<Uri>()
            val obj1 = MediaObject(name = image1, size1, "image/png", localMediaUri)
            val obj2 = MediaObject(name = image2, size2, "image/png", localMediaUri)
            val diff = Diff(remotes = emptyList(), locals = listOf(obj1, obj2), diff = listOf(obj1, obj2), size = totalSize)

            // WHEN
            val res = ss.sync(diff).last()

            // THEN
            assertThat(res.uploadedFiles, `is`(2))
            assertThat(res.readBytes, `is`(totalSize))
            assertThat(res.readBytes, `is`(diff.size))
            assertThat(res.percentage, `is`(1f))

            //Thread.sleep(3000)
            val objs = minioRepository.listObjects()
            assertThat(objs.size, `is`(2))
            assertThat(objs.map { it.name }, hasItems(image1, image2))

            val name = minioRepository.get(image1).`object`()
            assertThat(name, `is`(image1))

        }
    }

}