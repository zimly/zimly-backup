package app.zimly.backup.ui.screens.editor.form

import android.webkit.URLUtil
import app.zimly.backup.data.db.remote.Remote
import app.zimly.backup.ui.screens.editor.form.field.BooleanField
import app.zimly.backup.ui.screens.editor.form.field.Field
import app.zimly.backup.ui.screens.editor.form.field.RegionField
import app.zimly.backup.ui.screens.editor.form.field.TextField
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class BucketForm : Form {

    val name = TextField()
    val url = TextField(
        errorMessage = "Not a valid URL.",
        validate = { URLUtil.isValidUrl(it) })
    val key = TextField()
    val secret = TextField()
    val bucket = TextField()
    val region = RegionField()
    val virtualHostedStyle = BooleanField()

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

    fun populate(remote: Remote) {
        name.update(remote.name)
        url.update(remote.url)
        key.update(remote.key)
        secret.update(remote.secret)
        bucket.update(remote.bucket)
        region.update(remote.region)
        virtualHostedStyle.update(remote.virtualHostedStyle)
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
