package app.zimly.backup.sync

import android.annotation.SuppressLint
import androidx.work.Data
import androidx.work.hasKeyWithValueOfType

/**
 * Returns the [Data] value corresponding to the given [key] of type [T] or null if it doesn't exist.
 */
inline fun <reified T : Any> Data.getNullable(key: String): T? {
    return if (this.hasKeyWithValueOfType<T>(key)) this.keyValueMap[key] as T else null
}

/**
 * Adds the non-null [value] with [key] to the [Data.Builder]. In case of a null value it just ignores
 * the key/value pair.
 */
@SuppressLint("RestrictedApi")
fun Data.Builder.putIfNotNull(key: String, value: Any?): Data.Builder {
    if (value != null) this.put(key, value)
    return this
}