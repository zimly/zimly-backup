package io.zeitmaschine.zimzync.data.s3

import java.io.FilterInputStream
import java.io.InputStream

/**
 * Wraps an Inputstream to track the progress. Inspired by https://github.com/minio/minio-java/blob/master/examples/ProgressStream.java
 */
class ProgressStream(private val delegate: InputStream, private val progress: ProgressTracker) :
    FilterInputStream(delegate) {
    companion object {
        fun wrap(delegate: InputStream, progress: ProgressTracker): InputStream =
            ProgressStream(delegate, progress)
    }

    override fun read(): Int {
        val readBytes = delegate.read()
        this.progress.stepBy(readBytes)
        return readBytes
    }

    override fun read(toStore: ByteArray?): Int {
        val readBytes: Int = this.delegate.read(toStore)
        this.progress.stepBy(readBytes)
        return readBytes
    }

    override fun read(toStore: ByteArray?, off: Int, len: Int): Int {
        val readBytes: Int = this.delegate.read(toStore, off, len)
        this.progress.stepBy(readBytes)
        return readBytes
    }

    override fun skip(n: Long): Long {
        val readBytes = this.delegate.skip(n)
        this.progress.stepTo(readBytes)
        return readBytes
    }

}