package io.zeitmaschine.zimzync.data.s3

import java.io.FilterInputStream
import java.io.InputStream

/**
 * Wraps an Inputstream to track the progress. Inspired by https://github.com/minio/minio-java/blob/master/examples/ProgressStream.java
 */
class ProgressStream(private val delegate: InputStream, private val progress: Progress): FilterInputStream(delegate) {
    companion object {
        fun wrap(delegate: InputStream, progress: Progress) : InputStream = ProgressStream(delegate, progress)
    }

    override fun read(): Int {
        val readBytes = delegate.read()
        progress(readBytes)
        return readBytes
    }

    override fun read(toStore: ByteArray?): Int {
        val readBytes: Int = this.delegate.read(toStore)
        progress(readBytes)
        return readBytes
    }

    override fun read(toStore: ByteArray?, off: Int, len: Int): Int {
        val readBytes: Int = this.delegate.read(toStore, off, len)
        progress(readBytes)
        return readBytes
    }

    override fun skip(n: Long): Long {
        this.progress.stepTo(n)
        return this.delegate.skip(n)
    }

    // returns -1 when stream is read
    private fun progress(readBytes: Int) {
        if (readBytes == -1) {
            this.progress.complete()
        } else {
            this.progress.stepBy(readBytes)
        }
    }
}

class Progress(private val size: Long) {

    private var readBytes: Long = 0
    private var started: Long = System.currentTimeMillis() // This is not accurate
    private var lastTick: Long = -1;
    private var completed: Long = -1;

    fun stepBy(read: Int) {
        this.readBytes += read
        lastTick = System.currentTimeMillis()
    }
    fun stepTo(skipped: Long) {
        this.readBytes = skipped
        lastTick = System.currentTimeMillis()
    }

    fun complete() {
        this.completed = System.currentTimeMillis()
    }

    fun percentage(): Float {
        return readBytes.toFloat() / size
    }

    /**
     * Returns kB/s
     */
    fun currentSpeed(): Float {
        if (completed > -1) {
            return 0F
        }

        var duration = lastTick - started
        duration = if (duration > 0) duration else 1 // quard against infinity
        println(duration)
        return readBytes.toFloat() / duration
    }

    /**
     * Returns kB/s
     */
    fun avgSpeed(): Float {
        var duration = lastTick - completed
        duration = if (duration > 0) duration else 1
        println(duration)
        return readBytes.toFloat() / duration
    }


}