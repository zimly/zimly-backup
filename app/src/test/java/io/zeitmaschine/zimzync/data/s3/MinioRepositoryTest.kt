package io.zeitmaschine.zimzync.data.s3

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.testcontainers.containers.MinIOContainer


const val minioUser = "test"
const val minioPwd = "testtest"

class MinioRepositoryTest {

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

        val bucket = "test-bucket"
        minioRepository = MinioRepository(minioContainer.s3URL, minioUser, minioPwd, bucket)
        minioRepository.createBucket(bucket)
    }


    @Test
    fun put() = runTest {
        val image = "/testdata/test_image.png"
        val stream =
            javaClass.getResourceAsStream(image) ?: throw Error("Could not open test resource.")
        val size = stream.available().toLong()

        val lastProgress =
            minioRepository.put(stream, "testObj", "image/png", size)
                .onEach { println(it) }
                .last()

        assertThat(lastProgress.percentage, `is`(1f))

        val name = minioRepository.get("testObj").`object`()
        assertThat(name, `is`("testObj"))

    }
}
