package app.zimly.backup.sync

import android.net.Uri
import android.util.Log
import app.zimly.backup.data.media.ContentObject
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
import java.time.Instant

class UploadSyncServiceTest {
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
    fun localDiff() {
        val uri = mockk<Uri>()
        every { localContentResolver.listObjects() } returns listOf(
            ContentObject(
                "path/name",
                "path/name",
                1234,
                "jpeg",
                uri,
                Instant.now().toEpochMilli(),
            )
        )

        val ss = UploadSyncService(minioRepository, localContentResolver)

        val diff = ss.calculateDiff()
        MatcherAssert.assertThat(diff.remotes.size, CoreMatchers.`is`(0))
        MatcherAssert.assertThat(diff.locals.size, CoreMatchers.`is`(1))
        MatcherAssert.assertThat(diff.diff.size, CoreMatchers.`is`(1))
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

        val localMediaUri = mockk<Uri>()
        val obj1 = ContentObject(path = image1, image1, size1, "image/png", localMediaUri, Instant.now().toEpochMilli(),)
        val obj2 = ContentObject(path = image2, image1, size2, "image/png", localMediaUri, Instant.now().toEpochMilli(),)
        every { localContentResolver.listObjects() } returns listOf(obj1, obj2)
        every { localContentResolver.getInputStream(any()) } returns stream1 andThen stream2

        val ss = UploadSyncService(minioRepository, localContentResolver)

        // WHEN
        val res = ss.synchronize().last()

        // THEN
        MatcherAssert.assertThat(res.transferredFiles, CoreMatchers.`is`(2))
        MatcherAssert.assertThat(res.transferredBytes, CoreMatchers.`is`(totalSize))
        MatcherAssert.assertThat(res.percentage, CoreMatchers.`is`(1f))

        //Thread.sleep(3000)
        val objs = minioRepository.listObjects()
        MatcherAssert.assertThat(objs.size, CoreMatchers.`is`(2))
        MatcherAssert.assertThat(objs.map { it.name }, CoreMatchers.hasItems(image1, image2))

        val name = minioRepository.get(image1).`object`()
        MatcherAssert.assertThat(name, CoreMatchers.`is`(image1))

    }
}
