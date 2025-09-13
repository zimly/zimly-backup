package app.zimly.backup.ui.screens.editor.form

import android.webkit.URLUtil
import app.zimly.backup.data.db.sync.SyncProfile
import app.zimly.backup.ui.screens.editor.form.field.BooleanField
import app.zimly.backup.ui.screens.editor.form.field.Field
import app.zimly.backup.ui.screens.editor.form.field.RegionField
import app.zimly.backup.ui.screens.editor.form.field.TextField
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

class BucketForm : Form {

    val name = TextField()
    val url = TextField(
        errorMessage = "Not a valid URL.",
        validate = { URLUtil.isValidUrl(it) })
    val key = TextField()
    val secret = TextField()
    val bucket = TextField()
    val region = RegionField()
    val virtualHostedStyle = BooleanField(
        errorMessage = "",
        validate = { true })

    /**
     * Emits a warning in case virtual hosted style is enabled and the URL starts
     * with the bucket name.
     * This is not an error, because there might be cases where this is legit and the underlying
     * Minio SDK is messy, so this should not prohibit experimenting with the configuration.
     */
    fun warning(): Flow<Boolean> = virtualHostedStyle.state.map {
        if (it.value) {
            val bucketName = bucket.state.value.value
            val url = url.state.value.value
            val host = url.toHttpUrlOrNull()?.host
            if (host != null && host.startsWith(bucketName)) {
                return@map true
            }
        }
        return@map false
    }

    // Collect all fields into a list
    private val fields: List<Field<*>> by lazy {
        listOf(name, url, key, secret, bucket, region, virtualHostedStyle)
    }

    override fun valid(): Flow<Boolean> = combine(fields.map { it.valid() }) { values ->
        values.all { it }
    }

    fun errors(): Flow<List<String>> = combine(fields.map { it.error() }) { errors ->
        errors.filterNotNull()
    }

    override fun validate() {
        fields.forEach {
            it.touch()
            it.validate()
        }
    }

    fun populate(bucketConfiguration: BucketConfiguration) {
        name.update(bucketConfiguration.name)
        url.update(bucketConfiguration.url)
        key.update(bucketConfiguration.key)
        secret.update(bucketConfiguration.secret)
        bucket.update(bucketConfiguration.bucket)
        region.update(bucketConfiguration.region)
        virtualHostedStyle.update(bucketConfiguration.virtualHostedStyle)
    }

    fun populate(syncProfile: SyncProfile) {
        name.update(syncProfile.name)
        url.update(syncProfile.url)
        key.update(syncProfile.key)
        secret.update(syncProfile.secret)
        bucket.update(syncProfile.bucket)
        region.update(syncProfile.region)
        virtualHostedStyle.update(syncProfile.virtualHostedStyle)
    }

    fun values(): BucketConfiguration {
        return BucketConfiguration(
            name.state.value.value,
            url.state.value.value,
            key.state.value.value,
            secret.state.value.value,
            bucket.state.value.value,
            region.state.value.value,
            virtualHostedStyle.state.value.value,
        )
    }

    data class BucketConfiguration(
        val name: String,
        val url: String,
        val key: String,
        val secret: String,
        val bucket: String,
        val region: String?,
        val virtualHostedStyle: Boolean
    )
}
