package app.zimly.backup.ui.screens.editor.field

import android.webkit.URLUtil
import app.zimly.backup.data.db.remote.Remote
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class BucketForm {

    val name = TextField()
    val url = TextField(
        errorMessage = "Not a valid URL.",
        validate = { URLUtil.isValidUrl(it) })
    val key = TextField()
    val secret = TextField()
    val bucket = TextField()
    val region = RegionField()

    // Collect all fields into a list
    private val fields: List<Field<*>> by lazy {
        listOf(name, url, key, secret, bucket, region)
    }

    fun valid(): Flow<Boolean> = combine(fields.map { it.valid() }) { values ->
        values.all { it }
    }

    // TODO Use BucketConfiguration?
    fun populate(remote: Remote) {
        name.update(remote.name)
        url.update(remote.url)
        key.update(remote.key)
        secret.update(remote.secret)
        bucket.update(remote.bucket)
        region.update(remote.region)
    }

    fun values(): BucketConfiguration {
        return BucketConfiguration(
            name.state.value.value,
            url.state.value.value,
            key.state.value.value,
            secret.state.value.value,
            bucket.state.value.value,
            region.state.value.value,
        )
    }

    data class BucketConfiguration(
        val name: String,
        val url: String,
        val key: String,
        val secret: String,
        val bucket: String,
        val region: String?
    )
}
