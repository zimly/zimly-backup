package io.zeitmaschine.zimzync

import android.net.Uri
import android.util.Log
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

class SyncServiceImplTest {
    private lateinit var mediaRepository: MediaRepository
    private lateinit var minioRepository: MinioRepository
    private val containerName = "minio/minio:latest"
    private val minioPort = 9000

    @get:Rule
    val minioContainer: GenericContainer<*> = GenericContainer(DockerImageName.parse(containerName))
            .withEnv(mapOf("MINIO_ACCESS_KEY" to "test", "MINIO_SECRET_KEY" to "testtest"))
            .withCommand("server /data")
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

        every { mediaRepository.getMedia() } returns listOf(MediaObject("name", 1234, "12qsvsgv", "jpeg", 12354532, uri))

        val url = "http://" + minioContainer.host + ":" + minioContainer.getMappedPort(minioPort)
        val bucket = "test-bucket"
        minioRepository = MinioRepository(url, "test", "testtest", bucket)
        minioRepository.createBucket(bucket)
    }

    // https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-test/
    @Test
    fun diffA() = runTest {
        val ss = SyncServiceImpl(minioRepository, mediaRepository)

        when (val result = ss.diff()) {
            is Result.Success<Diff> -> {
                assertThat(result.data.remotes.size, `is`(0))
                assertThat(result.data.locals.size, `is`(1))
                assertThat(result.data.diff.size, `is`(1))
            }
            is Result.Error -> throw Exception("Diff failed", result.exception)
        }
    }

    @Test
    fun diff() {
        val ss = SyncServiceImpl(minioRepository, mediaRepository)

        val diff = ss.diffA()
        assertThat(diff.remotes.size, `is`(0))
        assertThat(diff.locals.size, `is`(1))
        assertThat(diff.diff.size, `is`(1))
    }
}