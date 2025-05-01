package app.zimly.backup.data.s3

import junit.framework.TestCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import okio.buffer
import okio.sink
import okio.source
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import java.util.concurrent.TimeUnit
import kotlin.random.Random


class ProgressTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun download() = runTest {

        val server = MockWebServer()

        val size = 1024 * 5
        val rndBytes = Random.Default.nextBytes(size)

        val enqueuedResponse = MockResponse()
            .setResponseCode(200)
            .setBody(Buffer().write(rndBytes))
            .addHeader("Content-Type", "application/octet-stream")

        server.enqueue(enqueuedResponse)

        // Start the server.
        server.start()

        val baseUrl = server.url("/")

        val totalSize = size.toLong()
        val progress = ProgressTracker(totalSize)

        // TODO this is just the okio http client
        val client = MinioRepository.client()

        val request: Request = Request.Builder()
            .url(baseUrl)
            .build()

        val res = mutableListOf<Progress>()

        // https://developer.android.com/kotlin/flow/test
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {

            progress.observe()
                .onEach { println("$it") }
                .toList(res)

        }

        val response = client.newCall(request).execute()

        // Ensure we read the body!
        response.body?.source()?.use { source ->
            val countedSource = source.counted(progress).buffer()
            while (!countedSource.exhausted()) {
                countedSource.readByteArray()
            }
        }
        // First entry is null
        TestCase.assertTrue(res.first().bytesPerSec == null)

        // Totals add up
        assertThat(res.last().totalReadBytes, `is`(totalSize))
        assertThat(res.last().percentage, `is`(1F))
        assertThat(response.code, `is`(200))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun slowDownstream() = runTest {

        val server = MockWebServer()

        val response = MockResponse().throttleBody(500L, 1000L, TimeUnit.MILLISECONDS)
            .setResponseCode(200)
        server.enqueue(response)
        server.enqueue(response)

        // Start the server.
        server.start()

        val baseUrl = server.url("/")

        val size = 5000
        val rndBytes = Random.Default.nextBytes(size)
        val totalSize = size.toLong() * 2
        val progress = ProgressTracker(totalSize)

        val client = MinioRepository.client(uploadProgressTracker = progress)

        val reqBody = rndBytes.toRequestBody()
        val request: Request = Request.Builder()
            .url(baseUrl)
            .put(reqBody)
            .build()

        val res = mutableListOf<Progress>()

        // https://developer.android.com/kotlin/flow/test
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {

            progress.observe()
                .onEach { println("$it") }
                .toList(res)

        }

        val resp1 = client.newCall(request).execute()
        val resp2 = client.newCall(request).execute()

        // First entry is null
        TestCase.assertTrue(res.first().bytesPerSec == null)

        // Rest should be around ~500 B/s
        res.subList(1, res.size).forEach {
            TestCase.assertTrue(it.bytesPerSec!! in 450..560)
        }

        // Totals add up
        assertThat(res.last().totalReadBytes, `is`(totalSize))
        assertThat(res.last().percentage, `is`(1F))
        assertThat(resp1.code, `is`(200))
        assertThat(resp2.code, `is`(200))
    }
}