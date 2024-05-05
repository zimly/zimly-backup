package io.zeitmaschine.zimzync

import android.net.Uri
import android.util.Log
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.zeitmaschine.zimzync.data.media.MediaObject
import io.zeitmaschine.zimzync.data.media.MediaRepository
import io.zeitmaschine.zimzync.data.s3.MinioRepository
import io.zeitmaschine.zimzync.data.s3.Progress
import io.zeitmaschine.zimzync.data.s3.S3Repository
import io.zeitmaschine.zimzync.data.s3.minioPwd
import io.zeitmaschine.zimzync.data.s3.minioUser
import io.zeitmaschine.zimzync.sync.Diff
import io.zeitmaschine.zimzync.sync.SyncServiceImpl
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.MinIOContainer
import org.testcontainers.shaded.com.google.common.net.MediaType
import org.testcontainers.utility.DockerImageName
import java.io.ByteArrayInputStream
import java.io.InputStream

class SyncServiceImplTest {
    private lateinit var mediaRepository: MediaRepository
    private lateinit var minioRepository: MinioRepository

    private val containerName = "minio/minio:latest"
    private val minioPort = 9000

    @get:Rule
    val minioContainer: MinIOContainer = MinIOContainer(containerName)
        .withUserName(minioUser)
        .withPassword(minioPwd)
        .withExposedPorts(minioPort)

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
        minioRepository.createBucket(bucket)
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
    fun sync() {
        val flowOf = flowOf(Progress(25L, 25L, 25F, 100L), Progress(25L, 50L, 50F, 100L), Progress(25L, 75L, 75F, 100L), Progress(25L, 100L, 100F, 100L))
        runTest {
            val s3Repo: S3Repository = mockk()
            every { s3Repo.put(any(), any(), any(), any()) } returns flowOf
            every { mediaRepository.getStream(any()) } returns ByteArrayInputStream("textavdfbad".toByteArray())

            val ss = SyncServiceImpl(s3Repo, mediaRepository)

            val localMediaUri = mockk<Uri>()
            val localMediaObj = MediaObject(name = "", 100L, "avr", localMediaUri)
            val res = ss.sync(Diff(remotes = emptyList(), locals = listOf(localMediaObj), diff = listOf(localMediaObj), size = 0L)).toList()

            assertThat(res.size, `is`(5)) // Includes initial EMPTY
            assertThat(res[4].readBytes, `is`(100L))
            assertThat(res[4].uploadedFiles, `is`(1))
        }
    }

    @Test
    fun syncIT() {
        runTest {
            // TODO real stream
            val image = "/testdata/test_image.png"
            val stream =
                javaClass.getResourceAsStream(image) ?: throw Error("Could not open test resource.")
            val size = stream.available().toLong()

            every { mediaRepository.getStream(any()) } returns stream

            val ss = SyncServiceImpl(minioRepository, mediaRepository)

            val localMediaUri = mockk<Uri>()
            val obj1 = MediaObject(name = "test1", size, "image/png", localMediaUri)
            val diff = Diff(remotes = emptyList(), locals = listOf(obj1), diff = listOf(obj1), size = size*2)
            val res = ss.sync(diff).toList()

            val name = minioRepository.get("test1").`object`()
            assertThat(name, `is`("test1"))

            assertThat(res.size, `is`(1)) // Includes initial EMPTY
            assertThat(res[4].readBytes, `is`(100L))
            assertThat(res[4].uploadedFiles, `is`(1))
        }
    }

}