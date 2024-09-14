package io.zeitmaschine.zimzync.data.s3

import io.mockk.every
import io.mockk.mockk
import io.zeitmaschine.zimzync.data.media.MediaObject
import io.zeitmaschine.zimzync.data.media.MediaRepository
import io.zeitmaschine.zimzync.sync.SyncServiceImpl
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

class AwsRepositoryTest {

    private lateinit var s3Repository: MinioRepository

    @Before
    fun setUp() {

        val url = "https://s3.eu-central-2.amazonaws.com"
        val bucket = "zimly-test"
        val key = System.getenv("S3_AWS_TEST_KEY")
        val secret = System.getenv("S3_AWS_TEST_SECRET")

        requireNotNull(key) { "Test case needs valid AWS key for zimly-test bucket" }
        requireNotNull(secret) { "Test case needs valid AWS secret for zimly-test bucket" }

        this.s3Repository = MinioRepository(url, key, secret, bucket)
    }

    @Test
    fun putAWS() = runTest {
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
        val mediaRepository = mockk<MediaRepository>()

        every { mediaRepository.getMedia(any()) } returns listOf(MediaObject("test_image.png", 123L, "image/png", mockk()))

        val ss = SyncServiceImpl(s3Repository, mediaRepository)

        val diff = ss.diff(setOf("zimly-test.bkp"))

        assertThat(diff.diff.size, `is`(1))
    }

    @After
    fun tearDown() = runBlocking {
        s3Repository.removeAll()
    }
}