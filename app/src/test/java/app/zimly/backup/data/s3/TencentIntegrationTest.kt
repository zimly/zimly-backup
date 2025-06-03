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
import org.junit.Before
import org.junit.Test
import kotlin.time.TimeSource

class TencentIntegrationTest {

    private lateinit var s3Repository: MinioRepository

    @Before
    fun setUp() {

        val url = "https://cos.eu-frankfurt.myqcloud.com"
        val bucket = "zimly-test-1361781432"
        val region = "eu-frankfurt"

        val key = System.getenv("TENCENT_KEY")
        checkNotNull(key) { "Missing ENV key: TENCENT_KEY" }
        val secret = System.getenv("TENCENT_SECRET")
        checkNotNull(secret) { "Missing ENV key: TENCENT_SECRET" }

        this.s3Repository = MinioRepository(
            url,
            key,
            secret,
            bucket,
            region,
            true
        )
    }

    @Test
    fun put() = runTest {
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
                "Camera/test_image.png",
                "Camera/test_image.png",
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

}