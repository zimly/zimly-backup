package io.zeitmaschine.zimzync.data.media

import android.content.ContentResolver
import android.content.ContentUris
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import java.io.InputStream

data class MediaObject(
    var name: String,
    var size: Long,
    var contentType: String,
    val path: Uri,
)

class ResolverBasedRepository(private val contentResolver: ContentResolver): MediaRepository {

    companion object {
        private val TAG: String? = ResolverBasedRepository::class.simpleName
    }

    override fun getPhotos(buckets: Set<String>): List<MediaObject> {
        // https://developer.android.com/training/data-storage/shared/media#media_store
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.BUCKET_ID,
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
        )

        // Display videos in alphabetical order based on their display name.
        val sortOrder = "${MediaStore.Images.ImageColumns.DATE_TAKEN} DESC"

        // https://stackoverflow.com/questions/30654774/android-is-external-content-uri-enough-for-a-photo-gallery
        val contentUri = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        Log.i(TAG, contentUri.path ?: "whoops")

        val photos = mutableListOf<MediaObject>()
        val contentBuckets = buckets.joinToString(separator = ",", transform = {bucket -> "'${bucket}'"})
        val selection = MediaStore.MediaColumns.BUCKET_DISPLAY_NAME + " IN ($contentBuckets)"
        contentResolver.query(
            contentUri,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val bucketIdColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_ID)
            val bucketNameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)

            Log.i(TAG, "Number of images: ${cursor.count}")
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val mimeType = cursor.getString(mimeTypeColumn)
                val size = cursor.getLong(sizeColumn)
                val bucketId: Long = cursor.getLong(bucketIdColumn)
                val bucketName = cursor.getString(bucketNameColumn)
                var contentUri: Uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

                // https://developer.android.com/training/data-storage/shared/media#location-media-captured
                contentUri= MediaStore.setRequireOriginal(contentUri)

                Log.d(TAG, "Name: $name bucketName: $bucketName bucketId: $bucketId")

                val objectName = if (bucketName.isNullOrEmpty()) name else "$bucketName/$name"
                photos.add(MediaObject(objectName, size, mimeType, contentUri))
            }
        }
        return photos.toList()
    }

    override fun getVideos(buckets: Set<String>): List<MediaObject> {

        val videos = mutableListOf<MediaObject>()

        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.BUCKET_ID,
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
        )

        // Display videos in order of creation
        val sortOrder = "${MediaStore.Video.VideoColumns.DATE_TAKEN} DESC"

        // https://developer.android.com/training/data-storage/shared/media#media_store
        val contentUri = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)

        val contentBuckets = buckets.joinToString(separator = ",", transform = {bucket -> "'${bucket}'"})
        contentResolver.query(
            contentUri,
            projection,
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME + " IN ($contentBuckets)",
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val bucketIdColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_ID)
            val bucketNameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)

            Log.i(TAG, "Number of videos: ${cursor.count}")
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val mimeType = cursor.getString(mimeTypeColumn)
                val size = cursor.getLong(sizeColumn)
                val bucketId: Long = cursor.getLong(bucketIdColumn)
                val bucketName = cursor.getString(bucketNameColumn)
                var contentUri: Uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

                // https://developer.android.com/training/data-storage/shared/media#location-media-captured
                contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)

                Log.d(TAG, "Name: $name bucketName: $bucketName bucketId: $bucketId")

                val objectName = if (bucketName.isNullOrEmpty()) name else "$bucketName/$name"
                videos.add(MediaObject(objectName, size, mimeType, contentUri))
            }
        }
        return videos.toList()
    }

    override fun getMedia(buckets: Set<String>): List<MediaObject> {
        return listOf(getPhotos(buckets), getVideos(buckets)).flatten()
    }

    override fun getStream(uri: Uri): InputStream {
        return contentResolver.openInputStream(uri) ?: throw Exception("Could not open stream for $uri.")
    }

    override fun getBuckets(): Map<String, Number> {
        val buckets = mutableMapOf<String, Int>()
        // DISTINCT? https://stackoverflow.com/questions/2315203/android-distinct-and-groupby-in-contentresolver
        // nope: https://stackoverflow.com/a/58643357
        val projection = arrayOf(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
        // https://stackoverflow.com/questions/30654774/android-is-external-content-uri-enough-for-a-photo-gallery
        val imageUri = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        val videoUri = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)

        val bucketCollector: (Cursor) -> Unit = { cursor ->
            val bucketNameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
            while (cursor.moveToNext()) {
                val bucketName = cursor.getString(bucketNameColumn)
                buckets.compute(bucketName) { _: String, v: Int? -> if (v == null) 1 else v + 1 }
            }
        }
        contentResolver.query(
            imageUri,
            projection,
            null,
            null,
            null
        )?.use(bucketCollector)

        contentResolver.query(
            videoUri,
            projection,
            null,
            null,
            null
        )?.use(bucketCollector)

        buckets.forEach { (b, c) -> Log.i(TAG, "Bucket: $b count: $c" ) }

        return buckets
    }
}


interface MediaRepository {
    fun getVideos(buckets: Set<String>): List<MediaObject>
    fun getPhotos(buckets: Set<String>): List<MediaObject>
    fun getMedia(buckets: Set<String>): List<MediaObject>
    fun getStream(uri: Uri): InputStream
    fun getBuckets(): Map<String, Number>

}
