package app.zimly.backup.ui.screens.start

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.zimly.backup.data.db.ZimlyDatabase
import app.zimly.backup.data.db.sync.SyncProfile
import app.zimly.backup.data.db.sync.SyncDao
import app.zimly.backup.data.db.sync.SyncDirection
import app.zimly.backup.data.media.ContentType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class StartViewModelTest {
    private lateinit var db: ZimlyDatabase
    private lateinit var dao: SyncDao
    private lateinit var viewModel: StartViewModel

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, ZimlyDatabase::class.java
        ).build()

        this.dao = db.syncDao()
        this.viewModel = StartViewModel(dao)

        runBlocking {
            dao.insert(
                SyncProfile(
                    null,
                    "Test 1",
                    "https://zimly.cloud",
                    "key",
                    "secret",
                    "bucket",
                    null,
                    false,
                    ContentType.MEDIA,
                    "Pictures",
                    SyncDirection.UPLOAD
                )
            )
            dao.insert(
                SyncProfile(
                    null,
                    "Test 2",
                    "https://zimly.cloud",
                    "key",
                    "secret",
                    "bucket",
                    null,
                    false,
                    ContentType.MEDIA,
                    "Pictures",
                    SyncDirection.UPLOAD
                )
            )
        }
    }

    @Test
    fun copy() {
        // GIVEN
        val syncProfiles = runBlocking {
            dao.getAll().first()
        }
        assertThat(syncProfiles.size, `is`(2))

        // WHEN
        viewModel.select(syncProfiles[0].uid!!.toInt())
        runBlocking {
            viewModel.copy()
        }

        // THEN
        runBlocking {
            assertThat(dao.getAll().first().size, `is`(3))
        }
    }

    @Test
    fun delete() {
        // GIVEN
        val syncProfiles = runBlocking {
            dao.getAll().first()
        }
        assertThat(syncProfiles.size, `is`(2))

        // WHEN
        viewModel.select(syncProfiles[0].uid!!.toInt())
        runBlocking {
            viewModel.delete()
        }

        // THEN
        runBlocking {
            assertThat(dao.getAll().first().size, `is`(1))
        }
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

}