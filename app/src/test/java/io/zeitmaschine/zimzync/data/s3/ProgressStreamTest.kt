package io.zeitmaschine.zimzync.data.s3

import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets


class ProgressStreamTest {

    @Test
    fun name() {
        val initialString = "text"
        val input: InputStream = ByteArrayInputStream(initialString.toByteArray())

        val size: Long = initialString.toByteArray().size.toLong()
        println(size)

        val progress = Progress(size)
        val wrapped = ProgressStream.wrap(input, progress)

        String(wrapped.readAllBytes(), StandardCharsets.UTF_8)

        assertThat(progress.percentage(), `is`(1F))
        assertThat(progress.avgSpeed(), `is`(4F))
        assertThat(progress.currentSpeed(), `is`(0F))

    }
}