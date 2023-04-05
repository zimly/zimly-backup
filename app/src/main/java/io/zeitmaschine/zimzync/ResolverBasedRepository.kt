package io.zeitmaschine.zimzync

import android.content.ContentResolver
import android.content.ContentUris
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
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.SIZE,
        )

        // Display videos in alphabetical order based on their display name.
        val sortOrder = "${MediaStore.Images.ImageColumns.DATE_TAKEN} DESC"

        // https://stackoverflow.com/questions/30654774/android-is-external-content-uri-enough-for-a-photo-gallery
        val contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        Log.i(TAG, contentUri.path ?: "whoops")

        val photos = mutableListOf<MediaObject>()
        contentResolver.query(
            contentUri,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
            val modifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)

            Log.i(TAG, "${cursor.count}")
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val mimeType = cursor.getString(mimeTypeColumn)
                val modified = cursor.getLong(modifiedColumn)
                val size = cursor.getLong(sizeColumn)
                var contentUri: Uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

                // https://developer.android.com/training/data-storage/shared/media#location-media-captured
                contentUri= MediaStore.setRequireOriginal(contentUri)

                Log.i(TAG, name)
                photos.add(MediaObject(name, size, "", mimeType, System.currentTimeMillis(), contentUri))
            }
        }
        return photos.toList()
    }

    override fun getStream(uri: Uri): InputStream {
        return contentResolver.openInputStream(uri) ?: throw Exception("Could not open stream for $uri.")
    }
}


interface MediaRepository {
    fun getPhotos(): List<MediaObject>
    fun getStream(uri: Uri): InputStream
}
