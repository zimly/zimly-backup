package io.zeitmaschine.zimzync

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.provider.MediaStore
import android.util.Log

// https://www.geeksforgeeks.org/services-in-android-using-jetpack-compose/
class SyncService : Service() {

    companion object {
        private val TAG: String? = SyncService::class.simpleName
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        // TODO: Jetpack compose and running stuff in the background:
        // https://stackoverflow.com/questions/74235464/jetpack-compose-make-launchedeffect-keep-running-while-app-is-running-in-the-b
        // https://stackoverflow.com/questions/73332937/what-would-be-the-most-lightweight-way-to-observe-current-time-for-a-an-androi/73333458#73333458
        // https://developer.android.com/guide/background - kotlin coroutines vs workmanager?

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
        this.contentResolver.query(
            contentUri,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            Log.i(TAG, "${cursor.count}")
            while (cursor.moveToNext()) {
                Log.i(TAG, "${cursor.columnNames}")
            }
        }
        return START_STICKY
    }

    override fun onBind(p0: Intent?): IBinder? {
        TODO("Not yet implemented")
    }
}