package io.zeitmaschine.zimzync

import android.content.ContentResolver
import android.content.ContentUris
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import java.io.InputStream

data class MediaObject(
    var name: String,
    var size: Long,
    var checksum: String,
    var contentType: String,
    val modified: Long,
    val path: Uri,
)

class ResolverBasedRepository(private val contentResolver: ContentResolver) : MediaRepository {

    companion object {
        private val TAG: String? = ResolverBasedRepository::class.simpleName
    }

    override fun getPhotos(): List<MediaObject> {
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
        contentResolver.query(
            contentUri,
            projection,
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME + " = 'Camera'",
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            val modifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val bucketIdColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_ID)
            val bucketNameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)

            Log.i(TAG, "${cursor.count}")
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val mimeType = cursor.getString(mimeTypeColumn)
                val modified = cursor.getLong(modifiedColumn)
                val size = cursor.getLong(sizeColumn)
                val bucketId: Long = cursor.getLong(bucketIdColumn)
                val bucketName = cursor.getString(bucketNameColumn)
                var contentUri: Uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

                // https://developer.android.com/training/data-storage/shared/media#location-media-captured
                contentUri= MediaStore.setRequireOriginal(contentUri)

                Log.i(TAG, bucketId.toString())
                Log.i(TAG, bucketName)
                Log.i(TAG, name)

                val objectName = if (bucketName.isNullOrEmpty()) name else "$bucketName/$name"
                photos.add(MediaObject(objectName, size, "", mimeType, System.currentTimeMillis(), contentUri))
            }
        }
        return photos.toList()
    }

    override fun getVideos(): List<MediaObject> {

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

        contentResolver.query(
            contentUri,
            projection,
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME + " = 'Camera'",
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            val modifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val bucketIdColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_ID)
            val bucketNameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)

            Log.i(TAG, "${cursor.count}")
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val mimeType = cursor.getString(mimeTypeColumn)
                val modified = cursor.getLong(modifiedColumn)
                val size = cursor.getLong(sizeColumn)
                val bucketId: Long = cursor.getLong(bucketIdColumn)
                val bucketName = cursor.getString(bucketNameColumn)
                var contentUri: Uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

                // https://developer.android.com/training/data-storage/shared/media#location-media-captured
                contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)

                Log.i(TAG, bucketId.toString())
                Log.i(TAG, bucketName)
                Log.i(TAG, name)
                val objectName = if (bucketName.isNullOrEmpty()) name else "$bucketName/$name"
                videos.add(MediaObject(objectName, size, "", mimeType, System.currentTimeMillis(), contentUri))
            }
        }
        return videos.toList()
    }

    override fun getMedia(): List<MediaObject> {
        return listOf(getPhotos(), getVideos()).flatten()
    }

    override fun getStream(uri: Uri): InputStream {
        return contentResolver.openInputStream(uri) ?: throw Exception("Could not open stream for $uri.")
    }
}


interface MediaRepository {
    fun getPhotos(): List<MediaObject>
    fun getStream(uri: Uri): InputStream
    fun getVideos(): List<MediaObject>
    fun getMedia(): List<MediaObject>
}
