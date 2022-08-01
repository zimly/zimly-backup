package io.zeitmaschine.zimzync

import java.util.*

data class Remote(val name: String, val url: String, val bucket: String) {
    var key: String = ""
    var secret: String = ""
    var lastSynced: Date? = null
}
