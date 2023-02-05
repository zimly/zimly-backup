package io.zeitmaschine.zimzync

import android.util.Log
import io.minio.*
import java.io.InputStream
import java.time.ZonedDateTime

data class S3Object(
    var name: String,
    var size: Long,
    var checksum: String,
    var contentType: String,
    var modified: ZonedDateTime,
)

class MinioRepository(url: String, key: String, secret: String, private val bucket: String) :
    S3Repository {

    private var mc: MinioClient

    init {
        try {
            mc = MinioClient.builder()
                .endpoint(url)
                .credentials(key, secret)
                .build()
            verify()
        } catch (e: Exception) {
            throw Exception("Failed to initialize minio client: ${e.message}", e)
        }
    }

    private fun verify() {
        val found = mc.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!found) {
            Log.i(SyncModel.TAG, "Bucket doesn't exists.");
        } else {
            Log.i(SyncModel.TAG, "Bucket already exists.");
        }
    }

    override fun listObjects(): List<S3Object> {
        return mc.listObjects(ListObjectsArgs.builder().bucket(bucket).build())
            .map { res -> mc.statObject(StatObjectArgs.builder().bucket(bucket).`object`(res.get().objectName()).build()) }
            .map { result ->
                val name = result.`object`()
                val size = result.size()
                val checksum = result.etag()
                // TODO: Needed? Would get rid of the statObj req
                val contentType = result.contentType()
                val modified = result.lastModified()
                S3Object(name, size, checksum, contentType, modified)
            }
    }

    override fun put(stream: InputStream, name: String, contentType: String, size: Long): Boolean {
        stream.use { stream ->
            val param = PutObjectArgs.builder()
                .bucket(bucket)
                .`object`(name)
                .contentType(contentType)
                .stream(stream, size, -1)
                .build()
            mc.putObject(param)
            return true
        }
    }
}

interface S3Repository {
    fun listObjects(): List<S3Object>
    fun put(stream: InputStream, name: String, contentType: String, size: Long): Boolean
}