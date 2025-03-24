package app.zimly.backup.ui.screens.editor.field

import io.minio.http.HttpUtils

/**
 * Optional field for an S3 region. Uses [HttpUtils.REGION_REGEX] to validate a given region, which
 * minio will use internally as well. `null` value will validate as well (optional).
 */
class RegionField(
    errorMessage: String = "Not a valid region.",
    validate: (value: String?) -> Boolean = {
        it?.let { HttpUtils.REGION_REGEX.matcher(it).find() } ?: true
    },
    defaultValue: String? = null
) : Field<String?>(errorMessage, validate, defaultValue)