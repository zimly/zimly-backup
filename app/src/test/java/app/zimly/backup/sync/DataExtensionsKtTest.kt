package app.zimly.backup.sync

import androidx.work.Data
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

class DataExtensionsKtTest {

    @Test
    fun nullValues() {

        val data = Data.Builder()
            .putLong("long", 1L)
            .putString("string", "test")
            .putIfNotNull("null", null)
            .putIfNotNull("non-null", "nix-null")
            .build()

        assertThat(data.getLong("long", -1), `is`(1L))
        assertThat(data.getString("string"), `is`("test"))
        assertThat(data.getNullable("null"), nullValue())
        assertThat(data.getNullable("non-null"), `is`("nix-null"))
    }
}
