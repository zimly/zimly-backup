package io.zeitmaschine.zimzync

import android.content.ContentResolver
import android.provider.MediaStore
import android.util.Log

// https://www.geeksforgeeks.org/services-in-android-using-jetpack-compose/
class ResolverBasedRepository(private val contentResolver: ContentResolver) : MediaRepository {

    companion object {
        private val TAG: String? = ResolverBasedRepository::class.simpleName
    }

    override fun getPhotos(): List<String> {
        // https://developer.android.com/training/data-storage/shared/media#media_store
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.DISPLAY_NAME,
        )

        // Display videos in alphabetical order based on their display name.
        val sortOrder = "${MediaStore.Images.ImageColumns.DATE_TAKEN} DESC"

        // https://stackoverflow.com/questions/30654774/android-is-external-content-uri-enough-for-a-photo-gallery
        val contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        Log.i(TAG, contentUri.path ?: "whoops")

        val photos = mutableListOf<String>()
        contentResolver.query(
            contentUri,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            Log.i(TAG, "${cursor.count}")
            while (cursor.moveToNext()) {
                val name = cursor.getString(2);
                Log.i(TAG, name)
                photos.add(name)
            }
        }
        return photos.toList()
    }
}


interface MediaRepository {
    fun getPhotos(): List<String>
}
