package io.zeitmaschine.zimzync

import android.content.ContentResolver
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// https://www.geeksforgeeks.org/services-in-android-using-jetpack-compose/
class MediaRepository(private val contentResolver: ContentResolver) {

    companion object {
        private val TAG: String? = MediaRepository::class.simpleName
    }

    suspend fun getPhotos(): Result<List<String>> {

        // TODO: Jetpack compose and running stuff in the background:
        // https://stackoverflow.com/questions/74235464/jetpack-compose-make-launchedeffect-keep-running-while-app-is-running-in-the-b
        // https://stackoverflow.com/questions/73332937/what-would-be-the-most-lightweight-way-to-observe-current-time-for-a-an-androi/73333458#73333458
        // https://developer.android.com/guide/background - kotlin coroutines vs workmanager?

        return withContext(Dispatchers.IO) {
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
            Log.i(TAG, contentUri.path?:"whoops")

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
            return@withContext Result.Success(photos.toList())
        }

    }
}
