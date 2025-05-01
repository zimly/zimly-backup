package app.zimly.backup.data.media

import android.content.ContentResolver
import android.content.ContentUris
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import java.io.InputStream
import java.io.OutputStream

/**
 * Wraps the [ContentResolver] for media specific queries. Compared to [LocalMediaResolver]
 * thr repository is not bound to a collection or scope and can be used for more general queries.
 */
class LocalMediaRepository(private val contentResolver: ContentResolver): MediaRepository {

    companion object {
        private val TAG: String? = LocalMediaRepository::class.simpleName
    }

    override fun getPhotos(collections: Set<String>): List<ContentObject> {
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
        Log.d(TAG, "Content URI path: ${contentUri.path ?: "null"}")

        val photos = mutableListOf<ContentObject>()
        val contentBuckets = collections.joinToString(separator = ",", transform = { bucket -> "'${bucket}'"})
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
            val bucketNameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)

            Log.d(TAG, "Number of images: ${cursor.count}")
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val mimeType = cursor.getString(mimeTypeColumn)
                val size = cursor.getLong(sizeColumn)
                val bucketName = cursor.getString(bucketNameColumn)
                // Get location data using the Exifinterface library.
                // Exception occurs if ACCESS_MEDIA_LOCATION permission isn't granted.
                // https://developer.android.com/training/data-storage/shared/media#location-media-captured
                var photoUri: Uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                photoUri= MediaStore.setRequireOriginal(photoUri)

                val objectName = if (bucketName.isNullOrEmpty()) name else "$bucketName/$name"
                photos.add(ContentObject(objectName, size, mimeType, photoUri))
            }
        }
        return photos.toList()
    }

    override fun getVideos(collections: Set<String>): List<ContentObject> {

        val videos = mutableListOf<ContentObject>()

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

        val contentBuckets = collections.joinToString(separator = ",", transform = { bucket -> "'${bucket}'"})
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
            val bucketNameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)

            Log.d(TAG, "Number of videos: ${cursor.count}")
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val mimeType = cursor.getString(mimeTypeColumn)
                val size = cursor.getLong(sizeColumn)
                val bucketName = cursor.getString(bucketNameColumn)

                // https://developer.android.com/training/data-storage/shared/media#location-media-captured
                val videoUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)

                val objectName = if (bucketName.isNullOrEmpty()) name else "$bucketName/$name"
                videos.add(ContentObject(objectName, size, mimeType, videoUri))
            }
        }
        return videos.toList()
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
                bucketName?.let { buckets.compute(it) { _: String, v: Int? -> if (v == null) 1 else v + 1 } }
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

        buckets.forEach { (b, c) -> Log.d(TAG, "Bucket: $b count: $c" ) }

        return buckets
    }
}

/**
 * Wraps the [ContentResolver] for scoped media collection specific queries. The scope is passed as
 * a collection.
 */
class LocalMediaResolverImpl(private val contentResolver: ContentResolver, private val collection: String): LocalContentResolver, LocalMediaResolver {

    private var mediaRepository: MediaRepository = LocalMediaRepository(contentResolver)

    override fun listObjects(): List<ContentObject> {
        return listOf(mediaRepository.getPhotos(setOf(collection)), mediaRepository.getVideos(setOf(collection))).flatten()
    }

    override fun getInputStream(uri: Uri): InputStream {
        return contentResolver.openInputStream(uri) ?: throw Exception("Could not open stream for $uri.")
    }

    override fun photoCount(): Int {
        return mediaRepository.getPhotos(setOf(collection)).count()
    }

    override fun videoCount(): Int {
        return mediaRepository.getVideos(setOf(collection)).count()
    }

    override fun getOutputStream(parentUri: Uri, objectName: String, mimeType: String): OutputStream {
        TODO("Fix interfaces!")
    }
}

interface MediaRepository {
    fun getVideos(collections: Set<String>): List<ContentObject>
    fun getPhotos(collections: Set<String>): List<ContentObject>
    fun getBuckets(): Map<String, Number>
}

interface LocalMediaResolver {
    fun photoCount(): Int
    fun videoCount(): Int
}

