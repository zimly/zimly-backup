package app.zimly.backup.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationV9Test {
    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ZimlyDatabase::class.java
    )

    // TODO: Rename things, add test for verifying contents (content_uri -> sync_path)
    @Test
    fun migrate7To9() {

        helper.createDatabase(TEST_DB, 7).apply {
            // Database has schema version 7. Insert some data using SQL queries.
            // You can't use DAO classes because they expect the latest schema.

            execSQL(
                """
                    INSERT INTO Remote VALUES(1,'test','http://','test','test','test',NULL,0,'FOLDER','content://com.android.externalstorage.documents/tree/primary%3Atest','UPLOAD');
                """.trimIndent()
            )

            // Prepare for the next version.
            close()
        }

        // Re-open the database with version 9 and run and validate auto-migration schema updates
        val migrated = helper.runMigrationsAndValidate(TEST_DB, 9, true)

        // Verify migrated entries
        val cursor = migrated.query("SELECT profile_id, uri FROM sync_path")

        assertTrue(cursor.moveToFirst())

        val profileId = cursor.getInt(cursor.getColumnIndexOrThrow("profile_id"))
        val uri = cursor.getString(cursor.getColumnIndexOrThrow("uri"))
        assertTrue(profileId == 1)
        assertTrue(uri == "content://com.android.externalstorage.documents/tree/primary%3Atest")

        val profileCursor = migrated.query("SELECT * FROM sync_profile")
        assertTrue(profileCursor.columnNames.contains("content_uri"))

    }
}