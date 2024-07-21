package io.zeitmaschine.zimzync.data.s3

import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import kotlin.time.TimeSource

@Ignore("Needs IT profile and GH secret")
class AwsRepositoryTest {

    private lateinit var s3Repository: MinioRepository

    @Before
    fun setUp() {

        val url = "https://s3.eu-central-2.amazonaws.com"
        val bucket = "zimly-test"
        val key = System.getenv("AWS_TEST_KEY")
        val secret = System.getenv("AWS_TEST_SECRET")

        requireNotNull(key) { "Test case needs valid AWS key for zimly-test bucket" }
        requireNotNull(secret) { "Test case needs valid AWS secret for zimly-test bucket" }

        this.s3Repository = MinioRepository(url, key, secret, bucket)
    }

    @Test
    fun putAWS() = runTest {
        val timeSource = TimeSource.Monotonic

        val image = "/testdata/test_image.png"
        val stream =
            javaClass.getResourceAsStream(image) ?: throw Error("Could not open test resource.")
        val size = stream.available().toLong()

        val before = timeSource.markNow()

        val res =
            s3Repository.put(stream, "testObj", "image/png", size)
                .onEach { println(it) }
                .toList()

        val after = timeSource.markNow()

        val duration = (after - before).inWholeMilliseconds

        val bytesPerSec = size * 10000 / duration

        println("SPEED: $bytesPerSec")

        assertThat(res.last().percentage, `is`(1f))
        val name = s3Repository.get("testObj").`object`()
        assertThat(name, `is`("testObj"))

        val avgSpeed = res.sumOf { it.bytesPerSec } / res.size

        assertTrue("Expected avgSpeed to be in ballpark of $bytesPerSec, but was $avgSpeed", avgSpeed.toDouble() in bytesPerSec*0.8..bytesPerSec*1.2)

    }

    @After
    fun tearDown() = runBlocking {
        s3Repository.remove("testObj")
    }
}