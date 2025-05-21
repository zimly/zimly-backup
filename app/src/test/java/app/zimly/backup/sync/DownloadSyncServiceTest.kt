package app.zimly.backup.sync

import android.net.Uri
import android.util.Log
import app.zimly.backup.data.media.LocalContentResolver
import app.zimly.backup.data.s3.MinioRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.testcontainers.containers.MinIOContainer
import java.io.ByteArrayOutputStream
import kotlin.test.assertEquals

class DownloadSyncServiceTest {
    private val minioUser = "test"
    private val minioPwd = "testtest"

    private lateinit var localContentResolver: LocalContentResolver
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

        this.localContentResolver = mockk()

        val bucket = "test-bucket"
        minioRepository = MinioRepository(minioContainer.s3URL, minioUser, minioPwd, bucket)

        runBlocking { minioRepository.createBucket(bucket) }
    }

    @Test
    fun synchronize() = runTest {

        // GIVEN
        val image1 = "test_image.png"
        val image2 = "test_image2.png"

        val path1 = "/testdata/$image1"
        val path2 = "/testdata/$image2"
        val stream1 =
            javaClass.getResourceAsStream(path1) ?: throw Error("Could not open test resource.")
        val stream2 =
            javaClass.getResourceAsStream(path2) ?: throw Error("Could not open test resource.")
        val size1 = stream1.available().toLong()
        val size2 = stream2.available().toLong()
        val totalSize = size1 + size2

        minioRepository.put(stream1, image1, "image/png", stream1.available().toLong()).last()
        minioRepository.put(stream2, image2, "image/png", stream2.available().toLong()).last()

        every { localContentResolver.listObjects() } returns emptyList()
        val out1 = ByteArrayOutputStream()
        val out2 = ByteArrayOutputStream()
        every {
            localContentResolver.getOutputStream(
                any(),
                any(),
                any()
            )
        } returns out1 andThen out2

        val target = mockk<Uri>()
        val ss = DownloadSyncService(minioRepository, localContentResolver, target)

        // WHEN
        val res = ss.synchronize().last()

        // THEN
        MatcherAssert.assertThat(res.transferredFiles, CoreMatchers.`is`(2))
        MatcherAssert.assertThat(res.transferredBytes, CoreMatchers.`is`(totalSize))
        MatcherAssert.assertThat(res.percentage, CoreMatchers.`is`(1f))

        assertEquals(out1.size(), size1.toInt())
        assertEquals(out2.size(), size2.toInt())
    }
}
