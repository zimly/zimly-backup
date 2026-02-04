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
import org.junit.ClassRule
import org.junit.Test
import org.junit.rules.ExternalResource
import java.time.Instant
import kotlin.time.TimeSource

class LinodeIntegrationTest {

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
            repository.put(stream, "testObj", "image/png", size)
                .onEach { println(it) }
                .toList()

        val after = timeSource.markNow()
        val duration = (after - before).inWholeMilliseconds
        val bytesPerSec = size * 1000 / duration

        // THEN
        val name = repository.get("testObj").`object`()
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
                "Camera/test_image.png",
                "Camera/test_image.png",
                123L,
                "image/png",
                mockk(),
                Instant.now().toEpochMilli(),
            )
        )

        val ss = UploadSyncService(repository, localContentResolver)

        val diff = ss.calculateDiff()

        assertThat(diff.diff.size, `is`(1))
    }

    @After
    fun tearDown() = runBlocking {
        repository.removeAll()
    }

    companion object {
        private lateinit var api: LinodeApi
        private lateinit var bucket: LinodeApi.BucketResponse
        private lateinit var accessKey: LinodeApi.KeyResponse
        private lateinit var repository: MinioRepository

        @JvmField
        @ClassRule
        val linodeResource = object : ExternalResource() {

            override fun before() {
                val token = System.getenv("LINODE_API_TOKEN")
                    ?: error("Missing LINODE_API_TOKEN")

                api = LinodeApi(token)
                bucket = api.createBucket("zimly-test")
                accessKey = api.createKey("zimly-test", bucket.label)

                repository = MinioRepository(
                    bucket.s3Endpoint,
                    accessKey.accessKey,
                    accessKey.secretKey,
                    bucket.label,
                )
            }

            override fun after() {
                api.deleteKey(accessKey.id)
                api.deleteBucket(bucket.region, bucket.label)
                api.cancelSubscription()
            }
        }
    }
}