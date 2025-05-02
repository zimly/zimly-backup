package app.zimly.backup.data.s3

import app.zimly.backup.data.media.ContentObject
import app.zimly.backup.data.media.LocalContentResolver
import app.zimly.backup.sync.UploadSyncService
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import kotlin.time.TimeSource

class LinodeIntegrationTest {

    private lateinit var s3Repository: MinioRepository

    @Before
    fun setUp() {
        this.s3Repository = MinioRepository(
            bucket.s3Endpoint,
            accessKey.accessKey,
            accessKey.secretKey,
            bucket.label
        )
    }

    @Test
    fun putLinode() = runTest {
        val timeSource = TimeSource.Monotonic

        // GIVEN
        val image = "/testdata/test_image.png"
        val stream =
            javaClass.getResourceAsStream(image) ?: throw Error("Could not open test resource.")
        val size = stream.available().toLong()

        val before = timeSource.markNow()

        // WHEN
        val res =
            s3Repository.put(stream, "testObj", "image/png", size)
                .onEach { println(it) }
                .toList()

        val after = timeSource.markNow()
        val duration = (after - before).inWholeMilliseconds
        val bytesPerSec = size * 1000 / duration

        // THEN
        val name = s3Repository.get("testObj").`object`()
        val filtered = res.filter { it.bytesPerSec != null }
        val avgSpeed = filtered.sumOf { it.bytesPerSec!! } / filtered.size

        assertThat(res.last().percentage, `is`(1f))
        assertThat(name, `is`("testObj"))

        // Don't assert, makes it flaky
        println("Expected avgSpeed to be in ballpark of $bytesPerSec B/s, but was $avgSpeed B/s. Factor ${avgSpeed.toDouble() / bytesPerSec.toDouble()} off.")
    }

    @Test
    fun createDiff() {
        val localContentResolver = mockk<LocalContentResolver>()

        every { localContentResolver.listObjects() } returns listOf(
            ContentObject(
                "test_image.png",
                123L,
                "image/png",
                mockk()
            )
        )

        val ss = UploadSyncService(s3Repository, localContentResolver)

        val diff = ss.calculateDiff()

        assertThat(diff.diff.size, `is`(1))
    }

    @After
    fun tearDown() = runBlocking {
        s3Repository.removeAll()
    }

    companion object {
        private lateinit var bucket: LinodeApi.BucketResponse
        private lateinit var accessKey: LinodeApi.KeyResponse

        private val token = System.getenv("LINODE_API_TOKEN")
            ?: throw Exception("LINODE_API_TOKEN anv variable missing")

        private val api = LinodeApi(token)

        @JvmStatic
        @BeforeClass
        fun bootstrap() {

            val bucketName = "zimly-test"
            val key = "zimly-test"

            this.bucket = api.createBucket(bucketName)
            this.accessKey = api.createKey(key, bucket.label)

            requireNotNull(accessKey.accessKey) { "Test case needs valid AWS key for zimly-test bucket" }
            requireNotNull(accessKey.secretKey) { "Test case needs valid AWS secret for zimly-test bucket" }

        }

        @JvmStatic
        @AfterClass
        fun cleanup() {
            api.deleteKey(accessKey.id)
            api.deleteBucket(bucket.region, bucket.label)
            api.cancelSubscription()
        }
    }
}