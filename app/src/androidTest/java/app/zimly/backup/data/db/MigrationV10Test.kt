package app.zimly.backup.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class MigrationV10Test {

    private val testDb = "migration-test"
    private val fromVersion = 7
    private val toVersion = 10
    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ZimlyDatabase::class.java
    )

    @Test
    fun migrate7To10() {

        helper.createDatabase(testDb, fromVersion).apply {
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
        val migrated = helper.runMigrationsAndValidate(testDb, toVersion, true)

        // Verify migrated entries
        val cursor = migrated.query("SELECT * FROM sync_profile")

        assertTrue(cursor.moveToFirst())
        assertFalse("content_uri column was removed", cursor.columnNames.contains("content_uri"))
    }
}